package hr.squidpai.zetlive.gtfs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import hr.squidpai.zetapi.CalendarDates
import hr.squidpai.zetapi.DirectionId
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.ServiceTypes
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.TimeOfDayList
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.filterByDirection
import hr.squidpai.zetapi.filterByServiceId
import hr.squidpai.zetapi.splitByDirection
import hr.squidpai.zetlive.MILLIS_IN_DAY
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.localCurrentTimeMillis
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
private const val TAG = "LiveSchedule"

data class LiveDisplayData(
	val realtimeDepartures: TimeOfDayList?,
	val timeOfDay: TimeOfDay,
	val nextStopIndex: Int,
	val nextStopValue: Float,
)

@Composable
fun getUpdatingLiveDisplayData(trip: Trip, selectedDate: Long): LiveDisplayData {
	var data by remember {
		mutableStateOf(getLiveDisplayData(trip, selectedDate))
	}

	LaunchedEffect(Unit) {
		while (true) {
			delay(5000)
			data = getLiveDisplayData(trip, selectedDate)
		}
	}

	return data
}

fun getLiveDisplayData(trip: Trip, selectedDate: Long): LiveDisplayData {
	val currentTimeMillis = localCurrentTimeMillis().milliseconds
	val selectedDateDays = selectedDate.days

	val appropriateToUseLiveData =
		(trip.departures.first().seconds + selectedDateDays
				- currentTimeMillis).absoluteValue < 12.hours

	val departures =
		if (appropriateToUseLiveData) trip.getRealtimeDepartures()
		else trip.departures

	val timeOfDay = TimeOfDay(currentTimeMillis - selectedDateDays)

	if (departures == null)
		return LiveDisplayData(
			realtimeDepartures = null,
			timeOfDay,
			nextStopIndex = 0,
			nextStopValue = 0f,
		)

	val nextStopIndex = findNextStopIndex(departures, timeOfDay)

	val nextStopValue = when (nextStopIndex) {
		0 -> 0f
		-1 -> trip.departures.size.toFloat()
		else -> nextStopIndex - 1 + getArrivalLineRatio(
			departures,
			nextStopIndex,
			timeOfDay,
		)
	}

	return LiveDisplayData(departures, timeOfDay, nextStopIndex, nextStopValue)
}

/**
 * An entry of [RouteLiveSchedule] returned by [getLiveSchedule]. Contains
 * all data required to display to the user where the current trip is.
 */
data class RouteScheduleEntry(
	val nextStopIndex: Int,
	val sliderValue: Float,
	val trip: Trip,
	val departureTime: Int,
	val delayAmount: Int,
	val selectedDate: Long,
	val isCancelled: Boolean,
)

sealed interface RouteLiveSchedule

/**
 * A live schedule of a route. Contains all data used
 * to display all current trips of the given route.
 */
data class ActualRouteLiveSchedule(
	val first: List<RouteScheduleEntry>,
	val second: List<RouteScheduleEntry>,
	val commonHeadsign: Pair<String, String>,
) : RouteLiveSchedule

class RouteNoLiveSchedule(val noLiveMessage: String) : RouteLiveSchedule

/**
 * Calculates the live schedule of the given route
 * and updates every few seconds.
 */
@Composable
fun Route.getLiveSchedule(): RouteLiveSchedule? {
	val schedule = ScheduleManager.instance ?: return null

	val calendarDates = schedule.calendarDates

	var liveSchedule by remember {
		mutableStateOf(getLiveSchedule(calendarDates, schedule.serviceTypes))
	}

	LaunchedEffect(Unit) {
		while (true) {
			delay(5000)
			liveSchedule = getLiveSchedule(calendarDates, schedule.serviceTypes)
		}
	}

	return liveSchedule
}

/**
 * Calculates the live schedule of the given route based on the given parameters.
 */
private fun Route.getLiveSchedule(
	calendarDates: CalendarDates,
	serviceTypes: ServiceTypes?,
): RouteLiveSchedule {
	val currentMillis = localCurrentTimeMillis()
	val timeOfDay = TimeOfDay(
		(currentMillis % MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS
	)
	val dateEpoch = currentMillis / MILLIS_IN_DAY

	if (timeOfDay.hours < 9) run yesterday@{
		val serviceId = calendarDates[dateEpoch - 1]
			?: return@yesterday
		val tripsOfDay = trips.filterByServiceId(serviceId)
			.splitByDirection()

		val timeByYesterday = timeOfDay + 1.days

		val first = buildLiveSchedule(
			builder = RouteLiveScheduleBuilder,
			tripsOfDay.first, timeByYesterday, dateEpoch - 1,
		)
		val second = buildLiveSchedule(
			builder = RouteLiveScheduleBuilder,
			tripsOfDay.second, timeByYesterday, dateEpoch - 1,
		)

		if (first.isNotEmpty() || second.isNotEmpty())
			return ActualRouteLiveSchedule(
				first, second,
				commonHeadsign = commonHeadsigns[serviceId] ?: ("" to ""),
			)
	}

	val serviceId = calendarDates[dateEpoch]
		?: return RouteNoLiveSchedule(Love.NULL_SERVICE_ID_MESSAGE)
	val tripsOfDay = trips.filterByServiceId(serviceId)
		.splitByDirection()

	val first = buildLiveSchedule(
		builder = RouteLiveScheduleBuilder,
		tripsOfDay.first, timeOfDay, dateEpoch,
	)
	val second = buildLiveSchedule(
		builder = RouteLiveScheduleBuilder,
		tripsOfDay.second, timeOfDay, dateEpoch,
	)

	if (first.isEmpty() && second.isEmpty())
		return RouteNoLiveSchedule(
			Love.giveMeTheSpecialLabelForNoTrips(
				route = this,
				serviceId,
				selectedDate = dateEpoch,
				serviceTypes,
			)
		)

	return ActualRouteLiveSchedule(
		first, second,
		commonHeadsign = commonHeadsigns[serviceId] ?: ("" to "")
	)
}

sealed interface StopLiveSchedule

/**
 * An entry of [StopLiveSchedule]. Contains all data required to
 * display to the user where the current trip is.
 */
data class StopScheduleEntry(
	val route: Route,
	val headsign: String,
	val trip: Trip,
	val absoluteTime: TimeOfDay,
	val relativeTime: Int,
	val useRelative: Boolean,
	val selectedDate: Long,
)

/**
 * A live schedule of a route. Contains all data used
 * to display all current trips of the given route.
 */
class ActualStopLiveSchedule(
	list: List<StopScheduleEntry>
) : List<StopScheduleEntry> by list, StopLiveSchedule

class StopNoLiveSchedule(val noLiveMessage: String?) : StopLiveSchedule

/**
 * Calculates the live schedule of the given stop
 * and updates every few seconds.
 */
@Composable
fun Stop.getLiveSchedule(
	keepDeparted: Boolean,
	maxSize: Int,
	routesFiltered: List<RouteId>? = null,
): StopLiveSchedule? {
	val schedule = ScheduleManager.instance ?: return null

	val calendarDates = schedule.calendarDates

	var liveSchedule by remember {
		mutableStateOf<StopLiveSchedule?>(null)
	}

	LaunchedEffect(this, routesFiltered) {
		while (true) {
			liveSchedule = getLiveSchedule(
				keepDeparted,
				maxSize,
				routesFiltered,
				calendarDates,
				schedule.serviceTypes,
			)
			delay(20000)
		}
	}

	return liveSchedule
}

/**
 * Calculates the live schedule of the given stop based on the given parameters.
 */
private fun Stop.getLiveSchedule(
	keepDeparted: Boolean,
	maxSize: Int,
	routesFiltered: List<RouteId>?,
	calendarDates: CalendarDates,
	serviceTypes: ServiceTypes?,
): StopLiveSchedule {
	val currentMillis = localCurrentTimeMillis()
	val timeOfDay = TimeOfDay(
		(currentMillis % MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS
	)
	val dateEpoch = currentMillis / MILLIS_IN_DAY

	val result = mutableListOf<StopScheduleEntry>()

	val filterEmpty =
		routesFiltered == null || routes.none { it.key.id in routesFiltered }

	if (timeOfDay.hours < 9) run yesterday@{
		val serviceId = calendarDates[dateEpoch - 1]
			?: return@yesterday

		val timeByYesterday = timeOfDay + 1.days
		val tripsOfDay = mutableListOf<Trip>()

		for ((route, routeAtStop) in routes) {
			if (!filterEmpty && route.id !in routesFiltered!!)
				continue
			val routeTripsOfDay = route.trips.filterByServiceId(serviceId)
			if (routeTripsOfDay.isEmpty()) continue

			tripsOfDay += when {
				routeAtStop.stopsAtDirectionZero && routeAtStop.stopsAtDirectionOne ->
					routeTripsOfDay

				routeAtStop.stopsAtDirectionZero ->
					routeTripsOfDay.filterByDirection(DirectionId.Zero)

				// if a route is in `routes`, then it stops at this stop;
				// since it doesn't stop at direction zero,
				// it must stop at direction one
				else -> routeTripsOfDay.filterByDirection(DirectionId.One)
			}
		}

		buildLiveSchedule(
			builder = StopLiveScheduleBuilder(stop = this, keepDeparted),
			tripsOfDay,
			timeOfDay = timeByYesterday,
			dateEpoch,
			appendTo = result,
			preferredSize = maxSize,
			maxSize = maxSize,
		)

		if (result.isNotEmpty()) {
			result.sortBy { it.relativeTime }
			return ActualStopLiveSchedule(result)
		}
	}

	val serviceId = calendarDates[dateEpoch]
		?: return StopNoLiveSchedule(Love.NULL_SERVICE_ID_MESSAGE)
	val tripsOfDay = mutableListOf<Trip>()

	for ((route, routeAtStop) in routes) {
		if (!filterEmpty && route.id !in routesFiltered!!)
			continue
		val routeTripsOfDay = route.trips.filterByServiceId(serviceId)
		if (routeTripsOfDay.isEmpty()) continue

		tripsOfDay += when {
			routeAtStop.stopsAtDirectionZero && routeAtStop.stopsAtDirectionOne ->
				routeTripsOfDay

			routeAtStop.stopsAtDirectionZero ->
				routeTripsOfDay.filterByDirection(DirectionId.Zero)

			// if a route is in `routes`, then it stops at this stop;
			// since it doesn't stop at direction zero,
			// it must stop at direction one
			else -> routeTripsOfDay.filterByDirection(DirectionId.One)
		}
	}

	buildLiveSchedule(
		builder = StopLiveScheduleBuilder(stop = this, keepDeparted),
		tripsOfDay, timeOfDay, dateEpoch,
		appendTo = result,
		preferredSize = maxSize,
		maxSize = maxSize,
	)

	if (result.isEmpty())
		return StopNoLiveSchedule(
			null
			/*Love.giveMeTheSpecialLabelForNoTrips(
				route = TODO(),
				serviceId,
				selectedDate = dateEpoch,
				serviceTypes,
			)*/
		)

	result.sortBy { it.relativeTime }
	return ActualStopLiveSchedule(result)
}

private fun <T> buildLiveSchedule(
	builder: LiveScheduleBuilder<T>,
	tripsOfDay: Collection<Trip>,
	timeOfDay: TimeOfDay,
	dateEpoch: Long,
	appendTo: MutableList<T>? = null,
	preferredSize: Int = 4,
	maxSize: Int = Int.MAX_VALUE,
): List<T> {
	val realtimeTrips = tripsOfDay.mapNotNullTo(mutableListOf()) { trip ->
		trip.getRealtimeDepartures()?.let { trip to it }
	}
	realtimeTrips.sortBy { it.second.first() }

	val firstTripIndex = realtimeTrips.indexOfFirst {
		it.second.last() >= timeOfDay.valueInSeconds
	}
	if (firstTripIndex == -1)
		return emptyList()

	val iterator = realtimeTrips.listIterator(firstTripIndex)

	val result = appendTo ?: mutableListOf()

	for ((trip, realtimeDepartures) in iterator) {
		val nextStopIndex = findNextStopIndex(realtimeDepartures, timeOfDay)
		if (nextStopIndex <= 0) {
			iterator.previous()
			break
		}
		builder.build(
			trip,
			realtimeDepartures,
			nextStopIndex,
			selectedDate = dateEpoch,
			timeOfDay,
		)?.let { result += it }
		if (result.size >= maxSize)
			return result
	}

	// always add at least one entry that hasn't happened yet
	do {
		if (!iterator.hasNext()) break
		val (trip, realtimeDepartures) = iterator.next()
		builder.build(
			trip,
			realtimeDepartures,
			nextStopIndex = 0,
			selectedDate = dateEpoch,
			timeOfDay,
		)?.let { result += it }
	} while (result.size < preferredSize)

	return result
}

private fun interface LiveScheduleBuilder<T> {
	fun build(
		trip: Trip,
		realtimeDepartures: TimeOfDayList,
		nextStopIndex: Int,
		selectedDate: Long,
		timeOfDay: TimeOfDay,
	): T?
}

private val RouteLiveScheduleBuilder =
	LiveScheduleBuilder { trip, realtimeDepartures, nextStopIndex, selectedDate, timeOfDay ->
		if (nextStopIndex != 0) RouteScheduleEntry(
			nextStopIndex,
			sliderValue = nextStopIndex - 1 + getArrivalLineRatio(
				realtimeDepartures, nextStopIndex, timeOfDay
			),
			trip,
			departureTime = 0,
			delayAmount = 0,
			selectedDate,
			isCancelled = false,
		) else RouteScheduleEntry(
			nextStopIndex = 0,
			sliderValue = -.5f,
			trip,
			departureTime = realtimeDepartures[0].let { departure ->
				val difference = TimeOfDay(departure).minusMinutes(timeOfDay)

				if (difference <= 0) -1
				else if (difference <= 15) -difference - 1
				else departure
			},
			delayAmount = realtimeDepartures[0] - trip.departures[0],
			selectedDate,
			isCancelled = false,
		)
	}

private class StopLiveScheduleBuilder(
	private val stop: Stop,
	private val keepDeparted: Boolean,
) : LiveScheduleBuilder<StopScheduleEntry> {
	override fun build(
		trip: Trip,
		realtimeDepartures: TimeOfDayList,
		nextStopIndex: Int,
		selectedDate: Long,
		timeOfDay: TimeOfDay
	): StopScheduleEntry? {
		val indexOfStop = trip.stops.indexOf(stop)
		if (indexOfStop == -1)
			return null

		val absoluteTime = TimeOfDay(realtimeDepartures[indexOfStop])
		val relativeTime = absoluteTime.minusSeconds(timeOfDay)
		if (relativeTime < 0 && !keepDeparted)
			return null

		return StopScheduleEntry(
			route = trip.route,
			headsign = trip.headsign,
			trip,
			absoluteTime,
			relativeTime,
			useRelative = realtimeDepartures !== trip.departures,
			selectedDate,
		)
	}
}

/*
/**
 * An entry of [StopLiveSchedule]. Contains all data required to
 * display to the user where the current trip is.
 */
data class StopScheduleEntry(
   val route: Route,
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
   keepDeparted: Boolean,
   maxSize: Int,
   routesFiltered: List<RouteId>? = null,
): StopLiveSchedule? {
   val schedule = ScheduleManager.loadedInstance ?: return null

   val routes = schedule.routes
   val calendarDates = schedule.calendarDates

   val live = Live.instance

   val filterEmpty =
      routesFiltered == null || routesAtStop.allRoutes.none { it in routesFiltered }

   val routeDirsAtStop = ArrayList<RouteDirAtStop>()
   routesAtStop.allRoutes.forEach { routeId ->
      if (filterEmpty || routeId in routesFiltered!!) {
         val routeAtStop = routeDirsAtStop.firstOrNull { routeId == it.route?.id }
         if (routeAtStop == null) {
            val route = routes[routeId]
            val trips = schedule.getTripsOfRoute(routeId).value
            routeDirsAtStop += RouteDirAtStop(
               route,
               trips,
               buildFirst = routeId in routesAtStop.routes.first,
               buildSecond = routeId in routesAtStop.routes.second,
            )
         } else if (routeId in routesAtStop.routes.first)
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
      val stopIndex = data.trip.stops.indexOf(this.id.rawValue)
      if (stopIndex == -1) return

      if (!keepDeparted && data.nextStopIndex > stopIndex)
         return

      val departureTime = data.trip.departures[stopIndex] + data.delayByStop[stopIndex]

      val useRelative = data.delayByStop != DelayByStop.Blank

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
      trips: Iterable<Trip>?,
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
      ?: throw NoLiveScheduleException(this.id, trips.values, serviceId, date, serviceIdTypes)

   val filteredStopTimes = trips.values
      .filterByServiceId(serviceId).also {
         if (it.isEmpty())
            throw NoLiveScheduleException(this.id, trips.values, serviceId, date, serviceIdTypes)
      }
      .splitByDirection()
      .sortedByDepartures()

   val firstList = mutableListOf<RouteScheduleData>()
   val secondList = mutableListOf<RouteScheduleData>()

   fun buildRouteSchedule(
      list: MutableList<RouteScheduleData>,
      iterator: ListIterator<Trip>,
      directionId: DirectionId,
      commonHeadsign: String,
      usedCommonHeadsign: String? = null,
      isYesterday: Boolean = false,
   ) {
      fun Trip.toEntry(nextStopIndex: Int = 0, delayByStop: DelayByStop = DelayByStop.Blank) =
         RouteScheduleData(
            nextStopIndex = nextStopIndex,
            trip = this,
            headsign = headsign,
            delayByStop = delayByStop,
            selectedDate = (if (isYesterday) date - 1 else date).toInt(),
         )

      val offsetTime = if (isYesterday) time + SECONDS_IN_DAY else time

      live.forEachOfRoute(
         routeId = this.id,
         onTripUpdate = { tripUpdate ->
            val next = trips[tripUpdate.trip.tripId]
            if (next?.directionId == directionId) {
               val delayByStop = tripUpdate.stopTimeUpdateList.getDelayByStop()
               val nextStop = next.findNextStopIndexToday(offsetTime, delayByStop)
               if (0 < nextStop && nextStop < next.stops.size) {
                  list += next.toEntry(nextStop, delayByStop)
               }
            }
         },
         onAlert = { alert ->
            if (alert.effect == GtfsRealtime.Alert.Effect.NO_SERVICE)
               trips[alert.informedEntityList.firstOrNull()?.trip?.tripId]?.let {
                  list += RouteScheduleData(
                     nextStopIndex = 0,
                     trip = it,
                     headsign = it.headsign,
                     delayByStop = DelayByStop.Cancelled,
                     selectedDate = (if (isYesterday) date - 1 else date).toInt(),
                  )
               }
         },
      )

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
         trips.values
            .filterByServiceId(yesterdayServiceId)
            .splitByDirection()
            .sortedByDepartures()

      val yIndices = yStopTimes.findFirstDepartures(time + SECONDS_IN_DAY)

      if (buildFirstList)
         buildRouteSchedule(
            firstList,
            yStopTimes.first.listIterator(yIndices.first),
            DirectionId.Zero,
            yCommonHeadsign.first,
            isYesterday = true,
         )
      if (buildSecondList)
         buildRouteSchedule(
            secondList,
            yStopTimes.second.listIterator(yIndices.second),
            DirectionId.One,
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
         DirectionId.Zero,
         commonHeadsign.first,
         usedCommonHeadsign?.first,
      )
   if (buildSecondList && secondList.size < preferredSize)
      buildRouteSchedule(
         secondList,
         secondIterator,
         DirectionId.One,
         commonHeadsign.second,
         usedCommonHeadsign?.second,
      )

   val commonFirstStop = trips.commonFirstStopByDay[yesterdayData?.first ?: serviceId]

   return RouteLiveScheduleData(firstList, secondList, commonHeadsign, commonFirstStop)
}*/
