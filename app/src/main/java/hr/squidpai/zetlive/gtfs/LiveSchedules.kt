package hr.squidpai.zetlive.gtfs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import hr.squidpai.zetlive.*
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * An entry of [RouteLiveSchedule] returned by [getLiveSchedule]. Contains
 * all data required to display to the user where the current trip is.
 */
data class RouteScheduleEntry(
  val nextStopIndex: Int,
  val sliderValue: Float,
  val trip: Trip,
  val overriddenHeadsign: String?,
  val overriddenFirstStop: StopId,
  val departureTime: Int,
)

/**
 * A live schedule of a route. Contains all data used
 * to display all current trips of the given route.
 */
class RouteLiveSchedule(
  val first: List<RouteScheduleEntry>,
  val second: List<RouteScheduleEntry>,
  val commonHeadsign: Pair<String, String>,
)

/**
 * Calculates the live schedule of the given route and updates it every time
 * the schedule or live data change.
 */
@Composable
fun Route.getLiveSchedule(): RouteLiveSchedule? {
  val schedule = Schedule.instance

  val trips = schedule.getTripsOfRoute(id).value ?: return null
  val calendarDates = schedule.calendarDates ?: return null

  val live = Live.instance

  return remember(this, trips, calendarDates, live) {
    val currentMillis = localCurrentTimeMillis()
    val time = (currentMillis % MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS
    val date = currentMillis / MILLIS_IN_DAY
    getLiveSchedule(trips, calendarDates, live, time, date)
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
) = getLiveScheduleData(
  trips, calendarDates, live, time, date,
).mapNoInline {
  RouteScheduleEntry(
    nextStopIndex = it.nextStopIndex,
    sliderValue = if (it.nextStopIndex == 0) -0.5f
    else it.nextStopIndex - 1 + getArrivalLineRatio(it.trip.departures, it.nextStopIndex, it.delayByStop, time),
    trip = it.trip,
    overriddenHeadsign = it.overriddenHeadsign,
    overriddenFirstStop = it.trip.stops[0].toStopId().takeIf { id -> id != trips.commonFirstStop[it.trip.directionId] },
    // departureTime is only used if nextStopIndex == 0
    departureTime = it.trip.departures[0].let { departure ->
      if (departure <= time) -1
      else if (departure - time <= 15 * 60) time - departure - 1
      else departure
    },
  )
}.let { RouteLiveSchedule(it.first, it.second, trips.commonHeadsign) }

/**
 * An entry of [StopLiveSchedule]. Contains all data required to
 * display to the user where the current trip is.
 */
data class StopScheduleEntry(
  val routeNumber: Int,
  val headsign: String,
  val trip: Trip,
  val absoluteTime: Int,
  val relativeTime: Int,
  val useRelative: Boolean,
  val departed: Boolean,
)

/**
 * A live schedule of a stop. Contains all data used
 * to display all current trips of the given stop.
 */
typealias StopLiveSchedule = List<StopScheduleEntry>

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
): StopLiveSchedule? {
  val schedule = Schedule.instance

  val routes = schedule.routes ?: return null
  val calendarDates = schedule.calendarDates ?: return null

  val live = Live.instance

  val routeDirsAtStop = ArrayList<RouteDirAtStop>()
  routesAtStop.routes.forEach { routeAtStopId ->
    val routeId = routeAtStopId.absoluteValue
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

  return remember(this, calendarDates, live, routeDirsAtStop, keepDeparted, maxSize) {
    getLiveSchedule(calendarDates, live, routeDirsAtStop, keepDeparted, maxSize)
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
): StopLiveSchedule {
  val currentMillis = localCurrentTimeMillis()
  val time = (currentMillis % MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS
  val date = currentMillis / MILLIS_IN_DAY

  val list = ArrayList<StopScheduleEntry>()

  fun handleData(routeNumber: Int, data: RouteScheduleData, commonHeadsign: String) {
    val stopIndex = data.trip.stops.indexOf(this.id.value)
    if (stopIndex == -1) return

    if (!keepDeparted && data.nextStopIndex > stopIndex)
      return

    val departureTime = data.trip.departures[stopIndex] + data.delayByStop[stopIndex]

    val useRelative = data.delayByStop != BlankDelayByStop

    list += StopScheduleEntry(
      routeNumber = routeNumber,
      headsign = data.overriddenHeadsign ?: commonHeadsign,
      trip = data.trip,
      absoluteTime = departureTime,
      relativeTime = max(departureTime - time, 0),
      useRelative = useRelative,
      departed = data.nextStopIndex > stopIndex
    )
  }

  for ((route, trips, buildFirst, buildSecond) in routeDirsAtStop) {
    route ?: continue
    trips ?: continue

    val commonHeadsign = trips.commonHeadsign

    val (first, second) = route.getLiveScheduleData(
      trips, calendarDates, live, time, date,
      preferredSize = 20,
      buildFirstList = buildFirst,
      buildSecondList = buildSecond,
    )

    for (data in first)
      handleData(
        routeNumber = route.id,
        data = data,
        commonHeadsign = commonHeadsign.first,
      )
    for (data in second)
      handleData(
        routeNumber = route.id,
        data = data,
        commonHeadsign = commonHeadsign.second,
      )
  }

  list.sortBy { it.absoluteTime }

  return if (list.size <= maxSize) list else list.subList(0, maxSize)
}

private data class RouteScheduleData(
  val nextStopIndex: Int,
  val trip: Trip,
  val overriddenHeadsign: String?,
  val delayByStop: DelayByStop,
)

private typealias RouteLiveScheduleData = ListPair<RouteScheduleData>

private fun Route.getLiveScheduleData(
  trips: Trips,
  calendarDates: CalendarDates,
  live: Live,
  time: Int,
  date: Long,
  preferredSize: Int = 4,
  buildFirstList: Boolean = true,
  buildSecondList: Boolean = true,
): RouteLiveScheduleData {
  val commonHeadsign = trips.commonHeadsign

  val serviceId = calendarDates[date] ?: return emptyListPair()

  val filteredStopTimes = trips.list
    .filterByServiceId(serviceId)
    .filterByDirection()
    .sortedByDepartures()

  val firstList = mutableListOf<RouteScheduleData>()
  val secondList = mutableListOf<RouteScheduleData>()

  fun buildRouteSchedule(
    list: MutableList<RouteScheduleData>,
    iterator: ListIterator<Trip>,
    directionId: Int,
    commonHeadsign: String,
  ) {
    fun Trip.toEntry(nextStopIndex: Int = 0, delayByStop: DelayByStop = BlankDelayByStop) =
      RouteScheduleData(
        nextStopIndex = nextStopIndex,
        trip = this,
        overriddenHeadsign = headsign.takeIf { it != commonHeadsign },
        delayByStop = delayByStop,
      )

    if (live.feedMessage != null) {
      live.forEachOfRoute(this.id) {
        val tripId = it.tripUpdate.trip.tripId
        val next = trips.list[tripId]
        if (next?.directionId == directionId) {
          val delayByStop = it.tripUpdate.stopTimeUpdateList.getDelayByStop()
          val nextStop = next.findNextStopIndexToday(time, delayByStop)
          if (0 < nextStop && nextStop < next.stops.size) {
            list += next.toEntry(nextStop, delayByStop)
          }
        }
      }
      list.sortBy { -it.nextStopIndex }
    }

    val firstAfter: Trip
    if (live.feedMessage == null || list.size == 0) while (true) {
      if (!iterator.hasNext()) return

      val next = iterator.next()
      if (time >= next.departures.first()) {
        val delayByStop = live.getDelayByStopForTrip(next.tripId)
        val nextStop = next.findNextStopIndex(time, delayByStop)
        if (nextStop < next.stops.size) {
          list += next.toEntry(nextStop, delayByStop)
        }
      } else {
        firstAfter = next
        break
      }
    } else while (true) {
      if (!iterator.hasNext()) return

      val next = iterator.next()
      if (time < next.departures.first()) {
        firstAfter = next
        break
      }
    }

    list += firstAfter.toEntry()

    /*while (list.size < 6) {
      if (!iterator.hasNext()) return

      val next = iterator.next()
      if (next.departures.first() - time < 60 * 60) {
        list += next.toEntry()
      } else break
    }*/

    while (list.size < preferredSize && iterator.hasNext()) {
      list += iterator.next().toEntry()
    }
  }

  if (time < 6 * SECONDS_IN_HOUR) run yesterday@{
    val yesterdayServiceId = calendarDates[date - 1] ?: return@yesterday

    val yStopTimes = if (yesterdayServiceId == serviceId) filteredStopTimes else
      trips.list
        .filterByServiceId(yesterdayServiceId)
        .filterByDirection()
        .sortedByDepartures()

    val yIndices = yStopTimes.findFirstDepartures(time + SECONDS_IN_DAY)

    if (buildFirstList)
      buildRouteSchedule(firstList, yStopTimes.first.listIterator(yIndices.first), 0, commonHeadsign.first)
    if (buildSecondList)
      buildRouteSchedule(secondList, yStopTimes.second.listIterator(yIndices.second), 1, commonHeadsign.second)
  }

  val indices = filteredStopTimes.findFirstDepartures(time)

  val firstIterator = filteredStopTimes.first.listIterator(indices.first)
  val secondIterator = filteredStopTimes.second.listIterator(indices.second)

  if (buildFirstList)
    buildRouteSchedule(firstList, firstIterator, 0, commonHeadsign.first)
  if (buildSecondList)
    buildRouteSchedule(secondList, secondIterator, 1, commonHeadsign.second)

  return RouteLiveScheduleData(firstList, secondList)
}
