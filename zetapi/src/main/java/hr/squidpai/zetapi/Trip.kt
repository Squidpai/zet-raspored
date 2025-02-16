package hr.squidpai.zetapi

import hr.squidpai.zetapi.realtime.RealtimeData
import hr.squidpai.zetapi.realtime.RealtimeDispatcher

/**
 * A merged datum of a trip from the GTFS schedule file "trips.txt",
 * and its associated stop time from the GTFS schedule file "stop_times.txt".
 */
public class Trip internal constructor(
   /** Identifies a route. */
   public val route: Route,
   /**
    * Identifies a set of dates when
    * service is available for one or more routes.
    */
   public val serviceId: ServiceId,
   /** Identifies a trip. */
   public val tripId: TripId,
   /**
    * Text that appears on signage identifying
    * the trip's destination to riders.
    */
   public val headsign: String,
   /** Indicates the direction of travel for a trip. */
   public val directionId: DirectionId,
   /**
    * Identifies the block to which the trip belongs.
    *
    * A block consists of a single trip or many sequential trips made
    * using the same vehicle, defined by shared service days and `blockId`.
    */
   public val blockId: String,
   /**
    * Identifies a geospatial shape describing
    * the vehicle travel path for a trip.
    */
   public val shape: Shape,
   /** List of stops the vehicle stops at. */
   public val stops: List<Stop>,
   /** List of departure times for each stop of [stops]. */
   public val departures: TimeOfDayList,
   internal val stopSequenceId: Int,
   /**
    * Identifies whether most trips of the same [route]
    * and [serviceId] have the same headsign.
    */
   public val isHeadsignCommon: Boolean,
   /**
    * Identifies whether most trips of the same [route]
    * and [serviceId] have the same first stop.
    */
   public val isFirstStopCommon: Boolean,
   private val realtimeDispatcher: RealtimeDispatcher,
) {
   /**
    * Returns the realtime departures for this trip.
    *
    * If the trip is cancelled, `null` is returned.
    *
    * If the trip has no realtime data associated with it,
    * [departures] is returned.
    */
   public fun getRealtimeDepartures(): TimeOfDayList? {
      return when (val data = realtimeDispatcher.getForTrip(tripId)) {
         is RealtimeData.DelayByStop -> data.applyDelays(departures)

         is RealtimeData.Position -> {
            // TODO actually calculate
            departures
         }

         RealtimeData.Cancelled -> null
         else -> departures
      }
   }

   override fun equals(other: Any?): Boolean =
      this === other || (other is Trip && this.tripId == other.tripId)

   override fun hashCode(): Int = tripId.hashCode()

   override fun toString(): String =
      "Trip(routeId=${route.id}, serviceId=$serviceId, tripId=$tripId, " +
            "headsign=$headsign, directionId=$directionId, blockId=$blockId, " +
            "shapeId=${shape.id}, stops=$stops, departures=$departures, " +
            "isHeadsignCommon=$isHeadsignCommon, " +
            "isFirstStopCommon=$isFirstStopCommon)"
}

public typealias TripId = String

@JvmInline
public value class DirectionId private constructor(public val isZero: Boolean) {

   public constructor(value: Int) : this(value == 0)

   public constructor(input: String?) : this(input?.toIntOrNull() ?: 0)

   public val value: Int get() = if (isZero) 0 else 1

   public val isOne: Boolean inline get() = !isZero

   public operator fun not(): DirectionId = DirectionId(!isZero)

   override fun toString(): String = value.toString()

   public companion object {
      public val Zero: DirectionId = DirectionId(0)
      public val One: DirectionId = DirectionId(1)
   }
}

public typealias Trips = Map<TripId, Trip>

public fun Trips.filterByServiceId(serviceId: ServiceId): List<Trip> =
   values.filterByServiceId(serviceId)

public fun Collection<Trip>.filterByServiceId(
   serviceId: ServiceId
): List<Trip> = filter { it.serviceId == serviceId }

public fun Collection<Trip>.splitByDirection(): Pair<List<Trip>, List<Trip>> =
   filter { it.directionId.isZero } to filter { it.directionId.isOne }

public fun Collection<Trip>.filterByDirection(
   directionId: DirectionId
): List<Trip> = filter { it.directionId == directionId }

public fun Pair<Iterable<Trip>, Iterable<Trip>>.sortedByDepartures():
      Pair<List<Trip>, List<Trip>> =
   first.sortedBy { it.departures.first() } to
         second.sortedBy { it.departures.first() }

public fun List<Trip>.findFirstDeparture(timeOfDay: TimeOfDay): Int {
   val firstIndex = binarySearch {
      it.departures.last() - timeOfDay.valueInSeconds
   }

   return if (firstIndex >= 0) firstIndex
   else -firstIndex - 1
}
