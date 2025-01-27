package hr.squidpai.zetapi.realtime

import com.google.transit.realtime.GtfsRealtime.Alert
import com.google.transit.realtime.GtfsRealtime.FeedEntity
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import com.google.transit.realtime.GtfsRealtime.TripUpdate
import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import hr.squidpai.zetapi.TripId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Object containing useful real time data for a trip. */
public sealed interface RealtimeData {

	/**
	 * This trip has a [TripUpdate]. Use the [get] operator to fetch
	 * the delay for a specific stop.
	 */
	public class DelayByStop(public val tripUpdate: TripUpdate) : RealtimeData {
		public operator fun get(stopIndex: Int): Duration =
			(tripUpdate.stopTimeUpdateList
				.lastOrNull { it.stopSequence - 1 <= stopIndex }
				?: tripUpdate.stopTimeUpdateList.first()).departure.delay.seconds

		override fun toString(): String =
			tripUpdate.stopTimeUpdateList
				.joinToString(prefix = "DelayByStop{", postfix = "}") {
					"${it.stopSequence - 1}: ${it.departure.delay}"
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
