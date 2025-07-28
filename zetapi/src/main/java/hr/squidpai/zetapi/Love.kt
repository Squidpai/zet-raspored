package hr.squidpai.zetapi

import androidx.collection.IntIntPair
import androidx.collection.MutableIntObjectMap
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.parseToYamlNode
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer

public object Love {

    @Suppress("unused")
    private const val TAG = "Love"

    private inline fun <reified T> decodeYamlResource(name: String) =
        Yaml.default.decodeFromStream<T>(
            Love::class.java.getResourceAsStream("/love/$name.yml")!!
        )

    private fun <T> lazyYamlResource(
        deserializer: DeserializationStrategy<T>,
        name: String,
    ) = lazy {
        Yaml.default.decodeFromStream(
            deserializer,
            Love::class.java.getResourceAsStream("/love/$name.yml")!!
        )
    }

    private inline fun <reified T> lazyYamlResource(name: String) =
        lazyYamlResource(Yaml.default.serializersModule.serializer<T>(), name)

    private val stopLabels by lazy {
        MutableIntObjectMap<String>().apply {
            val map = Yaml.default.parseToYamlNode(
                Love::class.java.getResourceAsStream("/love/stop_labels.yml")!!
            ).yamlMap

            for ((stopNumber, labelsByCode) in map.entries) {
                val stopNumber = stopNumber.toInt()

                for ((code, value) in labelsByCode.yamlMap.entries) {
                    put(
                        StopId(stopNumber, code.toInt()).rawValue,
                        value.yamlScalar.content
                    )
                }
            }
        }
    }

    public fun giveMeTheLabelForStop(stopId: StopId): String? = stopLabels[stopId.rawValue]

    public enum class Direction {
        EAST, WEST, NORTH, SOUTH;

        public val stopCode: StopCode get() = ordinal + 1
    }

    private val iconInfo by lazyYamlResource<Map<StopId, Direction>>("icon_info")

    public fun giveMeTheIconCodeForStop(stopId: StopId): Int =
        iconInfo[stopId]?.stopCode ?: stopId.stopCode

    // TODO actually this can probably be read from the ZET website to make fully accessible route names
    private val fullRouteNames by lazyYamlResource<Map<RouteId, String>>("full_route_names")

    public fun giveMeTheFullRouteNameForRoute(routeId: RouteId, longName: String): String? =
        fullRouteNames[routeId]?.takeIf { longName.isShortenedFrom(it) }

    private val extraStopKeywords by lazyYamlResource<Map<StopNumber, String>>("full_stop_names")

    public fun giveMeTheExtraKeywordForStop(stopNumber: StopNumber): String? =
        extraStopKeywords[stopNumber]

    private val fullHeadsignNames by lazyYamlResource<Map<String, String>>("full_headsign_names")

    public fun giveMeTheFullNameForHeadsign(headsign: String): String? =
        fullHeadsignNames[headsign]

    /*@Suppress("unused") // Used for testing the icon info, TODO should probably be moved someplace else...
    public fun testLabels(stops: Stops, routesAtStops: RoutesAtStopMap) {
       for (stop in stops.list) {
          if (stop.code !in 0..4 && stop.code !in 21..24 && stop.id.value !in stopLabels && stop.id.value !in iconInfo) {
             val routesAtStop = routesAtStops[stop.id.value]!!
             if (!routesAtStop.last && !routesAtStop.first)
                Log.i("LabelTest", "Stop ${stop.name} (${stop.id}) invalid")
          }
       }
    }*/

    private val specialTripLabels by lazyYamlResource(
        deserializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = MapSerializer(
                keySerializer = StopId.serializer(),
                valueSerializer = PairAsListSerializer(String.serializer().nullable),
            ),
        ),
        name = "special_trip_labels",
    )

    public fun giveMeTheSpecialTripLabel(trip: Trip): Pair<String?, String?>? =
        specialTripLabels[trip.route.id]?.let { conditions ->
            for ((stopId, label) in conditions) {
                if (stopId.stopNumber >= 0) {
                    if (trip.stops.any { it.id == stopId })
                        return@let label
                } else {
                    val normalizedStopId =
                        StopId(-stopId.stopNumber, stopId.stopCode)
                    if (label[(!trip.directionId)] != null &&
                        trip.stops.none { it.id == normalizedStopId }
                    ) return@let label
                }
            }
            null
        }

    public const val NULL_SERVICE_ID_MESSAGE: String =
        "Ne postoji vozni red za izabrani datum.\nPokušajte se spojiti na " +
                "internet, ako već niste, kako bi se preuzela najnovija inačica rasporeda."

    public const val NO_TRIPS_219_MESSAGE: String =
        "Polaske subotom, nedjeljom i praznikom ostvaruje autobus linije 229 koji " +
                "na Glavnom kolodvoru polazi s perona 10 na Koturaškoj cesti."

    public fun giveMeTheSpecialLabelForNoTrips(
        route: Route,
        serviceId: ServiceId?,
        selectedDate: Long,
        serviceTypes: ServiceTypes?,
    ): String {
        if (serviceId == null)
            return NULL_SERVICE_ID_MESSAGE

        val serviceType =
            if (serviceTypes != null)
                serviceTypes[serviceId] ?: return NULL_SERVICE_ID_MESSAGE
            else ServiceType.ofDate(selectedDate)

        // Route 219 gets a special label.
        if ((serviceType == ServiceType.SATURDAY ||
                    serviceType == ServiceType.SUNDAY) && route.id == "219"
        ) return NO_TRIPS_219_MESSAGE

        if (route.trips.values.any { it.serviceId == serviceId })
            return "Linija nema više polazaka danas."

        return when (serviceType) {
            ServiceType.WEEKDAY -> "Linija nema polazaka na izabrani datum."
            ServiceType.SATURDAY -> "Linija ne vozi vikendom i praznicima."
            ServiceType.SUNDAY -> {
                val saturdayServiceId = serviceTypes?.entries
                    ?.firstOrNull { it.value == ServiceType.SATURDAY }?.key

                if (saturdayServiceId != null &&
                    route.trips.values.none { it.serviceId == saturdayServiceId }
                ) "Linija ne vozi vikendom i praznicima."
                else "Linija ne vozi nedjeljom i praznicima."
            }
        }
    }

    public fun giveMeTheSpecialLabelForNoTrips(
        routes: Collection<Route>,
        filterEmpty: Boolean,
        serviceId: ServiceId?,
        selectedDate: Long,
        serviceTypes: ServiceTypes?,
    ): String {
        if (routes.isEmpty()) return ""

        if (routes.size == 1)
            return giveMeTheSpecialLabelForNoTrips(
                routes.first(), serviceId, selectedDate, serviceTypes
            )

        if (serviceId == null)
            return NULL_SERVICE_ID_MESSAGE

        val serviceType =
            if (serviceTypes != null)
                serviceTypes[serviceId] ?: return NULL_SERVICE_ID_MESSAGE
            else ServiceType.ofDate(selectedDate)

        if (routes.any { route -> route.trips.values.any { it.serviceId == serviceId } })
            return if (filterEmpty) "Na postaji nema više polazaka danas."
            else "Na postaji nema više polazaka danas za izabrane linije."

        return when (serviceType) {
            ServiceType.WEEKDAY -> "Linije nemaju polazaka na izabrani datum."
            ServiceType.SATURDAY -> "Linije ne voze vikendom i praznicima."
            ServiceType.SUNDAY -> {
                val saturdayServiceId = serviceTypes?.entries
                    ?.firstOrNull { it.value == ServiceType.SATURDAY }?.key

                if (saturdayServiceId != null &&
                    routes.all { route ->
                        route.trips.values.none { it.serviceId == saturdayServiceId }
                    }
                ) "Linije ne voze vikendom i praznicima."
                else "Linije ne voze nedjeljom i praznicima."
            }
        }
    }

    public fun giveMeTheServiceIdTypes(
        routes: Routes,
        calendarDates: CalendarDates,
    ): ServiceTypes {
        val serviceIds = calendarDates.serviceIds
        // I've selected 108 as the route with only weekday travels
        val tripsOfRouteWeekdaysOnly = routes["108"]?.trips
            ?: return emptyMap()
        // I've selected 159 as the route with weekday and saturday travels
        val tripsOfRouteWeekdaysAndSaturday = routes["159"]?.trips
            ?: return emptyMap()

        return giveMeTheServiceIdTypes(
            serviceIds,
            tripsOfRouteWeekdaysOnly,
            tripsOfRouteWeekdaysAndSaturday
        )
    }

    public fun giveMeTheServiceIdTypes(
        serviceIds: Iterator<ServiceId>,
        tripsOfRouteWeekdaysOnly: Trips,
        tripsOfRouteWeekdaysAndSaturday: Trips,
    ): ServiceTypes {
        val map = HashMap<ServiceId, ServiceType>()

        for (serviceId in serviceIds) {
            map[serviceId] =
                if (tripsOfRouteWeekdaysOnly.values
                        .any { it.serviceId == serviceId }
                )
                // There exist trips of this route on this service id.
                // Since this route drives on weekdays only, this must be a weekday.
                    ServiceType.WEEKDAY
                else if (tripsOfRouteWeekdaysAndSaturday.values
                        .any { it.serviceId == serviceId }
                )
                // There exist trips of this route on this service id.
                // Since this route drives on weekdays and saturdays,
                // and it is not a weekday, this must be a saturday.
                    ServiceType.SATURDAY
                else
                // There are no trips on the route that drives on weekdays
                // and saturdays, thus this must be a sunday (or a holiday).
                    ServiceType.SUNDAY
        }

        return map
    }

    public fun giveMeTheForcedCommonHeadsign(
        routeId: RouteId
    ): Pair<String?, String?>? = when (routeId) {
        "217" -> "Petruš. nas." to null
        "295" -> "Sajam Jakuševec" to null
        else -> null
    }

    private infix fun StopId.to(other: StopId) =
        IntIntPair(this.rawValue, other.rawValue)

    public fun giveMeTheForcedFirstStop(routeId: RouteId): IntIntPair =
        when (routeId) {
            "217" -> StopId.Invalid to StopId(1081, 21)
            "295" -> StopId.Invalid to StopId(1230, 22)
            else -> StopId.Invalid to StopId.Invalid
        }

    /*
    TODO display which routes have the ability to carry bikes.
    It would be too much to check each specific trip since it would need
    to be manually checked, rather just show that the routes can carry
    bikes and tell the user to check the official route schedule for specifics.
    routes: 102, 103, 140
     */

    private val betterStopMapper = HashMap<StopId, (Stop) -> List<Stop>>()

    /** Value must be either `Map<RouteId, StopId>`, or just `StopId`. */
    private val routeToBetterStopMapper = HashMap<StopId, Any>()

    private class BetterStopMapper(
        val routeStopMapper: Map<StopCode, Set<RouteId>>
    ) : (Stop) -> List<Stop> {
        override fun invoke(stop: Stop): List<Stop> {
            val remainingRoutes = stop.routes.keys.mapTo(mutableSetOf()) { it.id }
            val stops = ArrayList<Stop>()
            for ((newStopCode, mappedRoutes) in routeStopMapper) {
                stops += stop.copy(newStopCode, mappedRoutes)
                remainingRoutes -= mappedRoutes
            }
            if (remainingRoutes.isNotEmpty())
                stops += stop.copy(routes = stop.routes.filterKeys { it.id in remainingRoutes })
            return stops
        }
    }

    private fun putBetterStopMapping(
        stopIdToTransform: StopId,
        routeStopMapper: Map<StopCode, Set<RouteId>>
    ) {
        val stopNumber = stopIdToTransform.stopNumber
        betterStopMapper[stopIdToTransform] = BetterStopMapper(routeStopMapper)
        val betterRouteMap = mutableMapOf<RouteId, StopId>()
        for ((newStopCode, mappedRoutes) in routeStopMapper) {
            for (routeId in mappedRoutes)
                betterRouteMap[routeId] = StopId(stopNumber, newStopCode)
        }
        routeToBetterStopMapper[stopIdToTransform] = betterRouteMap
    }

    private fun putBetterStopMapping(sourceStopId: StopId, destinationStopId: StopId) {
        betterStopMapper[sourceStopId] = { listOf(it.copy(id = destinationStopId)) }
        routeToBetterStopMapper[sourceStopId] = destinationStopId
    }

    @Serializable
    private data class StopMapperData(
        val stopsToSplit: Map<StopId, Map<StopCode, Set<RouteId>>>,
        val stopsToMove: Map<StopId, StopId>,
    )

    internal fun makeMeABetterStopMapper() {
        val (stopsToSplit, stopsToMove) = decodeYamlResource<StopMapperData>("better_stop_mapper")
        for ((stopIdToTransform, routeStopMapper) in stopsToSplit)
            putBetterStopMapping(
                stopIdToTransform,
                routeStopMapper,
            )
        for ((sourceStopId, destinationStopId) in stopsToMove)
            putBetterStopMapping(sourceStopId, destinationStopId)
    }

    init {
        makeMeABetterStopMapper()
    }

    private fun mergeStops(oldStop: Stop, newStop: Stop): Stop {
        val newRoutes = oldStop.routes.toMutableMap()
        for ((route, routeAtStop) in newStop.routes) {
            val sameRouteAtStop = newRoutes[route]

            if (sameRouteAtStop == null)
                newRoutes[route] = routeAtStop
            else {
                newRoutes[route] = RouteAtStop(
                    // this is counted as a first/last stop for the route
                    // if for ALL trips this is the first/last stop
                    isFirst = sameRouteAtStop.isFirst && routeAtStop.isFirst,
                    isLast = sameRouteAtStop.isLast && routeAtStop.isLast,
                    // the route stops in a certain direction
                    // if ANY trip stops in that direction
                    stopsAtDirectionZero = sameRouteAtStop.stopsAtDirectionZero || routeAtStop.stopsAtDirectionZero,
                    stopsAtDirectionOne = sameRouteAtStop.stopsAtDirectionOne || routeAtStop.stopsAtDirectionOne,
                )
            }
        }

        return oldStop.copy(routes = newRoutes)
    }

    internal fun giveMeBetterStops(stops: Iterable<Stop>): MutableMap<StopId, Stop> {
        val stops = stops.associateByTo(mutableMapOf()) { it.id }

        for ((stopIdToTransform, mapper) in betterStopMapper) {
            val stopToTransform = stops[stopIdToTransform]
                ?: continue

            stops -= stopIdToTransform

            val betterStops = mapper(stopToTransform)
            for (betterStop in betterStops) {
                val oldStop = stops[betterStop.id]
                if (oldStop == null)
                    stops[betterStop.id] = betterStop
                else
                    stops[betterStop.id] = mergeStops(oldStop, betterStop)
            }
        }

        return stops
    }

    internal fun redirectMeToTheBetterStopId(forRoute: RouteId, stopId: StopId): StopId =
        when (val mapper = routeToBetterStopMapper[stopId]) {
            null -> stopId
            is StopId -> mapper
            is Map<*, *> -> mapper[forRoute] as StopId
            else -> throw ClassCastException("Illegal value for $mapper")
        }

}
