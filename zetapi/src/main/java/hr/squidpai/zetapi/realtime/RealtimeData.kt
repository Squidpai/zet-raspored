package hr.squidpai.zetapi.realtime

import androidx.collection.MutableIntList
import com.google.transit.realtime.GtfsRealtime.Alert
import com.google.transit.realtime.GtfsRealtime.FeedEntity
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import com.google.transit.realtime.GtfsRealtime.TripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate
import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import hr.squidpai.zetapi.TimeOfDayList
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.TripId
import hr.squidpai.zetapi.lerp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Object containing useful real time data for a trip. */
public sealed interface RealtimeData {

    /**
     * This trip has a [TripUpdate]. Use the [get] operator to fetch
     * the delay for a specific stop, or [applyDelays] to apply delays to
     * [Trip.departures] using linear interpolation.
     */
    public class DelayByStop(public val tripUpdate: TripUpdate) : RealtimeData {

        // NOTE: if at any point StopTimeUpdate.stopId should be read,
        //       a new function in Love will need to be created to map to the better stop id.

        private inline val StopTimeUpdate.stopIndex get() = stopSequence - 1

        public operator fun get(stopIndex: Int): Duration =
            (tripUpdate.stopTimeUpdateList
                .lastOrNull { it.stopIndex <= stopIndex }
                ?: tripUpdate.stopTimeUpdateList.first()).departure.delay.seconds

        public fun applyDelays(departures: TimeOfDayList): TimeOfDayList {
            if (tripUpdate.stopTimeUpdateList.isEmpty())
                return departures

            val realtimeDepartures = MutableIntList(departures.size)

            val sortedUpdateList = tripUpdate.stopTimeUpdateList
                .sortedBy { it.stopIndex }

            val iterator = sortedUpdateList.iterator()
            var previous = iterator.next()

            for (i in 0..previous.stopIndex)
                realtimeDepartures += departures[i] + previous.departure.delay

            while (iterator.hasNext()) {
                val current = iterator.next()
                val totalTime = (departures[current.stopIndex] -
                        departures[previous.stopIndex]).toFloat()
                var time = 0
                for (i in previous.stopIndex + 1..current.stopIndex) {
                    time += departures[i] - departures[i - 1]
                    realtimeDepartures += departures[i] + lerp(
                        previous.departure.delay,
                        current.departure.delay,
                        fraction = time / totalTime,
                    )
                }
                previous = current
            }

            for (i in previous.stopIndex + 1..departures.lastIndex)
                realtimeDepartures += departures[i] + previous.departure.delay

            return realtimeDepartures
        }

        override fun toString(): String =
            tripUpdate.stopTimeUpdateList
                .joinToString(prefix = "DelayByStop{", postfix = "}") {
                    "${it.stopIndex}: ${it.departure.delay}"
                }
    }

    /**
     * This trip has a [VehiclePosition]. It contains the
     * [latitude] and the [longitude] of the vehicle.
     */
    public class Position(
        public val vehiclePosition: VehiclePosition
    ) : RealtimeData {
        public val latitude: Float get() = vehiclePosition.position.latitude

        public val longitude: Float get() = vehiclePosition.position.longitude

        override fun toString(): String =
            "VehiclePosition(latitude=$latitude, longitude=$longitude)"
    }

    /** This trip is cancelled. */
    public data object Cancelled : RealtimeData

    /** This trip contains some other [Alert] which we don't recognise. */
    public class OtherAlert(public val alert: Alert) : RealtimeData

    /** This trip contains no realtime data associated with it. */
    public data object None : RealtimeData

    public companion object {
        public fun fromEntity(entity: FeedEntity, tripId: TripId): RealtimeData {
            if (entity.hasTripUpdate() && entity.tripUpdate.trip.tripId == tripId)
                return DelayByStop(entity.tripUpdate)
            if (entity.hasVehicle() && entity.vehicle.trip.tripId == tripId)
                return Position(entity.vehicle)
            if (entity.hasAlert() && entity.alert.informedEntityList
                    .any { it.trip.tripId == tripId }
            ) {
                if (entity.alert.effect == Alert.Effect.NO_SERVICE)
                    return Cancelled
                return OtherAlert(entity.alert)
            }
            return None
        }

        public fun searchForLinearly(
            feedMessage: FeedMessage,
            tripId: TripId,
        ): RealtimeData {
            for (entity in feedMessage.entityList) {
                val data = fromEntity(entity, tripId)
                if (data != None)
                    return data
            }

            return None
        }
    }
}
