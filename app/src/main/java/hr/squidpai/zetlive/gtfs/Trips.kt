package hr.squidpai.zetlive.gtfs

import androidx.collection.IntIntPair
import androidx.collection.IntList
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.mutableIntSetOf
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.SECONDS_IN_HOUR
import hr.squidpai.zetlive.SortedListMap
import hr.squidpai.zetlive.asSortedListMap
import hr.squidpai.zetlive.filterByValue
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.toInt
import kotlin.math.absoluteValue

@Suppress("unused")
private const val TAG = "Trips"

typealias TripId = String

data class Trip(
   val routeId: RouteId,
   val serviceId: ServiceId,
   val tripId: TripId,
   val headsign: String?,
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

typealias CommonHeadsignByDay = Map<ServiceId, Pair<String, String>>

typealias CommonFirstStopByDay = Map<ServiceId, Pair<StopId, StopId>>

class Trips(
   val list: TripsList,
   val commonHeadsignByDay: CommonHeadsignByDay,
   val commonFirstStopByDay: CommonFirstStopByDay,
   val tripShapes: List<IntList>,
)

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

   val routeIds: IntList = MutableIntList().apply {
      routes.forEach { route ->
         val routeAbs = route.absoluteValue
         if (routeAbs !in this)
            add(routeAbs)
      }
   }
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
