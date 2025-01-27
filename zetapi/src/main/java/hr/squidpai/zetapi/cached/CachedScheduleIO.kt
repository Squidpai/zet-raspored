package hr.squidpai.zetapi.cached

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import hr.squidpai.zetapi.CalendarDates
import hr.squidpai.zetapi.FeedInfo
import hr.squidpai.zetapi.Schedule
import hr.squidpai.zetapi.ServiceTypes
import hr.squidpai.zetapi.Shape
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.realtime.RealtimeDispatcher
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalSerializationApi::class)
public object CachedScheduleIO {

   private const val FEED_INFO_FILE = "feed_info.csv"
   private const val ROUTES_FILE = "routes.csv"
   private const val STOPS_FILE = "stops.csv"
   private const val SHAPES_FILE = "shapes.csv"
   private const val CALENDAR_DATES_FILE = "calendar_dates.json"
   private const val SERVICE_TYPES = "service_types.json"

   public fun save(schedule: Schedule, file: File) {
      ZipOutputStream(file.outputStream().buffered()).use { zip ->
         val writer = CSVWriter(zip.writer())

         zip.putNextEntry(ZipEntry(FEED_INFO_FILE))
         schedule.feedInfo.save(writer)
         // The writer needs to be flushed before closing the entry.
         writer.flush()

         zip.putNextEntry(ZipEntry(ROUTES_FILE))
         for (route in schedule.routes.values)
            CachedRoute.save(route, writer)
         writer.flush()

         zip.putNextEntry(ZipEntry(STOPS_FILE))
         for (stop in schedule.stops.values)
            CachedStop.save(stop, writer)
         writer.flush()

         zip.putNextEntry(ZipEntry(SHAPES_FILE))
         for (shape in schedule.shapes.values)
            CachedShape.save(shape, writer)
         writer.flush()

         zip.putNextEntry(ZipEntry(CALENDAR_DATES_FILE))
         Json.encodeToStream(schedule.calendarDates, zip)

         zip.putNextEntry(ZipEntry(SERVICE_TYPES))
         Json.encodeToStream(schedule.serviceTypes, zip)

         for (route in schedule.routes.values) {
            zip.putNextEntry(ZipEntry(route.id))
            CachedRoute.saveTNS(route, writer)
            writer.flush()
         }

         zip.closeEntry()
      }
   }

   public fun minimize(
      schedule: Schedule,
      cachedScheduleFile: File,
      realtimeDispatcher: RealtimeDispatcher,
   ): Schedule = Schedule(
      feedInfo = schedule.feedInfo,
      routes = schedule.routes.mapValues { (_, route) ->
         CachedRoute(
            route.id,
            route.shortName,
            route.longName,
            route.type,
            route.sortOrder,
            route.commonHeadsigns,
            scheduleFile = cachedScheduleFile,
            shapes = schedule.shapes,
            realtimeDispatcher,
         ).apply { stops = schedule.stops }
      },
      stops = schedule.stops,
      shapes = schedule.shapes,
      calendarDates = schedule.calendarDates,
      serviceTypes = schedule.serviceTypes,
   )

   public fun load(file: File, realtimeDispatcher: RealtimeDispatcher): Schedule {
      ZipFile(file).use { zip ->
         @Suppress("JoinDeclarationAndAssignment")
         var input: InputStream

         val feedInfo = FeedInfo.fromZip(zip, FEED_INFO_FILE)

         input = zip.getInputStream(zip.getEntry(SHAPES_FILE))
         val shapeList = mutableListOf<Shape>()
         for (data in CSVReader(input.reader()))
            shapeList += CachedShape(data)
         val shapes = shapeList.associateBy { it.id }

         input = zip.getInputStream(zip.getEntry(ROUTES_FILE))
         val routeList = mutableListOf<CachedRoute>()
         for (data in CSVReader(input.reader()))
            routeList += CachedRoute(data, file, shapes, realtimeDispatcher)
         val routes = routeList.associateBy { it.id }

         input = zip.getInputStream(zip.getEntry(STOPS_FILE))
         val stopList = mutableListOf<Stop>()
         for (data in CSVReader(input.reader()))
            stopList += CachedStop(data, routes)
         val stops = Stops(stopList)

         for (route in routes.values)
            route.stops = stops

         val calendarDates = Json.decodeFromStream<CalendarDates>(
            zip.getInputStream(zip.getEntry(CALENDAR_DATES_FILE))
         )

         val serviceTypes = Json.decodeFromStream<ServiceTypes>(
            zip.getInputStream(zip.getEntry(SERVICE_TYPES))
         )

         return Schedule(
            feedInfo,
            routes,
            stops,
            shapes,
            calendarDates,
            serviceTypes,
         )
      }
   }

   public fun getFeedInfo(file: File): FeedInfo =
      ZipFile(file).use { FeedInfo.fromZip(it, FEED_INFO_FILE) }

   public fun getFeedInfoOrNull(file: File): FeedInfo? = try {
      getFeedInfo(file)
   } catch (_: Exception) {
      null
   }

}