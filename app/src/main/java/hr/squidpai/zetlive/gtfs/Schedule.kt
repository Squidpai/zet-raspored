package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.*
import hr.squidpai.zetlive.decrementInt
import hr.squidpai.zetlive.localEpochDate
import hr.squidpai.zetlive.nullState
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
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

    private fun previousVersionLink(currentVersion: String) =
      "https://www.zet.hr/gtfs-scheduled/scheduled-000-${currentVersion.decrementInt()}.zip"

    private val parentScope = CoroutineScope(Dispatchers.IO)

    open class LoadingState(val text: String)
    class TrackableLoadingState(text: String) : LoadingState(text) {
      var progress by mutableFloatStateOf(0f)
    }

    var priorityLoadingState by mutableStateOf<LoadingState?>(null)
    var loadingState by mutableStateOf<LoadingState?>(null)

    var instance by mutableStateOf<Schedule>(EmptySchedule)
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
      /*run {
      val files = scheduleDirectory.listFiles()?.filter { it.isFile }

      if (files == null) {
        scheduleDirectory.mkdir()
        return@run null
      }
      if (files.isEmpty()) {
        return@run null
      }
      if (files.size == 1) {
        return@run files[0]
      }

      // If there are multiple versions,
      // choose the newest one and delete the rest.
      files.maxBy { it.lastModified() }.also { for (f in files) if (f !== it) f.delete() }
    }*/

      if (originalZipFile.isFile && instance == EmptySchedule) {
        instance = ScheduleImpl(parentScope, originalZipFile, stopTimesDirectory)
      }

      update(filesDir)
    }

    private fun download(link: String, file: File): Boolean {
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

    fun update(filesDir: File, link: String = LINK) {
      parentScope.launch {
        val originalSchedule = instance as? ScheduleImpl
        val newZipFile = File(filesDir, NEW_SCHEDULE_NAME)
        val newStopTimes = File(filesDir, NEW_STOP_TIMES)
        var newFeedInfo: FeedInfo? = null

        run download@{
          val url = try {
            URL(link).also { Log.d(TAG, "URL initialized successfully.") }
          } catch (e: MalformedURLException) {
            Log.w(TAG, "Malformed URL.", e)
            return@download
          }

          val connection = try {
            url.openConnection().also {
              Log.d(TAG, "Connection opened successfully.")
              it.connect()
              Log.d(TAG, "Connected successfully.")
            }
          } catch (e: IOException) {
            Log.w(TAG, "IOException occurred while opening connection.", e)
            return@download
          }

          val fieldValue = connection.getHeaderField("Content-Disposition")
            ?: run { // no content, do not download anything
              Log.d(TAG, "No content, not downloading.")
              return@download
            }

          Log.d(TAG, "fieldValue exists: $fieldValue")

          val index = fieldValue.indexOf("filename=")
          if (index == -1) {
            Log.d(TAG, "No filename")
            return@download // no file name, do not download
          }
          val newVersion = fieldValue.substring(index + 9).replace("\"", "").feedVersion

          Log.d(TAG, "Found version: $newVersion")

          val cachedVersion = originalSchedule?.feedInfo?.version

          if (newVersion != cachedVersion) {

            if (newZipFile.isFile)
              newFeedInfo = newZipFile.feedInfo

            // do not download if the new schedule is already up-to-date.
            if (newZipFile.isFile && newVersion == newFeedInfo?.version)
              return@download

            var success = false
            try {
              Log.d(TAG, "New version exists, downloading it${Typography.ellipsis}")

              connection.inputStream.use { inputStream ->
                FileOutputStream(newZipFile).use { output ->
                  output.channel.transferFrom(Channels.newChannel(inputStream), 0L, Long.MAX_VALUE)
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
              //
              newStopTimes.deleteRecursively()
            }
          } else Log.d(TAG, "Schedule already up-to-date.")
        }

        if (!newZipFile.isFile) {
          Log.d(TAG, "No new schedule found.")
          return@launch
        }
        Log.d(TAG, "Found new schedule.")

        if (newFeedInfo == null)
          newFeedInfo = newZipFile.feedInfo

        val startsIn = newFeedInfo!!.startDate.toEpochDay() - localEpochDate()

        // new schedule started
        if (startsIn <= 0L) {
          Log.d(TAG, "New schedule started.")
          // original schedule doesn't exist, load the new one immediately
          if (originalSchedule == null) {
            Log.d(TAG, "No original schedule found, loading the new one immediately.")
            replaceSchedule(filesDir, newZipFile, newStopTimes)
            return@launch
          }
          Log.d(TAG, "Found original schedule.")

          // new schedule is completely loaded, replace the old one with it immediately
          if (TripsLoader.tripsLoaded(newStopTimes)) {
            Log.d(TAG, "New schedule loaded, replacing the old one with it.")
            replaceSchedule(filesDir, newZipFile, newStopTimes)
            return@launch
          }

          Log.d(TAG, "Loading new schedule...")
          // new schedule isn't loaded. load it and then replace the old one with it
          val builder = ScheduleBuilder(newZipFile, newStopTimes)
          replaceSchedule(filesDir, newZipFile, newStopTimes, builder)
          return@launch
        }
        Log.d(TAG, "New schedule hasn't started yet.")

        // Big Problem: we have only one schedule, and it hasn't started yet!
        // Download the previous version and set it as the old one;
        // surely it's started... right? (if not, give up, ZET je glup ko kurac)
        if (originalSchedule == null) {
          Log.w(TAG, "bruh")
          val originalZipFile = File(filesDir, SCHEDULE_NAME)
          if (download(previousVersionLink(newFeedInfo!!.version), originalZipFile))
            instance = ScheduleImpl(parentScope, originalZipFile, File(filesDir, STOP_TIMES))
          return@launch
        }

        // new schedule hasn't started yet, load it behind the scenes
        if (!TripsLoader.tripsLoaded(newStopTimes)) {
          Log.d(TAG, "Loading new schedule behind the scenes.")
          TripsLoader.loadTrips(newZipFile, newStopTimes, TripsLoader.PriorityLevel.Hidden)
        }
      }
    }

    private fun replaceSchedule(filesDir: File, newZipFile: File, newStopTimes: File, builder: ScheduleBuilder? = null) {
      val originalZipFile = File(filesDir, SCHEDULE_NAME)
      val originalStopTimes = File(filesDir, STOP_TIMES)
      originalZipFile.delete()
      originalStopTimes.deleteRecursively()
      newZipFile.renameTo(originalZipFile)
      newStopTimes.renameTo(originalStopTimes)
      instance = ScheduleImpl(parentScope, originalZipFile, originalStopTimes, builder)
    }
  }

}

private class ScheduleBuilder(zipFile: File, stopTimesDirectory: File) {
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

private class ScheduleImpl(
  parentScope: CoroutineScope,
  val zipFile: File,
  val stopTimesDirectory: File,
  scheduleBuilder: ScheduleBuilder? = null,
) : Schedule {

  private val scope = parentScope + Job()

  init {
    scope.launch {
      ZipFile(zipFile).use { zip ->
        feedInfo = zip.feedInfo
        routes = scheduleBuilder?.routes ?: Routes(zip)
        stops = Stops(zip)
        calendarDates = CalendarDates(zip)
      }
      if (scheduleBuilder == null) {
        // stop times and routesAtStopMap are
        // already loaded with scheduleBuilder

        routesAtStopMap = TripsLoader.getRoutesAtStopMap(stopTimesDirectory)
        if (!TripsLoader.tripsLoaded(stopTimesDirectory))
          loadStopTimesAsync(TripsLoader.PriorityLevel.Foreground)
      }
    }
  }

  fun cancel() = scope.cancel()

  override var feedInfo by mutableStateOf<FeedInfo?>(null)

  override var routes by mutableStateOf<Routes?>(null)

  override var stops by mutableStateOf<Stops?>(null)

  override var calendarDates by mutableStateOf<CalendarDates?>(null)

  override var routesAtStopMap by mutableStateOf<RoutesAtStopMap?>(null)

  private var _serviceIdTypes by mutableStateOf<ServiceIdTypes?>(null)

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

  private val fieldLock = Any()

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
          (state as MutableState).value = TripsLoader.getTripsOfRoute(stopTimesDirectory, routeId)
        }

        routesAtStopMap = TripsLoader.getRoutesAtStopMap(stopTimesDirectory)

        /*val routes = routes ?: return@launch
        val stops = stops ?: return@launch
        val stopsByRoute = stopsByRoute ?: return@launch
        val needsToCalculateAllStopTypes = synchronized(fieldLock) { needsToCalculatedAllStopTypes }
        if (needsToCalculateAllStopTypes) stops.calculateAndGroupAllStopTypes(stopsByRoute, routes)*/
      } finally {
        loadingStopTimes = false
      }
    }
  }

}

/**
 * A schedule that doesn't have any data associated with it.
 *
 * Used as a default before the actual schedule is [initialized][Schedule.init].
 */
private data object EmptySchedule : Schedule {

  override val feedInfo = null

  override val routes = null

  override val stops = null

  override val calendarDates = null

  override val serviceIdTypes = null

  override fun getTripsOfRoute(routeId: RouteId) = nullState<Trips>()

  override val routesAtStopMap = null

}