package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.collection.IntIntPair
import androidx.collection.IntList
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntSetOf
import com.opencsv.CSVReader
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.SECONDS_IN_HOUR
import hr.squidpai.zetlive.SortedListMap
import hr.squidpai.zetlive.asSortedListMap
import hr.squidpai.zetlive.filterByValue
import hr.squidpai.zetlive.get
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.readShortString
import hr.squidpai.zetlive.toInt
import hr.squidpai.zetlive.toIntArray
import hr.squidpai.zetlive.writeShortString
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Scanner
import java.util.zip.ZipFile
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.absoluteValue

private const val TAG = "Trips"

typealias TripId = String

data class Trip(
   val routeId: RouteId,
   val serviceId: ServiceId,
   val tripId: TripId,
   val headsign: String,
   val directionId: Int,
   val blockId: Int,
   val stops: IntList,
   val departures: IntList,
   val tripShape: Int,
) {
   fun findNextStopIndex(time: Int, delays: DelayByStop): Int {
      // this cannot be optimized with a binary search, as the departures
      // with a delay are not sorted sometimes (even though that makes
      // little sense...)
      departures.forEachIndexed { index, departure ->
         if (time < (delays[index] + departure)) {
            return index
         }
      }
      return stops.size
   }

   fun findNextStopIndexToday(time: Int, delays: DelayByStop): Int {
      departures.forEachIndexed { index, departure ->
         if (time < (delays[index] + departure) % SECONDS_IN_DAY) {
            return index
         }
      }
      return stops.size
   }

   fun joinStopsToString(
      stopsList: SortedListMap<StopId, Stop>,
      beginIndex: Int = 0,
      endIndex: Int = stops.size,
      prefix: CharSequence? = null,
      separator: CharSequence = " ${Typography.bullet} ",
      postfix: CharSequence? = null
   ) = buildString {
      prefix?.let { append(it) }
      for (i in beginIndex..<endIndex) {
         if (i != beginIndex)
            append(separator)
         append(stopsList[stops[i].toStopId()]?.name.orLoading())
      }
      postfix?.let { append(it) }
   }
}

class Trips(
   val list: TripsList,
   val commonHeadsign: Pair<String, String>,
   val commonFirstStop: Pair<StopId, StopId>,
   val tripShapes: List<IntList>,
) {
   fun printTripShapes(stops: Stops) {
      for (shape in tripShapes) {
         if (shape.first().toStopId() != commonFirstStop.first && shape.first()
               .toStopId() != commonFirstStop.second
         )
            continue

         println(shape.joinToString(
            prefix = "[",
            postfix = "]",
            transform = { stops.list[it.toStopId()]?.name ?: "?" }
         ))
      }
   }

   fun printSpecialTripShapes(stops: Stops) {
      val printedShapes = MutableIntSet()
      for (trip in list) {
         if (trip.stops.first().toStopId() != commonFirstStop[trip.directionId] ||
            trip.headsign != commonHeadsign[trip.directionId] ||
            trip.tripShape in printedShapes
         )
            continue

         printedShapes += trip.tripShape

         Log.d("SpecialRoutes", trip.stops.joinToString(
            prefix = "[",
            postfix = "]",
            transform = { stops.list[it.toStopId()]?.name ?: "?" }
         ))
      }
   }
}

typealias TripsList = SortedListMap<TripId, Trip>

fun TripsList.filterByServiceId(serviceId: ServiceId): TripsList =
   filterByValue { serviceId == it.serviceId }

fun TripsList.splitByDirection(): Pair<TripsList, TripsList> {
   // we will assume that there are an equal number of trips in each direction
   // + 2 to leave room for error (the list can expand further if needed)
   val first = ArrayList<Trip>(this.size / 2 + 2)
   val second = ArrayList<Trip>(this.size / 2 + 2)

   for (trip in this) {
      if (trip.directionId == 0) first += trip
      else second += trip
   }

   return first.asSortedListMap(this.keyFactory) to second.asSortedListMap(this.keyFactory)
}

fun Pair<TripsList, TripsList>.sortedByDepartures() =
   first.sortedBy { it.departures.first() } to second.sortedBy { it.departures.first() }

fun Pair<List<Trip>, List<Trip>>.findFirstDepartures(
   timeInSeconds: Int
): IntIntPair {
   val comparison = { s: Trip -> s.departures.last().compareTo(timeInSeconds) }
   var firstIndex = first.binarySearch(comparison = comparison)
   var secondIndex = second.binarySearch(comparison = comparison)

   if (firstIndex < 0) firstIndex = -firstIndex - 1
   if (secondIndex < 0) secondIndex = -secondIndex - 1

   return IntIntPair(firstIndex, secondIndex)
}

fun Pair<List<Trip>, List<Trip>>.findFirstDepartures(
   timeInSeconds: Int,
   live: Live?,
): IntIntPair {
   if (live == null) return findFirstDepartures(timeInSeconds)

   val firstIndex = first.indexOfFirst {
      timeInSeconds < it.departures.first() + live.getDelayByStopForTrip(it.tripId)[0]
   }
   val secondIndex = second.indexOfFirst {
      timeInSeconds < it.departures.first() + live.getDelayByStopForTrip(it.tripId)[0]
   }

   return IntIntPair(firstIndex, secondIndex)
}

fun List<Trip>.findFirstDepartureTomorrow(): Int {
   val result = this.binarySearch { it.departures.first() - 24 * SECONDS_IN_HOUR }
   return if (result < 0) -result - 1 else result
}

class RoutesAtStop(
   val first: Boolean,
   val last: Boolean,
   val routes: IntList,
) {
   constructor(flags: Int, routes: IntArray) : this(
      first = flags and 1 != 0,
      last = flags and 2 != 0,
      routes = MutableIntList(routes.size).apply { plusAssign(routes) },
   )
}

class MutableRoutesAtStop(
   private var first: Boolean,
   private var last: Boolean,
   val routes: MutableIntSet,
) {
   constructor(first: Boolean, last: Boolean, firstRoute: Int) : this(
      first,
      last,
      mutableIntSetOf(firstRoute)
   )

   fun addRoute(first: Boolean, last: Boolean, route: Int) {
      if (!first) this.first = false
      if (!last) this.last = false
      routes += route
   }

   val flags get() = first.toInt { 1 } or last.toInt { 2 }
}

typealias RoutesAtStopMap = IntObjectMap<RoutesAtStop>
typealias MutableRoutesAtStopMap = MutableIntObjectMap<MutableRoutesAtStop>

object TripsLoader {

   fun getTripsOfRoute(stopTimesDirectory: File, routeId: RouteId): Trips? {
      val file = File(stopTimesDirectory, routeId.toString())

      if (!file.isFile)
         return null

      return DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
         val commonHeadsign = input.readCommonHeadsign()
         val tripShapes = input.readTripShapes()
         val commonFirstStop = input.readCommonFirstStop()

         val list = buildList<Trip> {
            try {
               while (input.available() > 0) {
                  add(input.readTrip(tripShapes))
               }
            } catch (_: EOFException) {
               // stop loading
            }
            sortWith { st1, st2 -> st1.tripId.compareTo(st2.tripId) }
         }.asSortedListMap { it.tripId }
         Trips(list, commonHeadsign, commonFirstStop, tripShapes)
      }
   }

   private const val STOPS_BY_ROUTE_FILE_NAME = "stops"

   fun getRoutesAtStopMap(stopTimesDirectory: File): RoutesAtStopMap? {
      val file = File(stopTimesDirectory, STOPS_BY_ROUTE_FILE_NAME)

      return try {
         DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            input.readStopsByRoute()
         }
      } catch (e: FileNotFoundException) {
         Log.i(TAG, "getStopsByRoute: file \"$STOPS_BY_ROUTE_FILE_NAME\" not found", e)
         null
      } catch (e: EOFException) {
         Log.w(TAG, "getStopsByRoute: failed to load StopsByRoute", e)
         null
      }
   }

   private const val SERVICE_ID_TYPES_FILE_NAME = "serviceIdTypes"

   fun loadServiceIdTypes(stopTimesDirectory: File): ServiceIdTypes? {
      val file = File(stopTimesDirectory, SERVICE_ID_TYPES_FILE_NAME)

      return try {
         Scanner(file).use { scanner ->
            val map = HashMap<ServiceId, ServiceIdType>()
            while (scanner.hasNext()) {
               val serviceId = scanner.next()

               val serviceIdTypeName = try {
                  scanner.next()
               } catch (_: NoSuchElementException) {
                  Log.w(TAG, "loadServiceIdTypes: incomplete serviceIdTypes file")
                  break
               }
               val serviceIdType = try {
                  ServiceIdType.valueOf(serviceIdTypeName)
               } catch (_: IllegalArgumentException) {
                  Log.w(TAG, "loadServiceIdTypes: invalid serviceIdType: $serviceIdTypeName")
                  break
               }

               map[serviceId] = serviceIdType
            }
            map
         }
      } catch (e: FileNotFoundException) {
         Log.i(TAG, "loadServiceIdTypes: file \"$SERVICE_ID_TYPES_FILE_NAME\" not found", e)
         null
      }
   }

   fun saveServiceIdTypes(stopTimesDirectory: File, serviceIdTypes: ServiceIdTypes) {
      val file = File(stopTimesDirectory, SERVICE_ID_TYPES_FILE_NAME)

      try {
         file.bufferedWriter().use { writer ->
            for ((serviceId, serviceIdType) in serviceIdTypes) {
               writer.write(serviceId)
               writer.write(' '.code)
               writer.write(serviceIdType.name)
               writer.write('\n'.code)
            }
         }
      } catch (e: IOException) {
         Log.w(TAG, "saveServiceIdTypes: failed to write serviceIdTypes", e)
      }
   }


   private class TripPrototype(
      /** The route id of the trip, used for matching [stops] to a shared instance. */
      val routeId: RouteId,
      val serviceId: ServiceId,
      /** The trip id that will directly match the [Trip.tripId]. */
      val tripId: TripId,
      val headsign: String,
      val directionId: Int,
      val blockId: Int,
      var stops: MutableIntList?,
      val departures: MutableIntList,
      var tripShape: Int = -1,
   )

   private val tripMapping: CsvHeaderMapping = { header ->
      val headerMap = IntArray(4) { -1 }
      header.forEachIndexed { i, h ->
         when (h) {
            "trip_id" -> headerMap[0] = i
            "departure_time" -> headerMap[1] = i
            "stop_id" -> headerMap[2] = i
            "stop_sequence" -> headerMap[3] = i
         }
      }
      headerMap
   }

   private val tripPrototypeFactory: SequentialCsvFactory<TripPrototype, Pair<ZippedTrips, MutableIntObjectMap<MutableList<IntList>>>> =
      { headerMap, data, previous, (trips, tripShapes) ->

         val tripId: TripId = data[headerMap[0]]
         val departureTime = data[headerMap[1]]
         val stopId = data[headerMap[2]]
         val stopSequence = data[headerMap[3]].toInt()

         val lastUnderscore = tripId.lastIndexOf('_')
         val beforeUnderscore = tripId.lastIndexOf('_', startIndex = lastUnderscore - 1)
         val routeId = tripId.substring(beforeUnderscore + 1, lastUnderscore).toInt()

         val stopInt = stopId.toStopId().value

         fun String.timeToInt() = (this[0] - '0') * 36000 + (this[1] - '0') * 3600 +
               (this[3] - '0') * 600 + (this[4] - '0') * 60 + (this[6] - '0') * 10 + (this[7] - '0')

         // If we are still reading the same trip,
         if (previous != null && previous.tripId == tripId && previous.stops != null &&
            previous.stops!!.size + 1 == stopSequence
         ) {
            // add it to the list.
            previous.stops!! += stopInt
            previous.departures += departureTime.timeToInt()
            null
         } else {
            // Thankfully, stop times are ordered on trips first, so when we are no longer reading the same
            // trip, we have reached the end of it.

            // If we were reading stop times previously, try matching it to an
            // already existing instance to save memory.
            if (previous?.stops != null) {

               val routeTrips = tripShapes[previous.routeId]

               if (routeTrips == null) {
                  tripShapes[previous.routeId] = mutableListOf(previous.stops!!)
                  previous.tripShape = 0
               } else {
                  val tripShapeIndex = routeTrips.indexOf(previous.stops!!)
                  if (tripShapeIndex != -1) {
                     previous.tripShape = tripShapeIndex
                     previous.stops = null
                  } else {
                     previous.tripShape = routeTrips.size
                     routeTrips += previous.stops!!
                  }
               }
            }
            val trip = trips.list[tripId]
               ?: throw IOException("stopTimes.txt contains a trip_id which doesn't exist in trips.txt")

            TripPrototype(
               routeId,
               trip.serviceId,
               tripId,
               trip.headsign,
               trip.directionId,
               trip.blockId,
               mutableIntListOf(stopInt),
               mutableIntListOf(departureTime.timeToInt())
            )
         }
      }

   private const val DONE_VERSION = "done5"

   fun tripsLoaded(stopTimesDirectory: File) =
      stopTimesDirectory.isDirectory && File(stopTimesDirectory, DONE_VERSION).isFile

   private fun calculateAllCommonHeadsigns(zippedTrips: ZippedTrips): IntObjectMap<Pair<String, String>> {
      val appearances =
         MutableIntObjectMap<Pair<MutableObjectIntMap<String>, MutableObjectIntMap<String>>>()

      for (trip in zippedTrips.list) {
         val (first, second) = appearances.getOrPut(trip.routeId) {
            MutableObjectIntMap<String>() to MutableObjectIntMap()
         }

         val map = if (trip.directionId == 0) first else second

         map[trip.headsign] = map.getOrDefault(trip.headsign, 0) + 1
      }

      val outputMap = MutableIntObjectMap<Pair<String, String>>(appearances.size)

      appearances.forEach { routeId, (first, second) ->
         var firstMaxCount = 0
         var firstMaxSign = ""
         first.forEach { sign, count ->
            if (count > firstMaxCount) {
               firstMaxCount = count
               firstMaxSign = sign
            }
         }

         var secondMaxCount = 0
         var secondMaxSign = ""
         second.forEach { sign, count ->
            if (count > secondMaxCount) {
               secondMaxCount = count
               secondMaxSign = sign
            }
         }

         outputMap[routeId] = firstMaxSign to secondMaxSign
      }

      return outputMap
   }

   private fun calculateCommonFirstStop(
      tripList: List<IntList>,
      trips: List<TripPrototype>,
      beginIndex: Int,
      endIndex: Int,
   ): Pair<StopId, StopId> {
      val appearances = MutableIntIntMap() to MutableIntIntMap()

      for (i in beginIndex..<endIndex) {
         val trip = trips[i]
         val firstStop = tripList[trip.tripShape].first()
         val map =
            if (trip.directionId == 0) appearances.first
            else appearances.second
         map[firstStop] = map.getOrDefault(firstStop, 0) + 1
      }

      val (first, second) = appearances

      var firstMaxCount = 0
      var firstMaxStop = -1
      first.forEach { stop, count ->
         if (count > firstMaxCount) {
            firstMaxCount = count
            firstMaxStop = stop
         }
      }

      var secondMaxCount = 0
      var secondMaxStop = -1
      second.forEach { stop, count ->
         if (count > secondMaxCount) {
            secondMaxCount = count
            secondMaxStop = stop
         }
      }

      return (firstMaxStop.toStopId() to secondMaxStop.toStopId())
   }

   enum class PriorityLevel { Hidden, Background, Foreground }

   fun loadTrips(zipFile: File, stopTimesDirectory: File, priorityLevel: PriorityLevel) {
      val loadingState = when (priorityLevel) {
         PriorityLevel.Hidden -> null

         PriorityLevel.Background ->
            Schedule.Companion.TrackableLoadingState("Ažuriranje rasporeda${Typography.ellipsis}")
               .also { Schedule.loadingState = it }

         PriorityLevel.Foreground ->
            Schedule.Companion.TrackableLoadingState("Pripremanje rasporeda za prvo pokretanje${Typography.ellipsis}")
               .also { Schedule.priorityLoadingState = it }
      }

      try {
         loadTrips(zipFile, stopTimesDirectory, loadingState)
      } finally {
         when (priorityLevel) {
            PriorityLevel.Hidden -> {}
            PriorityLevel.Background -> Schedule.loadingState = null
            PriorityLevel.Foreground -> Schedule.priorityLoadingState = null
         }
      }
   }

   private fun loadTrips(
      zipFile: File,
      stopTimesDirectory: File,
      loadingState: Schedule.Companion.TrackableLoadingState?,
   ) {
      if (!stopTimesDirectory.isDirectory && !stopTimesDirectory.mkdir())
         throw IOException("Cannot create stopTimesDirectory")

      val time1 = System.nanoTime()

      // keys = routeId, value = list of stopTime stops
      val tripShapes = MutableIntObjectMap<MutableList<IntList>>()

      val zippedTrips: ZippedTrips
      val stopTimes: MutableList<TripPrototype>
      ZipFile(zipFile).use {
         zippedTrips = ZippedTrips(it)
         stopTimes = CSVReader(it.getInputStream(it.getEntry("stop_times.txt")).bufferedReader())
            .toListSequential(
               zippedTrips to tripShapes,
               tripMapping,
               tripPrototypeFactory,
               loadingState
            )
      }

      kotlin.run { // using run so the names last and routeTrips do not leak out (so I can reuse them)
         val last = stopTimes.last()
         val routeTrips = tripShapes[last.routeId]

         if (routeTrips == null) {
            tripShapes[last.routeId] = mutableListOf(last.stops!!)
            last.tripShape = 0
         } else {
            val tripShapeIndex = routeTrips.indexOf(last.stops!!)
            if (tripShapeIndex != -1) {
               last.tripShape = tripShapeIndex
               last.stops = null // release the duplicate list to free memory
            } else {
               last.tripShape = routeTrips.size
               routeTrips += last.stops!!
            }
         }
      }

      stopTimes.sortBy { it.routeId }

      loadingState?.progress = .8f

      val commonHeadsigns = calculateAllCommonHeadsigns(zippedTrips)

      loadingState?.progress = .85f

      var currentRouteId = stopTimes[0].routeId
      var beginIndex = 0
      var i = 0
      while (true) {
         i++
         if (i != stopTimes.size && stopTimes[i].routeId == currentRouteId) continue

         val tripList = tripShapes[currentRouteId]!!
         val commonHeadsign = commonHeadsigns[currentRouteId]!!
         val commonFirstStop = calculateCommonFirstStop(tripList, stopTimes, beginIndex, i)

         DataOutputStream(
            BufferedOutputStream(
               FileOutputStream(File(stopTimesDirectory, currentRouteId.toString()))
            )
         ).use { output ->
            output.write(commonHeadsign)
            output.write(tripShapes[currentRouteId]!!)
            output.write(commonFirstStop)
            for (j in beginIndex..<i)
               output.write(stopTimes[j])
         }

         if (i == stopTimes.size)
            break

         beginIndex = i
         currentRouteId = stopTimes[i].routeId
      }


      loadingState?.progress = .9f

      val stopsByRoute = MutableRoutesAtStopMap()

      for (stopTime in stopTimes) {
         val trip = zippedTrips.list[stopTime.tripId]
         val sign = if (trip?.directionId == 1) -1 else 1
         val stops = stopTime.stops
         stops?.forEachIndexed { index, stopId ->
            val first = index == 0
            val last = index == stops.lastIndex
            val stopByRoute = stopsByRoute[stopId]
            if (stopByRoute != null) stopByRoute.addRoute(first, last, stopTime.routeId * sign)
            else stopsByRoute[stopId] = MutableRoutesAtStop(first, last, stopTime.routeId * sign)
         }
      }

      loadingState?.progress = .95f

      DataOutputStream(
         BufferedOutputStream(FileOutputStream(File(stopTimesDirectory, STOPS_BY_ROUTE_FILE_NAME)))
      ).use { output ->
         output.write(stopsByRoute)
      }

      val time2 = System.nanoTime() - time1

      File(stopTimesDirectory, DONE_VERSION).createNewFile()

      Log.d(TAG, "loadStopTimes: loaded stop times! time took: ${time2 / 1_000_000_000.0} s")
   }

   @JvmName("writeCommonHeadsign") // this function and writeCommonFirstStop have the same signature
   private fun DataOutputStream.write(commonHeadsign: Pair<String, String>) {
      writeShortString(commonHeadsign.first)
      writeShortString(commonHeadsign.second)
   }

   private fun DataInputStream.readCommonHeadsign(): Pair<String, String> {
      val first = readShortString()
      val second = readShortString()
      return first to second
   }

   @JvmName("writeCommonFirstStop") // this function and writeCommonHeadsign have the same signature
   private fun DataOutputStream.write(commonFirstStop: Pair<StopId, StopId>) {
      writeInt(commonFirstStop.first.value)
      writeInt(commonFirstStop.second.value)
   }

   private fun DataInputStream.readCommonFirstStop(): Pair<StopId, StopId> {
      val first = readInt()
      val second = readInt()
      return first.toStopId() to second.toStopId()
   }

   private fun DataOutputStream.write(tripShapes: List<IntList>) {
      writeByte(tripShapes.size)
      for (tripShape in tripShapes) {
         writeByte(tripShape.size)
         tripShape.forEach { writeInt(it) }
      }
   }

   private fun DataInputStream.readTripShapes(): List<IntList> {
      val size = readUnsignedByte()
      return List(size) {
         val tripShapeSize = readUnsignedByte()
         MutableIntList(tripShapeSize).apply {
            repeat(tripShapeSize) { add(readInt()) }
         }
      }
   }

   private fun DataOutputStream.write(stopTime: TripPrototype) {
      writeShort(stopTime.routeId)
      writeShortString(stopTime.serviceId)
      writeShortString(stopTime.tripId)
      writeShortString(stopTime.headsign)
      writeByte(stopTime.directionId)
      writeInt(stopTime.blockId)
      writeByte(stopTime.tripShape)
      writeByte(stopTime.departures.size)
      stopTime.departures.forEach { writeInt(it) }
   }

   private fun DataInputStream.readTrip(tripShapes: List<IntList>): Trip {
      val routeId = readUnsignedShort()
      val serviceId = readShortString()
      val tripId = readShortString()
      val headsign = readShortString()
      val directionId = readByte().toInt()
      val blockId = readInt()

      val tripShape = readUnsignedByte()
      val stops = tripShapes[tripShape]

      val departuresSize = readUnsignedByte()
      val departures = MutableIntList(departuresSize)
      repeat(departuresSize) { departures += readInt() }

      return Trip(
         routeId,
         serviceId,
         tripId,
         headsign,
         directionId,
         blockId,
         stops,
         departures,
         tripShape
      )
   }

   private fun DataOutputStream.write(stopsByRoute: MutableRoutesAtStopMap) {
      writeInt(stopsByRoute.size)
      stopsByRoute.forEach { stopId, stopByRoute ->
         writeInt(stopId)
         writeByte(stopByRoute.flags)
         val array = stopByRoute.routes.toIntArray().sortedBy { it.absoluteValue }
         writeByte(array.size)
         for (route in array) writeShort(route)
      }
   }

   private fun DataInputStream.readStopsByRoute(): RoutesAtStopMap {
      val size = readInt()
      val map = MutableIntObjectMap<RoutesAtStop>(size)

      repeat(size) {
         val stopId = readInt()
         val flags = readUnsignedByte()
         val routesSize = readUnsignedByte()
         val routes = IntArray(routesSize) { readShort().toInt() }
         map[stopId] = RoutesAtStop(flags, routes)
      }

      return map
   }

}
