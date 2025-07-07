package hr.squidpai.zetapi.cached

import hr.squidpai.zetapi.Schedule
import hr.squidpai.zetapi.gtfs.GtfsScheduleLoader
import hr.squidpai.zetapi.realtime.EmptyRealtimeDispatcher
import java.io.File
import java.io.FileInputStream
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
   }
}

private var schedule: Schedule? = null

private object NoScheduleException : Throwable() {
   private fun readResolve(): Any = NoScheduleException
}

private fun requireSchedule() = schedule ?: throw NoScheduleException

private val commands = mapOf<String, (List<String>) -> Unit>(
   "exit" to { args -> exitProcess(args.getOrNull(0)?.toIntOrNull() ?: 0) },
   "download" to { args ->
      val version = args.lastOrNull()?.takeUnless { it.startsWith("-") }

      val file = File("schedule.zip")
      println("Downloading schedule...")
      val result = GtfsScheduleLoader.download(
         file,
         link = version?.let { GtfsScheduleLoader.versionLink(it) }
            ?: GtfsScheduleLoader.LINK,
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
         schedule = GtfsScheduleLoader.load(file, EmptyRealtimeDispatcher) { }
         println("Gtfs Schedule loaded successfully")

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
   "routes" to { requireSchedule().routes.values.joinToString(separator = "\n") },
   "stops" to { requireSchedule().stops.values.joinToString(separator = "\n") },
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
   }
)
