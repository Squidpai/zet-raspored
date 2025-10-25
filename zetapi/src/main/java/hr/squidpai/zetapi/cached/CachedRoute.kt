package hr.squidpai.zetapi.cached

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import hr.squidpai.zetapi.DirectionId
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.PairAsListSerializer
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.ServiceId
import hr.squidpai.zetapi.Shapes
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.TripId
import hr.squidpai.zetapi.Trips
import hr.squidpai.zetapi.get
import hr.squidpai.zetapi.readNextIntList
import hr.squidpai.zetapi.realtime.RealtimeDispatcher
import hr.squidpai.zetapi.toStopId
import hr.squidpai.zetapi.writeNext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.channels.FileChannel
import java.util.zip.ZipFile

private val spaceHyphenSpaceRegex = Regex(" *- *")

/** Trips 'N' Stop sequences */
private typealias TNS = Pair<Trips, Pair<List<List<Stop>>, List<List<Stop>>>>

internal class CachedRoute(
    id: RouteId,
    shortName: String,
    longName: String,
    type: Type,
    sortOrder: Int,
    commonHeadsigns: Map<ServiceId, Pair<String, String>>,
    private val scheduleFile: File,
    private val shapes: Shapes,
    private val realtimeDispatcher: RealtimeDispatcher,
) : Route(
    id, shortName,
    longName = longName.replace(spaceHyphenSpaceRegex, " ${Typography.ndash} "),
    type, sortOrder, commonHeadsigns,
) {

    constructor(
        data: Array<out String>,
        scheduleFile: File,
        shapes: Shapes,
        realtimeDispatcher: RealtimeDispatcher,
    ) : this(
        id = data[0],
        shortName = data[1],
        longName = data[2],
        type = Type(data[3]),
        sortOrder = data[4].toInt(),
        commonHeadsigns = Json.decodeFromString(CommonHeadsignsSerializer, data[5]),
        scheduleFile, shapes, realtimeDispatcher
    )

    override val trips get() = getTNS().first

    override val stopSequences get() = getTNS().second

    private var tns: TNS? = null

    internal lateinit var stops: Stops

    private fun CSVReader.readStops(): List<Stop> {
        val next = readNext()
        return List(next.size) {
            // TODO update stops when Love updates
            stops[Love.redirectMeToTheBetterStopId(
                id, next[it].toStopId()
            )]!!
        }
    }

    @Synchronized
    private fun getTNS(): TNS {
        tns?.let { return it }

        // gets a shared lock on the file
        FileChannel.open(scheduleFile.toPath()).lock(0, Long.MAX_VALUE, true).use {
            ZipFile(scheduleFile).use { zip ->
                val reader = CSVReader(
                    zip.getInputStream(zip.getEntry(id)).bufferedReader()
                )
                val sizes = reader.readNext()
                val first = List(sizes[0].toInt()) { reader.readStops() }
                val second = List(sizes[1].toInt()) { reader.readStops() }
                val stopSequences = first to second

                val trips = LinkedHashMap<TripId, Trip>()
                repeat(sizes[2].toInt()) {
                    val data = reader.readNext()
                    val departures = reader.readNextIntList()

                    val tripId = data[1]
                    val directionId = DirectionId(data[3])
                    val stopSequenceId = data[6].toInt()
                    trips[tripId] = Trip(
                        route = this,
                        serviceId = data[0],
                        tripId,
                        headsign = data[2],
                        directionId,
                        blockId = data[4],
                        shape = shapes[data[5]]
                            ?: throw NullPointerException("No shape id ${data[5]}"),
                        stops = stopSequences[directionId][stopSequenceId],
                        departures,
                        stopSequenceId,
                        isHeadsignCommon = data[7].toBoolean(),
                        isFirstStopCommon = data[8].toBoolean(),
                        realtimeDispatcher,
                    )
                }

                return (trips to stopSequences).also { tns = it }
            }
        }
    }

    companion object {
        fun save(route: Route, writer: CSVWriter) = writer.writeNext(
            route.id, route.shortName, route.longName,
            route.type.value.toString(), route.sortOrder.toString(),
            Json.encodeToString(
                CommonHeadsignsSerializer,
                route.commonHeadsigns
            )
        )

        fun saveTNS(route: Route, writer: CSVWriter) {
            val stopSequences = route.stopSequences
            val trips = route.trips
            val firstSize = stopSequences.first.size
            val secondSize = stopSequences.second.size
            val tripsSize = trips.size

            writer.writeNext(firstSize, secondSize, tripsSize)
            for (sequence in stopSequences.first)
                writer.writeNext(Array(sequence.size) { sequence[it].id.toString() })
            for (sequence in stopSequences.second)
                writer.writeNext(Array(sequence.size) { sequence[it].id.toString() })

            for (trip in trips.values) {
                writer.writeNext(
                    trip.serviceId, trip.tripId, trip.headsign,
                    trip.directionId.toString(), trip.blockId, trip.shape.id,
                    trip.stopSequenceId.toString(), trip.isHeadsignCommon.toString(),
                    trip.isFirstStopCommon.toString()
                )
                writer.writeNext(trip.departures)
            }
        }
    }
}

private val CommonHeadsignsSerializer = MapSerializer(
    keySerializer = String.serializer(),
    valueSerializer = PairAsListSerializer(String.serializer()),
)
