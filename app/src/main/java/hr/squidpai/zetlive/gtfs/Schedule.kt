package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hr.squidpai.zetlive.decrementInt
import hr.squidpai.zetlive.localEpochDate
import hr.squidpai.zetlive.nullState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
import java.util.zip.ZipException
import java.util.zip.ZipFile

sealed interface Schedule {

   val feedInfo: FeedInfo?

   val routes: Routes?

   val stops: Stops?

   val calendarDates: CalendarDates?

   fun getTripsOfRoute(routeId: RouteId): State<Trips?>

   val routesAtStopMap: RoutesAtStopMap?

   val serviceIdTypes: ServiceIdTypes?

   companion object {
      private const val TAG = "ScheduleInit"

      private const val SCHEDULE_NAME = "schedule.zip"
      private const val NEW_SCHEDULE_NAME = "new.zip"
      private const val STOP_TIMES = "stopTimes"
      private const val NEW_STOP_TIMES = "newStopTimes"

      private const val LINK = "https://www.zet.hr/gtfs-scheduled/latest"

      private fun versionLink(version: String) =
         "https://www.zet.hr/gtfs-scheduled/scheduled-000-$version.zip"

      private val parentScope = CoroutineScope(Dispatchers.IO)

      open class LoadingState(val text: String)
      class TrackableLoadingState(text: String) : LoadingState(text) {
         var progress by mutableFloatStateOf(0f)
      }

      var priorityLoadingState by mutableStateOf<LoadingState?>(null)
      var loadingState by mutableStateOf<LoadingState?>(null)

      var instance by mutableStateOf<Schedule>(EmptySchedule())
         private set

      var lastCheckedLatestVersion by mutableStateOf<String?>(null)
         private set

      /**
       * Initializes the GTFS schedule:
       *
       * 1. See if a schedule in [filesDir] already exists.
       *
       * 2. If it exists, load from it to have data immediately.
       *
       * 3. Check if a new version is available (by comparing the name of
       *    the downloaded schedule) and download it.
       *
       * 4. If there is a new version available or stop times aren't loaded,
       *    start loading stop times immediately.
       */
      fun init(filesDir: File) {
         val originalZipFile = File(filesDir, SCHEDULE_NAME)
         val stopTimesDirectory = File(filesDir, STOP_TIMES)
         val oldInstance = instance

         if (oldInstance is EmptySchedule) {
            if (originalZipFile.isFile)
               try {
                  instance = LoadedSchedule(parentScope, originalZipFile, stopTimesDirectory)
               } catch (e: Exception) {
                  Log.e(TAG, "init: an error occurred while loading the schedule", e)
               }

            if (oldInstance.errorType != null)
               instance = EmptySchedule()
         }

         parentScope.launch {
            val errorType = update(filesDir).await()
            if (instance !is LoadedSchedule)
               instance = EmptySchedule(errorType)
         }
      }

      fun getNewScheduleFile(filesDir: File) = File(filesDir, NEW_SCHEDULE_NAME)

      @Volatile
      private var downloading = false

      private fun downloadRecursively(link: String, file: File): Boolean {
         Log.d(TAG, "Downloading from: $link")
         val url = try {
            URL(link).also { Log.d(TAG, "URL initialized successfully.") }
         } catch (e: MalformedURLException) {
            Log.w(TAG, "Malformed URL.", e)
            return false
         }

         val connection = try {
            url.openConnection().also {
               Log.d(TAG, "Connection opened successfully.")
               it.connect()
               Log.d(TAG, "Connected successfully.")
            }
         } catch (e: IOException) {
            Log.w(TAG, "IOException occurred while opening connection.", e)
            return false
         }

         try {
            Log.d(TAG, "Downloading${Typography.ellipsis}")

            connection.inputStream.use { inputStream ->
               FileOutputStream(file).use { output ->
                  output.channel.transferFrom(Channels.newChannel(inputStream), 0L, Long.MAX_VALUE)
               }
            }
            Log.d(TAG, "Finished download.")
            return true
         } catch (e: IOException) {
            Log.w(TAG, "load: IOException occurred while downloading", e)
            return false
         }
      }

      private fun download(
         link: String,
         cachedVersion: String?,
         newZipFile: File,
         newStopTimes: File,
      ): Pair<FeedInfo?, ErrorType?> {
         val url = try {
            URL(link).also { Log.d(TAG, "URL initialized successfully.") }
         } catch (e: MalformedURLException) {
            Log.w(TAG, "Malformed URL.", e)
            return null to ErrorType.MALFORMED_URL
         }

         val connection = try {
            url.openConnection().also {
               Log.d(TAG, "Connection opened successfully.")
               it.connect()
               Log.d(TAG, "Connected successfully.")
            }
         } catch (e: IOException) {
            Log.w(TAG, "IOException occurred while opening connection.", e)
            return null to ErrorType.OPENING_CONNECTION
         }

         val fieldValue = connection.getHeaderField("Content-Disposition")
            ?: run { // no content, do not download anything
               Log.d(TAG, "No content, not downloading.")
               return null to ErrorType.NO_CONTENT
            }

         Log.d(TAG, "fieldValue exists: $fieldValue")

         val index = fieldValue.indexOf("filename=")
         if (index == -1) {
            Log.d(TAG, "No filename")
            // no file name, do not download
            return null to ErrorType.NO_FILENAME
         }
         val newVersion = fieldValue.substring(index + 9).replace("\"", "").feedVersion
         lastCheckedLatestVersion = newVersion
         Log.d(TAG, "Found version: $newVersion")

         var newFeedInfo: FeedInfo? = null

         if (newVersion != cachedVersion) {

            if (newZipFile.isFile) {
               try {
                  newFeedInfo = newZipFile.feedInfo

                  // do not download if the new schedule is already up-to-date.
                  if (newVersion == newFeedInfo.version)
                     return newFeedInfo to ErrorType.UP_TO_DATE
               } catch (_: IOException) {
                  // new schedule is invalid
               }
            }

            var success = false
            try {
               Log.d(TAG, "New version exists, downloading it${Typography.ellipsis}")

               connection.inputStream.use { inputStream ->
                  FileOutputStream(newZipFile).use { output ->
                     output.channel.transferFrom(
                        Channels.newChannel(inputStream),
                        0L,
                        Long.MAX_VALUE
                     )
                  }
               }
               Log.d(TAG, "Finished download.")
               success = true
            } catch (e: IOException) {
               Log.w(TAG, "load: IOException occurred while downloading", e)
               // failed to download the new version, keep the old one (and delete the new one if it exists)
               newZipFile.delete()
            }

            if (success) {
               newFeedInfo = null
               // delete stop times of the new version, as we've found an even newer version
               newStopTimes.deleteRecursively()
            }
         } else {
            Log.d(TAG, "Schedule already up-to-date.")
            return null to ErrorType.UP_TO_DATE
         }

         return newFeedInfo to null
      }

      fun update(filesDir: File, link: String = LINK): Deferred<ErrorType?> {
         if (downloading)
            return parentScope.async { ErrorType.ALREADY_DOWNLOADING }
         downloading = true

         return parentScope.async {
            val errorType: ErrorType?
            run downloading@{
               val originalSchedule = instance as? LoadedSchedule
               val newZipFile = getNewScheduleFile(filesDir)
               val newStopTimes = File(filesDir, NEW_STOP_TIMES)

               val downloadResult =
                  download(link, originalSchedule?.feedInfo?.version, newZipFile, newStopTimes)
               var newFeedInfo = downloadResult.first
               errorType = downloadResult.second

               if (!newZipFile.isFile) {
                  Log.d(TAG, "No new schedule found.")
                  return@downloading
               }
               Log.d(TAG, "Found new schedule.")

               if (newFeedInfo == null)
                  try {
                     newFeedInfo = newZipFile.feedInfo
                  } catch (e: IOException) {
                     Log.d(TAG, "Zip file invalid: $e")
                     return@downloading
                  }

               val startsIn = newFeedInfo.startDate.toEpochDay() - localEpochDate()

               // new schedule started
               if (startsIn <= 0L) {
                  Log.d(TAG, "New schedule started.")
                  // original schedule doesn't exist, load the new one immediately
                  if (originalSchedule == null) {
                     Log.d(TAG, "No original schedule found, loading the new one immediately.")
                     replaceSchedule(filesDir, newZipFile, newStopTimes)
                     return@downloading
                  }
                  Log.d(TAG, "Found original schedule.")

                  // new schedule is completely loaded, replace the old one with it immediately
                  if (TripsLoader.tripsLoaded(newStopTimes)) {
                     Log.d(TAG, "New schedule loaded, replacing the old one with it.")
                     replaceSchedule(filesDir, newZipFile, newStopTimes)
                     return@downloading
                  }

                  Log.d(TAG, "Loading new schedule...")
                  // new schedule isn't loaded. load it and then replace the old one with it
                  val builder = ScheduleBuilder(newZipFile, newStopTimes)
                  replaceSchedule(filesDir, newZipFile, newStopTimes, builder)
                  return@downloading
               }
               Log.d(TAG, "New schedule hasn't started yet.")

               // Big Problem: we have only one schedule, and it hasn't started yet!
               // Download the previous version and set it as the old one;
               // surely it's started... right?
               // (if not, try this recursively 5 times)
               if (originalSchedule == null) {
                  Log.w(TAG, "bruh")
                  val originalZipFile = File(filesDir, SCHEDULE_NAME)
                  var previousVersion = newFeedInfo.version
                  repeat(5) {
                     previousVersion = previousVersion.decrementInt()
                     if (downloadRecursively(versionLink(previousVersion), originalZipFile)) {
                        try {
                           instance = LoadedSchedule(
                              parentScope,
                              originalZipFile,
                              File(filesDir, STOP_TIMES),
                           )
                           return@downloading
                        } catch (e: Exception) {
                           Log.e(
                              TAG,
                              "Exception thrown while initializing recursively downloaded schedule",
                              e
                           )
                        }
                     }
                  }
                  Log.d(
                     TAG,
                     "There are no schedules that have started yet, loading the latest one."
                  )
                  replaceSchedule(filesDir, newZipFile, newStopTimes)
                  return@downloading
               }

               // new schedule hasn't started yet, load it behind the scenes
               if (!TripsLoader.tripsLoaded(newStopTimes)) {
                  Log.d(TAG, "Loading new schedule behind the scenes.")
                  TripsLoader.loadTrips(newZipFile, newStopTimes, TripsLoader.PriorityLevel.Hidden)
               }

            }
            downloading = false
            return@async errorType
         }
      }

      private fun replaceSchedule(
         filesDir: File,
         newZipFile: File,
         newStopTimes: File,
         builder: ScheduleBuilder? = null
      ) {
         val originalZipFile = File(filesDir, SCHEDULE_NAME)
         val originalStopTimes = File(filesDir, STOP_TIMES)
         originalZipFile.delete()
         originalStopTimes.deleteRecursively()
         newZipFile.renameTo(originalZipFile)
         newStopTimes.renameTo(originalStopTimes)
         val oldSchedule = instance as? LoadedSchedule
         try {
            instance = LoadedSchedule(parentScope, originalZipFile, originalStopTimes, builder)
            oldSchedule?.cancel()
         } catch (e: Exception) {
            Log.e(TAG, "replaceSchedule: failed to init schedule", e)
         }
      }
   }

   enum class ErrorType {
      MALFORMED_URL, OPENING_CONNECTION, NO_CONTENT, NO_FILENAME, UP_TO_DATE, ALREADY_DOWNLOADING,
      DOWNLOAD_ERROR;

      val errorMessage
         get() = when (this) {
            ALREADY_DOWNLOADING -> null
            MALFORMED_URL ->
               "Nije moguće spojiti se na ZET-ovu stranicu. Provjerite svoju internet konekciju."

            OPENING_CONNECTION -> "Dogodila se greška prilikom spajanja na ZET-ovu stranicu. " +
                  "Provjerite svoju internet konekciju."

            NO_CONTENT, NO_FILENAME -> "Nije moguće preuzeti raspored sa ZET-ove stranice."
            UP_TO_DATE -> "Već je preuzet najnoviji raspored."
            DOWNLOAD_ERROR -> "Dogodila se greška prilikom preuzimanja novog rasporeda. " +
                  "Provjerite svoju internet konekciju."
         }
   }

}

class ScheduleBuilder(zipFile: File, stopTimesDirectory: File) {
   val routes: Routes
   val routesAtStopMap: RoutesAtStopMap?

   init {
      ZipFile(zipFile).use { zip ->
         routes = Routes(zip)
      }
      TripsLoader.loadTrips(zipFile, stopTimesDirectory, TripsLoader.PriorityLevel.Background)
      routesAtStopMap = TripsLoader.getRoutesAtStopMap(stopTimesDirectory)
   }
}

/** The actual schedule implementation containing all schedule data. */
class LoadedSchedule(
   parentScope: CoroutineScope,
   val zipFile: File,
   val stopTimesDirectory: File,
   scheduleBuilder: ScheduleBuilder? = null,
) : Schedule {

   private val scope = parentScope + Job()

   override var feedInfo by mutableStateOf<FeedInfo?>(null)

   override var routes by mutableStateOf(Routes.empty)

   override var stops by mutableStateOf(Stops.empty)

   override var calendarDates by mutableStateOf<CalendarDates?>(null)

   override var routesAtStopMap by mutableStateOf<RoutesAtStopMap?>(null)

   private var _serviceIdTypes by mutableStateOf<ServiceIdTypes?>(null)

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

            routesAtStopMap = TripsLoader.getRoutesAtStopMap(stopTimesDirectory)
            if (!TripsLoader.tripsLoaded(stopTimesDirectory))
               loadStopTimesAsync(TripsLoader.PriorityLevel.Foreground)
         } else {
            routesAtStopMap = scheduleBuilder.routesAtStopMap
         }
      }
   }

   fun cancel() = scope.cancel()

   override val serviceIdTypes: ServiceIdTypes?
      get() {
         val serviceIdTypes = _serviceIdTypes

         if (serviceIdTypes != null)
            return serviceIdTypes

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
   override fun getTripsOfRoute(routeId: RouteId) =
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
         } finally {
            loadingStopTimes = false
         }
      }
   }

}

/**
 * A default schedule used in place of the actual schedule.
 * Displayed before the actual schedule is loaded (in which case [errorType] is `null`) or
 * if an error occurs while loading the schedule.
 */
data class EmptySchedule(val errorType: Schedule.ErrorType? = null) : Schedule {

   override val feedInfo = null

   override val routes = null

   override val stops = null

   override val calendarDates = null

   override val serviceIdTypes = null

   override fun getTripsOfRoute(routeId: RouteId) = nullState<Trips>()

   override val routesAtStopMap = null

}
