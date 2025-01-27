package hr.squidpai.zetapi.cached

import hr.squidpai.zetapi.Schedule
import hr.squidpai.zetapi.gtfs.GtfsScheduleLoader
import hr.squidpai.zetapi.realtime.EmptyRealtimeDispatcher
import java.io.File

private fun main() {
   val file = File("schedule.zip")
   if (file.isFile)
      println("Schedule already downloaded")
   else {
      println("Downloading schedule...")
      val result = GtfsScheduleLoader.download(file)
      if (result.errorType != null) {
         System.err.println("Error while downloading schedule: ${result.errorType}")
         result.exception?.printStackTrace()
         return
      } else println("Schedule downloaded successfully")
   }
   val cachedFile = File("cached.zip")
   var schedule: Schedule?
   if (cachedFile.isFile)
      println("Schedule already cached")
   else {
      println("Loading schedule...")
      schedule = GtfsScheduleLoader.load(file, EmptyRealtimeDispatcher)
      println("Schedule loaded successfully")
      println("Saving schedule...")
      CachedScheduleIO.save(schedule, cachedFile)
      schedule = null // unload old schedule
   }
   println("Loading cached schedule...")
   schedule = CachedScheduleIO.load(cachedFile, EmptyRealtimeDispatcher)
   println("Routes: ")
   println(schedule.feedInfo)
   println(schedule.routes)
   println(schedule.stops.groupedStops)
   //println(schedule.shapes)
   println(schedule.calendarDates)
   println(schedule.serviceTypes)
}