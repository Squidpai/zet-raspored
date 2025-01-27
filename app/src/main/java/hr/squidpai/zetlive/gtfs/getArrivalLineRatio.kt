package hr.squidpai.zetlive.gtfs

import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.TimeOfDayList

/**
 * Calculates the estimated amount the route has traveled between the current and the next stop
 * based on how much time it takes for it to travel between them and when it is arriving at the
 * next stop.
 */
fun getArrivalLineRatio(departures: TimeOfDayList, nextStop: Int, timeOfDay: TimeOfDay): Float {
	val stopDiff = departures[nextStop] - departures[nextStop - 1]
	val arrival = departures[nextStop] - timeOfDay.valueInSeconds
	return (1 - arrival.toFloat() / stopDiff).coerceIn(0f, 1f)
}

fun findNextStopIndex(departures: TimeOfDayList, timeOfDay: TimeOfDay) =
	departures.indexOfFirst { TimeOfDay(it) >= timeOfDay }
