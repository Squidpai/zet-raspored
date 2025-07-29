package hr.squidpai.zetapi.gtfs

import androidx.collection.IntIntMap
import androidx.collection.IntIntPair
import androidx.collection.IntList
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntList
import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import com.opencsv.CSVReader
import hr.squidpai.zetapi.CsvHeaderMapping
import hr.squidpai.zetapi.DirectionId
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.RouteAtStop
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.ServiceId
import hr.squidpai.zetapi.Shape
import hr.squidpai.zetapi.ShapeId
import hr.squidpai.zetapi.Shapes
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.StopId
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.TimeOfDayList
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.TripId
import hr.squidpai.zetapi.asStopId
import hr.squidpai.zetapi.csvToListFromEntry
import hr.squidpai.zetapi.filterInPlaceAllNotNull
import hr.squidpai.zetapi.get
import hr.squidpai.zetapi.realtime.RealtimeDispatcher
import hr.squidpai.zetapi.toParentStopId
import hr.squidpai.zetapi.toStopId
import java.util.zip.ZipFile

internal class GtfsTripLoader private constructor(
   private val zip: ZipFile,
   private val routes: Map<RouteId, GtfsRoute>,
   private val stops: Stops,
   private val shapes: Shapes,
   private val realtimeDispatcher: RealtimeDispatcher,
   private val onLoadProgress: (Float) -> Unit,
) {

   private lateinit var trips: Map<TripId, GtfsTrip>

   private val fauxShapes = mutableMapOf<ShapeId, Shape>()

   private fun internalize(
      tripId: TripId,
      departureTimes: IntList,
      stopIds: MutableIntList,
   ) {
      val trip = trips[tripId]
      // the trip id in the stop times file doesn't exist (?!) -> skip
         ?: return
      val stops = List(stopIds.size) {
         this.stops[Love.redirectMeToTheBetterStopId(trip.route.id, stopIds[it].asStopId())]
            ?: return
      }

      val dirStopSequences = trip.route.stopSequences[trip.directionId]

      trip.departures = departureTimes

      val sequenceId = dirStopSequences.indexOf(stops)
      if (sequenceId == -1) {
         dirStopSequences += stops
         trip.stops = stops
         trip.stopSequenceId = dirStopSequences.lastIndex
         return
      }

      // Use the internalized instance.
      trip.stops = dirStopSequences[sequenceId]
      trip.stopSequenceId = sequenceId
   }

   private fun loadTrips() {
      trips = zip.csvToListFromEntry(
         "trips.txt",
         tripHeaderMapping,
         ::tripFactory,
      ).filterInPlaceAllNotNull()
         .associateBy { it.tripId }

      onLoadProgress(.32f)

      loadStopTimes()

      onLoadProgress(.84f)

      bindTripsToRoutes()

      onLoadProgress(.90f)

      bindRoutesToStops()

      onLoadProgress(.96f)
   }

   private fun loadStopTimes() {
      val reader = CSVReader(
         zip.getInputStream(zip.getEntry("stop_times.txt"))
            .bufferedReader()
      )

      val headerMap = stopTimeHeaderMapping(reader.readNext() ?: return)

      val first = reader.readNext() ?: return

      var currentTripId = first[headerMap[0]]
      val firstStopId = first.getOrNull(headerMap[2])?.toStopId()
      var departureTimes = MutableIntList().also {
         if (firstStopId != null)
            it += TimeOfDay(first[headerMap[1]]).valueInSeconds
      }
      var stopIds = MutableIntList().also {
         if (firstStopId != null)
            it += firstStopId.rawValue
      }
      reader.forEachIndexed { index, data ->
         val tripId = data[headerMap[0]]
         val departureTime = TimeOfDay(data[headerMap[1]])
         val stopId = data.getOrNull(headerMap[2])?.toStopId() ?: return@forEachIndexed

         if (currentTripId != tripId) {
            internalize(currentTripId, departureTimes, stopIds)
            currentTripId = tripId
            departureTimes = MutableIntList()
            stopIds = MutableIntList()
         }

         departureTimes += departureTime.valueInSeconds
         stopIds += stopId.rawValue

         if (index and (1 shl 14) - 1 != 0)
            onLoadProgress(.32f + index / 4_000_000f)
      }

      internalize(currentTripId, departureTimes, stopIds)
   }

   private fun bindTripsToRoutes() {
      val commonFirstStops = calculateCommonHeadsignsAndFirstStops()

      for (trip in trips.values) {
         trip.route.trips[trip.tripId] = trip.toTrip(
            isHeadsignCommon = trip.route.commonHeadsigns[trip.serviceId]
               ?.get(trip.directionId) == trip.headsign,
            isFirstStopCommon = commonFirstStops[trip.route.id]
               ?.get(trip.serviceId)
               ?.get(trip.directionId) == trip.stops.first().id.rawValue,
            fauxShapes,
         )
      }

      for (route in routes.values) {
         val sortedTrips = route.trips
            .values
            .sortedBy { it.departures.first() }
            .associateByTo(LinkedHashMap()) { it.tripId }
         route.trips = sortedTrips
      }
   }

   private fun bindRoutesToStops() {
      for (route in routes.values) {
         fun bind(sequences: List<List<Stop>>, isZero: Boolean) {
            for (sequence in sequences) {
               sequence.forEachIndexed { i, stop ->
                  val bind = { s: Stop ->
                     (s.routes as GtfsStopRoutes)
                        .getOrPut(route) { RouteAtStop() }
                        .run {
                           if (i > 0)
                              isFirst = false
                           if (i < sequence.lastIndex)
                              isLast = false
                           if (isZero)
                              stopsAtDirectionZero = true
                           else
                              stopsAtDirectionOne = true
                        }
                  }

                  bind(stop)
                  // all stops should be stations, which have a parent stop
                  //if (stop.parentId != -1)
                  stops[stop.parentId.toParentStopId()]?.let { bind(it) }
               }
            }
         }

         bind(route.stopSequences.first, isZero = true)
         bind(route.stopSequences.second, isZero = false)
      }
   }

   /**
    * Calculates all common headsigns and first stops.
    *
    * Common headsigns are placed into their matching [routes].
    *
    * Common first stops are returned from this function.
    */
   private fun calculateCommonHeadsignsAndFirstStops():
         Map<RouteId, Map<ServiceId, IntIntPair>> {
      val appearances =
         // grouped by routes
         HashMap<RouteId,
               // grouped by service day
               HashMap<ServiceId, Pair<
                     // first = DirectionId.Zero, second = DirectionId.One
                     //   first key = headsign, second key = first stop
                     //   values = appearance count
                     Pair<MutableObjectIntMap<String>, MutableIntIntMap>,
                     Pair<MutableObjectIntMap<String>, MutableIntIntMap>>>>()

      for (trip in trips.values) {
         val byRoute = appearances.getOrPut(trip.route.id) { HashMap() }
         val byServiceId = byRoute.getOrPut(trip.serviceId) {
            (MutableObjectIntMap<String>() to MutableIntIntMap()) to
                  (MutableObjectIntMap<String>() to MutableIntIntMap())
         }
         val byDirection =
            if (trip.directionId.isZero) byServiceId.first
            else byServiceId.second

         byDirection.first[trip.headsign] =
            byDirection.first.getOrDefault(trip.headsign, 0) + 1

         val firstStopId = trip.stops.first().id.rawValue
         byDirection.second[firstStopId] =
            byDirection.second.getOrDefault(firstStopId, 0) + 1
      }

      val commonFirstStops = HashMap<RouteId, Map<ServiceId, IntIntPair>>()

      for (route in routes.values) {
         val byRoute = appearances[route.id] ?: continue

         val forcedCommonHeadsign = Love.giveMeTheForcedCommonHeadsign(route.id)

         val forcedFirstStops = Love.giveMeTheForcedFirstStop(route.id)

         byRoute.mapValuesTo(route.commonHeadsigns as GtfsRouteCommonHeadsigns) {
            getCommonHeadsign(
               it.value.first.first,
               forcedCommonHeadsign?.first,
            ) to getCommonHeadsign(
               it.value.second.first,
               forcedCommonHeadsign?.second,
            )
         }

         commonFirstStops[route.id] = byRoute.mapValues {
            IntIntPair(
               getCommonFirstStop(
                  it.value.first.second,
                  forcedFirstStops.first,
               ),
               getCommonFirstStop(
                  it.value.second.second,
                  forcedFirstStops.second,
               )
            )
         }
      }

      return commonFirstStops
   }

   private fun getCommonHeadsign(
      appearances: ObjectIntMap<String>,
      forcedCommonHeadsign: String?,
   ): String {
      if (appearances.isEmpty())
         return ""

      var maxCount = 0
      var maxSign = ""
      appearances.forEach { sign, count ->
         if (forcedCommonHeadsign == sign)
            return forcedCommonHeadsign
         if (count > maxCount) {
            maxCount = count
            maxSign = sign
         }
      }

      return maxSign
   }

   private fun getCommonFirstStop(
      appearances: IntIntMap,
      forcedFirstStop: Int,
   ): Int {
      if (appearances.isEmpty())
         return StopId.Invalid.rawValue

      var maxCount = 0
      var maxStop = StopId.Invalid.rawValue
      appearances.forEach { stop, count ->
         if (forcedFirstStop == stop)
            return forcedFirstStop
         if (count > maxCount) {
            maxCount = count
            maxStop = stop
         }
      }

      return maxStop
   }

   companion object {
      fun loadTrips(
         zip: ZipFile,
         routes: Map<RouteId, GtfsRoute>,
         stops: Stops,
         shapes: Shapes,
         realtimeDispatcher: RealtimeDispatcher,
         onLoadProgress: (Float) -> Unit,
      ): Shapes {
         val loader = GtfsTripLoader(zip, routes, stops, shapes, realtimeDispatcher, onLoadProgress)
         loader.loadTrips()
         return loader.fauxShapes
      }

      private val tripHeaderMapping: CsvHeaderMapping = { header ->
         val headerMap = IntArray(7) { -1 }
         header.forEachIndexed { i, h ->
            when (h) {
               "route_id" -> headerMap[0] = i
               "service_id" -> headerMap[1] = i
               "trip_id" -> headerMap[2] = i
               "trip_headsign" -> headerMap[3] = i
               "direction_id" -> headerMap[4] = i
               "block_id" -> headerMap[5] = i
               "shape_id" -> headerMap[6] = i
            }
         }
         headerMap
      }

      private val stopTimeHeaderMapping: CsvHeaderMapping = { header ->
         val headerMap = IntArray(3) { -1 }
         header.forEachIndexed { i, h ->
            when (h) {
               "trip_id" -> headerMap[0] = i
               "departure_time" -> headerMap[1] = i
               "stop_id" -> headerMap[2] = i
            }
         }
         headerMap
      }
   }

   private fun tripFactory(
      headerMap: IntArray,
      data: Array<out String>,
   ): GtfsTrip? {
      val route = routes[data[headerMap[0]]] ?: return null
      val shape = shapes[data[headerMap[6]]]
      return GtfsTrip(
         route,
         serviceId = data[headerMap[1]],
         tripId = data[headerMap[2]],
         headsign = data.getOrNull(headerMap[3]).orEmpty(),
         directionId = DirectionId(data.getOrNull(headerMap[4])),
         blockId = data[headerMap[5]],
         shape,
         realtimeDispatcher,
      )
   }

   private class GtfsTrip(
      val route: GtfsRoute,
      val serviceId: ServiceId,
      val tripId: TripId,
      val headsign: String,
      val directionId: DirectionId,
      val blockId: String,
      val shape: Shape?,
      val realtimeDispatcher: RealtimeDispatcher,
   ) {
      lateinit var stops: List<Stop>
      lateinit var departures: TimeOfDayList
      var stopSequenceId = -1

      @OptIn(ExperimentalStdlibApi::class)
      fun toTrip(
         isHeadsignCommon: Boolean,
         isFirstStopCommon: Boolean,
         fauxShapes: MutableMap<ShapeId, Shape>,
      ) = Trip(
         route,
         serviceId,
         tripId,
         headsign,
         directionId,
         blockId,
         shape ?: fauxShapes.getOrPut("${route.id}_${stops.hashCode().toHexString()}") {
            Shape("", stops.map { listOf(Shape.Point(it.latitude, it.longitude, Float.NaN)) })
         },
         stops,
         departures,
         stopSequenceId,
         isHeadsignCommon,
         isFirstStopCommon,
         realtimeDispatcher,
      )
   }

}