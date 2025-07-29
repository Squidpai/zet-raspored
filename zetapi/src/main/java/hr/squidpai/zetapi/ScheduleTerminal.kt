package hr.squidpai.zetapi

import androidx.collection.FloatFloatPair
import hr.squidpai.zetapi.cached.CachedScheduleIO
import hr.squidpai.zetapi.gtfs.GtfsScheduleLoader
import hr.squidpai.zetapi.realtime.EmptyRealtimeDispatcher
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.system.exitProcess

private fun main() {
    while (true) {
        print("> ")
        val line = readln()
        if (line.isBlank())
            continue
        val args = line.split(' ')

        val command = args[0]

        val function = commands[command]

        if (function == null) {
            System.err.println("Unknown command: $command")
            continue
        }

        try {
            function(args.subList(1, args.size))
        } catch (_: NoScheduleException) {
            System.err.println("No schedule loaded")
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        System.err.flush()
        System.out.flush()
    }
}

private var schedule: Schedule? = null

private object NoScheduleException : Throwable() {
    private fun readResolve(): Any = NoScheduleException
}

private fun requireSchedule() = schedule ?: throw NoScheduleException

private val commands = mapOf<String, (List<String>) -> Unit>(
    "exit" to { args -> exitProcess(args.getOrNull(0)?.toIntOrNull() ?: 0) },
    "download" to download@{ args ->
        val version = args.lastOrNull()?.takeUnless { it.startsWith("-") }
        val head = "-head" in args

        val file = File("schedule.zip")

        val versionLink = version?.let { GtfsScheduleLoader.versionLink(it) }
            ?: GtfsScheduleLoader.LINK

        if (head) {
            println("Opening connection...")
            val connection = URI(versionLink).toURL().openConnection() as HttpURLConnection

            //connection.requestMethod = "HEAD"
            try {
                connection.ifModifiedSince = LocalDateTime.of(
                    2025, 7, 3, 11, 18, 2
                ).toEpochSecond(ZoneOffset.UTC) * 1000
                /*Files.readAttributes(
                file.toPath(),
                BasicFileAttributes::class.java,
             ).creationTime().toMillis()*/
            } catch (e: Exception) {
                System.err.println(e)
            }

            println("Connecting...")
            connection.connect()

            println("Response code: ${connection.responseCode}")
            println("Response headers: ${connection.headerFields}")
            println("Body:")
            println(connection.inputStream.bufferedReader().readText())

            connection.disconnect()

            return@download
        }

        println("Downloading schedule...")
        val result = GtfsScheduleLoader.download(
            file,
            link = versionLink,
        )
        if (result.errorType != null) {
            System.err.println("Error while downloading schedule: ${result.errorType}")
            result.exception?.printStackTrace()
        } else println("Schedule downloaded successfully")
    },
    "load" to load@{ args ->
        val loadGtfs = "-gtfs" in args

        if (loadGtfs) {
            val file = File("schedule.zip")

            if (!file.isFile) {
                System.err.println("No schedule downloaded")
                return@load
            }

            println("Loading gtfs schedule...")
            schedule = GtfsScheduleLoader.load(file, EmptyRealtimeDispatcher, onLoadProgress = { })
            println("Gtfs schedule loaded successfully")

            return@load
        }

        val file = File("cached.zip")

        if (!file.isFile) {
            System.err.println("No schedule saved")
            return@load
        }

        println("Loading cached schedule...")
        schedule = CachedScheduleIO.load(file, EmptyRealtimeDispatcher)
        println("Schedule loaded successfully")
    },
    "save" to {
        val schedule = requireSchedule()

        println("Saving schedule...")
        CachedScheduleIO.save(schedule, File("cached.zip"))
        println("Schedule saved successfully")
    },
    "minimize" to {
        val schedule = requireSchedule()

        println("Minimizing schedule...")
        CachedScheduleIO.minimize(schedule, File("cached.zip"), EmptyRealtimeDispatcher)
        println("Schedule minimized successfully")
    },
    "feedInfo" to { println(requireSchedule().feedInfo) },
    "calendarDates" to { println(requireSchedule().calendarDates) },
    "serviceTypes" to { println(requireSchedule().serviceTypes) },
    "routes" to { println(requireSchedule().routes.values.joinToString(separator = "\n")) },
    "stops" to { args ->
        if (args.size == 1 && args[0] == "all")
            println(requireSchedule().stops.values.joinToString(separator = "\n"))
        else if (args.isEmpty())
            System.err.println("No stop specified (use all to print all stops)")
        else {
            val schedule = requireSchedule()
            for (stopId in args) {
                val stop = schedule.stops[stopId.toStopId()]
                if (stop != null)
                    println(stop)
                else
                    System.err.println("No such stop.")
            }
        }
    },
    "trips" to trips@{ args ->
        val schedule = requireSchedule()
        val routeId = args.lastOrNull()?.takeUnless { it.startsWith("-") }
            ?: run {
                System.err.println("No route specified")
                return@trips
            }

        val route = schedule.routes[routeId]
            ?: run {
                System.err.println("No such route")
                return@trips
            }

        println(route.trips.values.joinToString(separator = "\n"))
    },
    "lock" to { args ->
        FileInputStream(args[0]).channel.lock(0, Long.MAX_VALUE, true).use {
            println("Locked file. Press enter to unlock.")
            readln()
        }
    },
    "find" to find@{ args ->
        val query = args.getOrNull(0)

        if (query.isNullOrBlank()) {
            System.err.println("No query specified")
            println("Available queries: ${findQueries.keys.joinToString()}")
            return@find
        }

        val queryAction = findQueries[query]

        if (queryAction == null) {
            System.err.println("Unknown query")
            println("Available queries: ${findQueries.keys.joinToString()}")
            return@find
        }

        queryAction()
    },
)

private val findQueries: Map<String, () -> Unit> = mapOf(
    "same-location-stops" to {
        val searchedLocations = HashMap<FloatFloatPair, Stop>()

        for (stop in requireSchedule().stops.values) {
            if (stop.code == 0)
                continue

            val location = FloatFloatPair(stop.latitude, stop.longitude)
            val stopIdWithSameLocation = searchedLocations[location]

            if (stopIdWithSameLocation != null)
                println("${stop.name} (${stop.id}) and ${stopIdWithSameLocation.name} (${stopIdWithSameLocation.id})")
            else
                searchedLocations[location] = stop
        }
    },
    "routes-at-stop-both-directions" to {
        for (stop in requireSchedule().stops.values) {
            if (stop.id.stopCode == 0)
                continue

            for ((route, routeAtStop) in stop.routes)
                if (routeAtStop.stopsAtDirectionZero && routeAtStop.stopsAtDirectionOne)
                    println("Route ${route.id} at stop ${stop.name} (${stop.id})")
        }
    },
    "headsigns" to {
        val headsigns = LinkedHashMap<String, MutableSet<RouteId>>()

        for (route in requireSchedule().routes.values)
            for (trip in route.trips.values)
                headsigns.getOrPut(trip.headsign) { mutableSetOf() } += route.id

        for ((headsign, routes) in headsigns) {
            print(headsign)
            print(": ")
            println(routes.joinToString())
        }
    },
)
