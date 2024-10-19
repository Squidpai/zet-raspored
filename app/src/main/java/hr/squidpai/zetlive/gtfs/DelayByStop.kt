package hr.squidpai.zetlive.gtfs

import androidx.collection.IntList
import com.google.transit.realtime.GtfsRealtime
import hr.squidpai.zetlive.SECONDS_IN_DAY

/**
 * Shows how much a route is late (or early) compared to the
 * time in the schedule.
 */
sealed interface DelayByStop {
  /**
   * Returns the delay time (in seconds) for the stop with the index [index]
   * in the stop sequence of the trip ([Trip.stops]).
   */
  operator fun get(index: Int): Int
}

/**
 * The current trip contains the precise delay for one or multiple
 * stops.
 */
class PreciseDelayByStop(private val source: List<GtfsRealtime.TripUpdate.StopTimeUpdate>) : DelayByStop {

  override fun get(index: Int) =
    (source.lastOrNull { it.stopSequence - 1 <= index } ?: source.first()).departure.delay

  override fun toString() = source.joinToString(prefix = "[", postfix = "]") { it.departure.delay.toString() }
}

/**
 * The current trip does not exist in the live data, or
 * there are no departures in it, or live data doesn't exist.
 */
data object BlankDelayByStop : DelayByStop {
  override fun get(index: Int) = 0
}

/**
 * Returns a specific [DelayByStop] instance appropriate for the current trip.
 */
fun List<GtfsRealtime.TripUpdate.StopTimeUpdate>?.getDelayByStop() =
  if (isNullOrEmpty()) BlankDelayByStop
  else PreciseDelayByStop(this)

/**
 * Returns a specific [DelayByStop] instance appropriate for the given [tripId].
 */
fun Live?.getDelayByStopForTrip(tripId: TripId) =
  this?.findForTripIgnoringServiceId(tripId)?.tripUpdate?.stopTimeUpdateList.getDelayByStop()

/**
 * Calculates the estimated amount the route has traveled between the current and the next stop
 * based on how much time it takes for it to travel between them and when it is arriving at the
 * next stop.
 *
 * @param currentTime the current time in seconds
 */
fun getArrivalLineRatio(departures: IntList, nextStop: Int, delayByStop: DelayByStop, currentTime: Int): Float {
  val stopDiff = departures[nextStop] - departures[nextStop - 1]
  val arrival = (departures[nextStop] + delayByStop[nextStop] - currentTime) % SECONDS_IN_DAY
  return (1 - arrival.toFloat() / stopDiff).coerceIn(0f, 1f)
}
