package hr.squidpai.zetlive.gtfs

import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipFile

/** The actual schedule implementation containing all schedule data. */
class LoadedSchedule(
   parentScope: CoroutineScope,
   val zipFile: File,
   val stopTimesDirectory: File,
   scheduleBuilder: ScheduleBuilder? = null,
) : Schedule {

   private val scope = parentScope + Job()

   val feedInfo: FeedInfo

   val routes: Routes

   val stops: Stops

   val calendarDates: CalendarDates

   var routesAtStopMap by mutableStateOf<RoutesAtStopMap?>(null)

   private val fieldLock = Any()

   init {
      try {
         ZipFile(zipFile).use { zip ->
            feedInfo = zip.feedInfo
            routes = scheduleBuilder?.routes ?: Routes(zip)
            stops = Stops(zip)
            calendarDates = CalendarDates(zip)
         }
      } catch (e: ZipException) {
         // zip file is probably corrupted, delete it
         zipFile.delete()
         stopTimesDirectory.deleteRecursively()
         throw e
      }
      scope.launch {
         if (scheduleBuilder == null) {
            // stop times and routesAtStopMap are
            // already loaded with scheduleBuilder

            if (TripsLoader.tripsLoaded(stopTimesDirectory))
               routesAtStopMap = TripsLoader.getRoutesAtStopMap(stopTimesDirectory)
            else
               loadStopTimesAsync(TripsLoader.PriorityLevel.Foreground)
         } else {
            routesAtStopMap = scheduleBuilder.routesAtStopMap
         }
      }
   }

   fun cancel() = scope.cancel()

   private var _serviceIdTypes: ServiceIdTypes? = null

   val serviceIdTypes: ServiceIdTypes?
      get() {
         if (_serviceIdTypes != null)
            return _serviceIdTypes

         val loaded = TripsLoader.loadServiceIdTypes(stopTimesDirectory)

         if (loaded != null)
            return loaded.also { _serviceIdTypes = it }

         val given = Love.giveMeTheServiceIdTypes(this)

         if (given != null)
            return given.also {
               _serviceIdTypes = it
               TripsLoader.saveServiceIdTypes(stopTimesDirectory, it)
            }

         return null
      }

   private val tripsCache = MutableIntObjectMap<State<Trips?>>()

   @Synchronized
   fun getTripsOfRoute(routeId: RouteId) =
      tripsCache.getOrPut(routeId) {
         val stopTimes = TripsLoader.getTripsOfRoute(stopTimesDirectory, routeId)

         if (stopTimes == null)
            loadStopTimesAsync(TripsLoader.PriorityLevel.Hidden)

         mutableStateOf(stopTimes)
      }

   private var loadingStopTimes = false

   private fun loadStopTimesAsync(priorityLevel: TripsLoader.PriorityLevel) {
      synchronized(fieldLock) {
         if (loadingStopTimes) return
         loadingStopTimes = true
      }

      scope.launch {
         try {
            TripsLoader.loadTrips(zipFile, stopTimesDirectory, priorityLevel)

            tripsCache.forEach { routeId, state ->
               (state as MutableState).value =
                  TripsLoader.getTripsOfRoute(stopTimesDirectory, routeId)
            }

            routesAtStopMap = TripsLoader.getRoutesAtStopMap(stopTimesDirectory)
         } catch (_: IOException) {
         } finally {
            loadingStopTimes = false
         }
      }
   }
}
