package hr.squidpai.zetapi

import androidx.collection.IntList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Class containing a single datum from the GTFS schedule file "stops.txt". */
public class Stop internal constructor(
    /**
     * Identifies a location.
     *
     * ZET's stop id are of form: [parentId]_[code] when a child stop,
     * or just the [StopNumber] when a parent stop.
     */
    public val id: StopId,
    /**
     * Usually, short text or a number that identifies the location for riders.
     *
     * However, ZET doesn't use these codes on their stops.
     * Instead, these are usually used arbitrarily or to identify
     * the direction of travel for a stop.
     */
    public val code: StopCode,
    /** Name of the location. */
    public val name: String,
    /** Latitude of the location. */
    public val latitude: Float,
    /** Longitude of the location. */
    public val longitude: Float,
    /** Contains the ID of the parent stop, or -1 if this is the parent stop. */
    //@Deprecated("Use the parent property.", ReplaceWith("parent?.id"))
    public val parentId: StopNumber,
    //public val parent: Stop?,
    /**
     * Set of all routes that stop at this stop.
     *
     * If it is a parent stop, this contains routes of all its child stops.
     *
     * If it is a child stop, this contains only routes that stop at
     * this specific child stop.
     */
    public val routes: Map<Route, RouteAtStop>,
) {

    private var _fullName: Pair<Int, String>? = null

    public val fullName: String
        get() {
            val implKey = Love.implKey()
            return _fullName?.takeIf { it.first == implKey }?.second
                ?: (Love.giveMeTheExtraKeywordForStop(id.stopNumber) ?: name)
                    .also { _fullName = implKey to it }
        }

    private var routesString: String? = null

    public val allRoutesListed: String
        get() = routesString ?: routes.keys.sortedBy { it.sortOrder }
            .joinToString { it.shortName }
            .also { routesString = it }

    /** Returns `parentId`, or `id.stopNumber` if `parentId` is -1. */
    public val parentIdOrSelf: StopNumber
        get() = if (parentId != -1) parentId else id.stopNumber

    override fun equals(other: Any?): Boolean =
        this === other || other is Stop && this.id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "Stop(id=$id, code=$code, name=$name, latitude=$latitude, " +
                "longitude=$longitude, parentId=$parentId)"

    internal fun copy(
        code: StopCode,
        routes: Set<RouteId>,
        // TODO make these parameters not optional
        latitude: Float = this.latitude,
        longitude: Float = this.longitude,
    ) = Stop(
        id = StopId(id.stopNumber, code),
        code, name, latitude, longitude, parentId,
        routes = this.routes.filterKeys { it.id in routes },
    )

    internal fun copy(id: StopId) =
        Stop(id, code = id.stopCode, name, latitude, longitude, parentId = id.stopNumber, routes)

    internal fun copy(routes: Map<Route, RouteAtStop>) =
        Stop(id, id.stopCode, name, latitude, longitude, parentId, routes)
}

/** Unique [Stop] identifier. */
@JvmInline
@Serializable(with = StopId.Serializer::class)
public value class StopId(public val rawValue: Int) : Comparable<StopId> {

    public constructor(stopNumber: StopNumber, stopCode: StopCode) :
            this(stopNumber shl 16 or stopCode)

    public val stopNumber: StopNumber get() = rawValue shr 16

    public val stopCode: StopCode get() = rawValue and 0xFFFF

    override fun compareTo(other: StopId): Int =
        this.rawValue.compareTo(other.rawValue)

    override fun toString(): String = "${stopNumber}_$stopCode"

    public fun isInvalid(): Boolean = rawValue == Invalid.rawValue

    public fun isValid(): Boolean = rawValue != Invalid.rawValue

    public inline fun <T> ifValid(action: (StopId) -> T): T? =
        if (this.isValid()) action(this) else null

    public inline fun <T> ifInvalid(action: () -> T): T? =
        if (this.isInvalid()) action() else null

    public operator fun component1(): Int = stopNumber

    public operator fun component2(): Int = stopCode

    public companion object {
        public val Invalid: StopId = StopId(-1)
    }

    public object Serializer : KSerializer<StopId> {
        override val descriptor: SerialDescriptor = SerialDescriptor(
            "hr.squidpai.zetapi.StopId",
            String.serializer().descriptor,
        )

        override fun serialize(encoder: Encoder, value: StopId) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): StopId =
            decoder.decodeString().toStopId()
    }
}

public fun String.toStopId(): StopId = indexOf('_').let {
    if (it == -1) toIntOrHashCode().toParentStopId()
    else StopId(
        substring(0, it).toIntOrHashCode(),
        substring(it + 1).toIntOrHashCode(),
    )
}

/**
 * Returns a [StopId] with `this` as the [stopNumber][StopId.stopNumber],
 * and `0` as the [stopCode][StopId.stopCode].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun StopNumber.toParentStopId(): StopId =
    StopId(this, 0)

/** Returns a [StopId] whose [rawValue][StopId.rawValue] is `this`. */
@Suppress("NOTHING_TO_INLINE")
public inline fun Int.asStopId(): StopId = StopId(this)

/** First part of a [StopId], the parent id. */
public typealias StopNumber = Int

/** Second part of a [StopId], the stop code. */
public typealias StopCode = Int

/** Type alias for a list of [StopId]s. */
public typealias StopIdList = IntList

/** List of [Stop]s, typically extracted from the GTFS schedule file "stops.txt". */
public class Stops internal constructor(source: Map<StopId, Stop>) :
    Map<StopId, Stop> by source {

    internal constructor(source: Iterable<Stop>) :
            this(source.associateBy { it.id })

    public val groupedStops: Map<StopNumber, Grouped> =
        source.values.fold(mutableMapOf<StopNumber, Grouped.Builder>()) { map, stop ->
            val parentId = stop.parentIdOrSelf
            val builder = map.getOrPut(parentId) { Grouped.Builder() }

            if (stop.parentId == -1)
                builder.parentStop = stop
            else {
                builder.childStops += stop
                if (stop.code < 10)
                    builder.routeType = Route.Type.Tram
                else if (builder.routeType.isUnspecified)
                    builder.routeType = Route.Type.Bus
            }

            map
        }.mapValuesNotNull { it.build() }

    public class Grouped private constructor(
        public val parentStop: Stop,
        childStops: List<Stop>,
        public val routeType: Route.Type,
    ) : List<Stop> by childStops {

        public class Builder {
            public lateinit var parentStop: Stop
            public val childStops: MutableList<Stop> = mutableListOf()
            public var routeType: Route.Type = Route.Type.Unspecified

            public fun build(): Grouped? =
                if (childStops.isNotEmpty())
                    Grouped(parentStop, childStops.toList(), routeType)
                else null
        }
    }

}

public fun Iterable<Stops.Grouped>.filter(
    trimmedInput: String
): List<Stops.Grouped> {
    val splitInput = trimmedInput.split(' ')
    return filter { stop ->
        splitInput.all { stop.parentStop.fullName.softContains(it) }
    }
}
