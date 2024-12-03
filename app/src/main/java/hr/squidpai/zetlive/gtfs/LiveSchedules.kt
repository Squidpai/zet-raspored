package hr.squidpai.zetlive.gtfs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import hr.squidpai.zetlive.MILLIS_IN_DAY
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.SECONDS_IN_HOUR
import hr.squidpai.zetlive.get
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.none
import kotlin.math.absoluteValue

@Suppress("UNUSED")
private const val TAG = "LiveSchedule"

/**
 * An entry of [RouteLiveSchedule] returned by [getLiveSchedule]. Contains
 * all data required to display to the user where the current trip is.
 */
data class RouteScheduleEntry(
   val nextStopIndex: Int,
   val sliderValue: Float,
   val trip: Trip,
   val headsign: String,
   val isHeadsignCommon: Boolean,
   val isFirstStopCommon: Boolean,
   val departureTime: Int,
   val delayAmount: Int,
   val selectedDate: Int,
)

sealed interface RouteLiveSchedule

/**
 * A live schedule of a route. Contains all data used
 * to display all current trips of the given route.
 */
class ActualRouteLiveSchedule(
   val first: List<RouteScheduleEntry>,
   val second: List<RouteScheduleEntry>,
   val commonHeadsign: Pair<String, String>,
) : RouteLiveSchedule

class RouteNoLiveSchedule(val noLiveMessage: String) : RouteLiveSchedule

/**
 * Calculates the live schedule of the given route and updates it every time
 * the schedule or live data change.
 */
@Composable
fun Route.getLiveSchedule(): RouteLiveSchedule? {
   val schedule = Schedule.loadedInstance ?: return null

   val trips = schedule.getTripsOfRoute(id).value ?: return null
   val calendarDates = schedule.calendarDates

   val live = Live.instance

   return remember(this, trips, calendarDates, live) {
      val currentMillis = localCurrentTimeMillis()
      val time = (currentMillis % MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS
      val date = currentMillis / MILLIS_IN_DAY
      getLiveSchedule(trips, calendarDates, live, time, date, schedule.serviceIdTypes)
   }
}

/**
 * Calculates the live schedule of the given route based on the given parameters.
 */
private fun Route.getLiveSchedule(
   trips: Trips,
   calendarDates: CalendarDates,
   live: Live,
   time: Int,
   date: Long,
   serviceIdTypes: ServiceIdTypes?,
) = try {
   val liveScheduleData = getLiveScheduleData(
      trips, calendarDates, live, time, date, serviceIdTypes
   )

   fun List<RouteScheduleData>.mapLiveScheduleData() = map { data ->
      val delay =
         if (data.nextStopIndex == 0) live.getDelayByStopForTrip(data.trip.tripId)[0]
         else 0

      RouteScheduleEntry(
         nextStopIndex = data.nextStopIndex,
         sliderValue = if (data.nextStopIndex == 0) -0.5f
         else data.nextStopIndex - 1 + getArrivalLineRatio(
            data.trip.departures,
            data.nextStopIndex,
            data.delayByStop,
            time
         ),
         trip = data.trip,
         headsign = data.headsign,
         isHeadsignCommon = data.isHeadsignCommon,
         isFirstStopCommon = data.trip.stops[0].toStopId() ==
               liveScheduleData.commonFirstStop?.get(data.trip.directionId),
         // departureTime is only used if nextStopIndex == 0
         departureTime = (data.trip.departures[0] + delay).let { departure ->
            val dateDifference = (date - data.selectedDate).toInt()
            val relativeTime = departure - (time + dateDifference * SECONDS_IN_DAY)
            if (relativeTime <= 0) -1
            else if (relativeTime <= 15 * 60) -relativeTime - 1
            else departure
         },
         delayAmount = delay,
         selectedDate = data.selectedDate,
      )
   }
   ActualRouteLiveSchedule(
      liveScheduleData.first.mapLiveScheduleData(),
      liveScheduleData.second.mapLiveScheduleData(),
      liveScheduleData.commonHeadsign,
   )
} catch (e: NoLiveScheduleException) {
   RouteNoLiveSchedule(e.message ?: Love.NULL_SERVICE_ID_MESSAGE)
}

/**
 * An entry of [StopLiveSchedule]. Contains all data required to
 * display to the user where the current trip is.
 */
data class StopScheduleEntry(
   val routeId: RouteId,
   val headsign: String,
   val trip: Trip,
   val absoluteTime: Int,
   val relativeTime: Int,
   val useRelative: Boolean,
   val departed: Boolean,
   val selectedDate: Int,
)

/**
 * A live schedule of a stop. Contains all data used
 * to display all current trips of the given stop.
 */
typealias StopLiveSchedule = Pair<List<StopScheduleEntry>?, String?>

/**
 * Stores the information of how to build a live schedule
 * of a single route for `Stop.getLiveSchedule`. If the data
 * in this class changes, the live schedule is recalculated.
 */
private data class RouteDirAtStop(
   val route: Route?,
   val trips: Trips?,
   var buildFirst: Boolean,
   var buildSecond: Boolean,
)

/**
 * Calculates the live schedule of the given stop and updates it every time
 * the schedule or live data change.
 */
@Composable
fun Stop.getLiveSchedule(
   routesAtStop: RoutesAtStop,
   keepDeparted: Boolean,
   maxSize: Int,
   routesFiltered: List<Int>? = null,
): StopLiveSchedule? {
   val schedule = Schedule.loadedInstance ?: return null

   val routes = schedule.routes
   val calendarDates = schedule.calendarDates

   val live = Live.instance

   val filterEmpty =
      routesFiltered == null || routesAtStop.routes.none { it.absoluteValue in routesFiltered }

   val routeDirsAtStop = ArrayList<RouteDirAtStop>()
   routesAtStop.routes.forEach { routeAtStopId ->
      val routeId = routeAtStopId.absoluteValue

      if (filterEmpty || routeId in routesFiltered!!) {
         val routeAtStop = routeDirsAtStop.firstOrNull { routeId == it.route?.id }
         if (routeAtStop == null) {
            val route = routes.list.get(key = routeId)
            val trips = schedule.getTripsOfRoute(routeId).value
            routeDirsAtStop += RouteDirAtStop(
               route,
               trips,
               buildFirst = routeAtStopId >= 0,
               buildSecond = routeAtStopId < 0
            )
         } else if (routeAtStopId >= 0)
            routeAtStop.buildFirst = true
         else
            routeAtStop.buildSecond = true
      }
   }

   return remember(this, calendarDates, live, routeDirsAtStop, keepDeparted, maxSize) {
      getLiveSchedule(
         calendarDates,
         live,
         routeDirsAtStop,
         keepDeparted,
         maxSize,
         schedule.serviceIdTypes
      )
   }
}

/**
 * Calculates the live schedule of the given stop based on the given parameters.
 */
private fun Stop.getLiveSchedule(
   calendarDates: CalendarDates,
   live: Live,
   routeDirsAtStop: List<RouteDirAtStop>,
   keepDeparted: Boolean,
   maxSize: Int,
   serviceIdTypes: ServiceIdTypes?,
): StopLiveSchedule {
   val currentMillis = localCurrentTimeMillis()
   val time = (currentMillis % MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS
   val date = currentMillis / MILLIS_IN_DAY

   val list = ArrayList<StopScheduleEntry>()

   fun handleData(routeId: RouteId, data: RouteScheduleData) {
      val stopIndex = data.trip.stops.indexOf(this.id.value)
      if (stopIndex == -1) return

      if (!keepDeparted && data.nextStopIndex > stopIndex)
         return

      val departureTime = data.trip.departures[stopIndex] + data.delayByStop[stopIndex]

      val useRelative = data.delayByStop != BlankDelayByStop

      list += StopScheduleEntry(
         routeId = routeId,
         headsign = data.headsign,
         trip = data.trip,
         absoluteTime = departureTime,
         relativeTime = departureTime - (time + (date - data.selectedDate).toInt() * SECONDS_IN_DAY),
         useRelative = useRelative,
         departed = data.nextStopIndex > stopIndex,
         selectedDate = data.selectedDate,
      )
   }

   for ((route, trips, buildFirst, buildSecond) in routeDirsAtStop) {
      route ?: continue
      trips ?: continue

      val (first, second) = try {
         route.getLiveScheduleData(
            trips, calendarDates, live, time, date, serviceIdTypes,
            preferredSize = 20,
            buildFirstList = buildFirst,
            buildSecondList = buildSecond,
         )
      } catch (e: NoLiveScheduleException) {
         if (e is NullServiceIdLiveScheduleException || routeDirsAtStop.size == 1)
            return null to e.message
         continue
      }

      for (data in first)
         handleData(routeId = route.id, data = data)
      for (data in second)
         handleData(routeId = route.id, data = data)
   }

   list.sortBy { it.relativeTime }

   return (if (list.size <= maxSize) list else list.subList(0, maxSize)) to null
}

private data class RouteScheduleData(
   val nextStopIndex: Int,
   val trip: Trip,
   val headsign: String,
   val isHeadsignCommon: Boolean,
   val delayByStop: DelayByStop,
   val selectedDate: Int,
)

private data class RouteLiveScheduleData(
   val first: List<RouteScheduleData>,
   val second: List<RouteScheduleData>,
   val commonHeadsign: Pair<String, String>,
   val commonFirstStop: Pair<StopId, StopId>?,
)

open class NoLiveScheduleException(message: String) : Exception(message) {
   constructor(
      routeId: RouteId,
      trips: List<Trip>?,
      serviceId: ServiceId?,
      selectedDate: Long,
      serviceIdTypes: ServiceIdTypes?,
   ) : this(
      Love.giveMeTheSpecialLabelForNoTrips(routeId, trips, serviceId, selectedDate, serviceIdTypes)
   )
}

class NullServiceIdLiveScheduleException : NoLiveScheduleException(Love.NULL_SERVICE_ID_MESSAGE)

private fun Route.getLiveScheduleData(
   trips: Trips,
   calendarDates: CalendarDates,
   live: Live,
   time: Int,
   date: Long,
   serviceIdTypes: ServiceIdTypes?,
   preferredSize: Int = 4,
   buildFirstList: Boolean = true,
   buildSecondList: Boolean = true,
): RouteLiveScheduleData {
   val serviceId = calendarDates[date]
      ?: throw NullServiceIdLiveScheduleException()

   val commonHeadsign = trips.commonHeadsignByDay[serviceId]
      ?: throw NoLiveScheduleException(this.id, trips.list, serviceId, date, serviceIdTypes)

   val filteredStopTimes = trips.list
      .filterByServiceId(serviceId).also {
         if (it.isEmpty())
            throw NoLiveScheduleException(this.id, trips.list, serviceId, date, serviceIdTypes)
      }
      .splitByDirection()
      .sortedByDepartures()

   val firstList = mutableListOf<RouteScheduleData>()
   val secondList = mutableListOf<RouteScheduleData>()

   fun buildRouteSchedule(
      list: MutableList<RouteScheduleData>,
      iterator: ListIterator<Trip>,
      directionId: Int,
      commonHeadsign: String,
      usedCommonHeadsign: String? = null,
      isYesterday: Boolean = false,
   ) {
      fun Trip.toEntry(nextStopIndex: Int = 0, delayByStop: DelayByStop = BlankDelayByStop) =
         RouteScheduleData(
            nextStopIndex = nextStopIndex,
            trip = this,
            headsign = headsign ?: commonHeadsign,
            isHeadsignCommon =
            if (usedCommonHeadsign == null) headsign == null
            else (headsign ?: commonHeadsign) == usedCommonHeadsign,
            delayByStop = delayByStop,
            selectedDate = (if (isYesterday) date - 1 else date).toInt(),
         )

      val offsetTime = if (isYesterday) time + SECONDS_IN_DAY else time

      live.forEachOfRoute(this.id) {
         val tripId = it.tripUpdate.trip.tripId
         val next = trips.list[tripId]
         if (next?.directionId == directionId) {
            val delayByStop = it.tripUpdate.stopTimeUpdateList.getDelayByStop()
            val nextStop = next.findNextStopIndexToday(offsetTime, delayByStop)
            if (0 < nextStop && nextStop < next.stops.size) {
               list += next.toEntry(nextStop, delayByStop)
            }
         }
      }
      list.sortBy { -it.nextStopIndex }


      val firstAfter: Trip
      while (true) {
         if (!iterator.hasNext()) return

         val next = iterator.next()

         if (offsetTime >= next.departures.first()) {
            if (list.any { it.trip.blockId == next.blockId })
               continue

            val delayByStop = live.getDelayByStopForTrip(next.tripId)
            val nextStop = next.findNextStopIndex(offsetTime, delayByStop)
            if (nextStop < next.stops.size) {
               list += next.toEntry(nextStop, delayByStop)
            }
         } else if (list.none { it.trip.tripId == next.tripId }) {
            firstAfter = next
            break
         }
      }

      list.sortBy { -it.nextStopIndex }

      list += firstAfter.toEntry()

      /*while (list.size < 6) {
        if (!iterator.hasNext()) return

        val next = iterator.next()
        if (next.departures.first() - time < 60 * 60) {
          list += next.toEntry()
        } else break
      }*/

      while (list.size < preferredSize && iterator.hasNext())
         list += iterator.next().toEntry()
   }

   var yesterdayData: Pair<ServiceId, Pair<String, String>>? = null

   if (time < 6 * SECONDS_IN_HOUR) run yesterday@{
      val yesterdayServiceId = calendarDates[date - 1] ?: return@yesterday

      val yCommonHeadsign = trips.commonHeadsignByDay[yesterdayServiceId] ?: return@yesterday

      val yStopTimes = if (yesterdayServiceId == serviceId) filteredStopTimes else
         trips.list
            .filterByServiceId(yesterdayServiceId)
            .splitByDirection()
            .sortedByDepartures()

      val yIndices = yStopTimes.findFirstDepartures(time + SECONDS_IN_DAY)

      if (buildFirstList)
         buildRouteSchedule(
            firstList,
            yStopTimes.first.listIterator(yIndices.first),
            directionId = 0,
            yCommonHeadsign.first,
            isYesterday = true,
         )
      if (buildSecondList)
         buildRouteSchedule(
            secondList,
            yStopTimes.second.listIterator(yIndices.second),
            directionId = 1,
            yCommonHeadsign.second,
            isYesterday = true,
         )

      if (firstList.isNotEmpty() || secondList.isNotEmpty())
         yesterdayData = yesterdayServiceId to yCommonHeadsign
   }

   val indices = filteredStopTimes.findFirstDepartures(time)

   val firstIterator = filteredStopTimes.first.listIterator(indices.first)
   val secondIterator = filteredStopTimes.second.listIterator(indices.second)

   val usedCommonHeadsign = yesterdayData?.second?.takeUnless { yesterdayData?.first == serviceId }

   if (buildFirstList && firstList.size < preferredSize)
      buildRouteSchedule(
         firstList,
         firstIterator,
         directionId = 0,
         commonHeadsign.first,
         usedCommonHeadsign?.first,
      )
   if (buildSecondList && secondList.size < preferredSize)
      buildRouteSchedule(
         secondList,
         secondIterator,
         directionId = 1,
         commonHeadsign.second,
         usedCommonHeadsign?.second,
      )

   val commonFirstStop = trips.commonFirstStopByDay[yesterdayData?.first ?: serviceId]

   return RouteLiveScheduleData(firstList, secondList, commonHeadsign, commonFirstStop)
}
