package hr.squidpai.zetlive.gtfs

import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.TimeOfDayList

/**
 * Returns the index of the next stop in the [departures] list based on the given [timeOfDay].
 *
 * Note that if all departures are before [timeOfDay],
 * `departures.lastIndex + 1` is returned (`departures.size`).
 */
fun findNextStopIndex(departures: TimeOfDayList, timeOfDay: TimeOfDay): Int {
    departures.forEachIndexed { index, departure ->
        if (TimeOfDay(departure) >= timeOfDay)
            return index
    }
    return departures.size
}