package hr.squidpai.zetapi

import hr.squidpai.zetapi.Route.Type.Companion.Bus
import hr.squidpai.zetapi.Route.Type.Companion.Tram

/** Unique [Route] identifier. */
public typealias RouteId = String

/** Class containing a single datum from the GTFS schedule file "routes.txt". */
public abstract class Route internal constructor(
    /** Identifies a route. */
    public val id: RouteId,
    /**
     * Short name of a route. Often a short, abstract identifier
     * (e.g., "32", "100X", "Green") that riders use to identify
     * a route.
     */
    public val shortName: String,
    /**
     * Full name of a route. This name is generally more descriptive
     * than the [shortName] and often includes the route's destination
     * or stop.
     */
    public val longName: String,
    /** Indicates the type of transportation used on a route. */
    public val type: Type,
    /** Orders the routes in a way which is ideal for presentation to customers. */
    public val sortOrder: Int,
    /**
     * Contains all common headsigns by service day for this route.
     *
     * A common headsign is a headsign that appears most often for a route.
     *
     * In values, [Pair.first] contains the common headsign for
     * [DirectionId.Zero], and [Pair.second] for [DirectionId.One].
     */
    public val commonHeadsigns: Map<ServiceId, Pair<String, String>>,
) {

    private var _fullName: String? = null

    public val fullName: String
        get() = _fullName
            ?: (Love.giveMeTheFullRouteNameForRoute(id, longName) ?: longName)
                .also { _fullName = it }

    private var _fullCommonHeadsigns: Map<ServiceId, Pair<String, String>>? = null

    public val fullCommonHeadsigns: Map<ServiceId, Pair<String, String>>
        get() = _fullCommonHeadsigns
            ?: commonHeadsigns.mapValues {
                (Love.giveMeTheFullNameForHeadsign(it.value.first) ?: it.value.first) to
                        (Love.giveMeTheFullNameForHeadsign(it.value.second) ?: it.value.second)
            }.also { _fullCommonHeadsigns = it }

    /**
     * Map of all trips on this route.
     *
     * The trips are sorted by departure.
     */
    public abstract val trips: Trips

    internal abstract val stopSequences: Pair<List<List<Stop>>, List<List<Stop>>>

    public fun sortedTripsOfDay(
        serviceId: ServiceId
    ): Pair<List<Trip>, List<Trip>> {
        val tripsOfDay = trips.values.filter { it.serviceId == serviceId }
        return tripsOfDay.filter { it.directionId.isZero } to
                tripsOfDay.filter { it.directionId.isOne }
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Route && this.id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "Route(id=$id, shortName=$shortName, longName=$longName, " +
                "sortOrder=$sortOrder)"

    /**
     * Indicates the type of transportation used on a route.
     *
     * ZET only has [Bus]es and [Tram]s, so only those fields
     * are given unique names.
     */
    @JvmInline
    public value class Type(public val value: Int) {

        public constructor(input: String?) :
                this(input?.toIntOrNull() ?: Unspecified.value)

        public val isUnspecified: Boolean get() = this == Unspecified

        override fun toString(): String = when (this) {
            Tram -> "Tram"
            Bus -> "Bus"
            Unspecified -> "Unspecified"
            else -> "Type($value)"
        }

        public companion object {
            public val Unspecified: Type = Type(-1)
            public val Tram: Type = Type(0)
            public val Bus: Type = Type(3)
        }
    }
}

/** Type alias for a list of [RouteId]s. */
public typealias RouteIdList = List<String>

/** Type alias for a mutable list of [RouteId]s. */
public typealias MutableRouteIdList = MutableList<String>

/** List of [Route]s, typically extracted from the GTFS schedule file "routes.txt". */
public typealias Routes = Map<RouteId, Route>

public fun Collection<Route>.filter(trimmedInput: String): Collection<Route> {
    if (trimmedInput.isEmpty()) return this

    if (trimmedInput.all { it in '0'..'9' })
        return filter { trimmedInput in it.shortName }

    val splitInput = trimmedInput.split(' ')

    return filter { route ->
        splitInput.all { route.fullName.softContains(it) }
    }
}
