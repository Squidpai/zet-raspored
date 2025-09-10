package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hr.squidpai.zetapi.Schedule
import hr.squidpai.zetapi.cached.CachedScheduleIO
import hr.squidpai.zetapi.gtfs.ErrorType
import hr.squidpai.zetapi.gtfs.GtfsScheduleLoader
import hr.squidpai.zetapi.realtime.SingleThreadRealtimeDispatcher
import hr.squidpai.zetlive.localEpochDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

object ScheduleManager {

    private const val TAG = "ScheduleManager"

    private val _instance = MutableStateFlow<Schedule?>(null)
    val instance = _instance.asStateFlow()

    private val _lastDownloadError = MutableStateFlow<ErrorType?>(null)
    val lastDownloadError = _lastDownloadError.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState?>(null)
    val downloadState = _downloadState.asStateFlow()

    private val onDownloadProgress = { progress: Float ->
        _downloadState.value = DownloadState(DownloadOperation.DOWNLOADING, progress)
    }

    private val onTripLoadProgress = { progress: Float ->
        _downloadState.value = DownloadState(DownloadOperation.LOADING_GTFS, progress)
    }

    data class DownloadState(val operation: DownloadOperation, val progress: Float = Float.NaN)

    enum class DownloadOperation {
        DOWNLOADING,
        LOADING_CACHED,
        LOADING_GTFS;
    }

    val realtimeDataState = MutableStateFlow(RealtimeDataState.UNINITIALIZED)

    enum class RealtimeDataState {
        UNINITIALIZED,
        INITIALIZED,
        DOWNLOADING,
        ERROR;

        val displayRealtimeDataNotLive
            get() = this == DOWNLOADING || this == ERROR

        companion object {
            fun fromDownloadResult(success: Boolean) =
                if (success) INITIALIZED else ERROR
        }
    }

    val realtimeDispatcher = SingleThreadRealtimeDispatcher(
        onDownloadResult = {
            realtimeDataState.value = RealtimeDataState.fromDownloadResult(success = it == null)
        }
    )

    private const val LEGACY_STOP_TIMES = "stopTimes"
    private const val LEGACY_NEW_STOP_TIMES = "newStopTimes"

    private const val SCHEDULE_NAME = "schedule.zip"
    private const val NEW_SCHEDULE_NAME = "new.zip"
    private const val DOWNLOADED_SCHEDULE = "gtfs.zip"
    private const val NEW_DOWNLOADED_SCHEDULE = "gtfs_new.zip"

    private val parentScope = CoroutineScope(Dispatchers.IO)

    var lastCheckedLatestVersion by mutableStateOf<String?>(null)
        private set

    /** 0 = not initialized, 1 = initializing, 2 = initialized */
    private var initState = 0

    /**
     * Initializes the GTFS schedule:
     *
     * 1. See if a schedule in [filesDir] already exists.
     *
     * 2. If it exists, load from it to have data immediately.
     *
     * 3. Check if a new version is available.
     *
     * 4. If there is a new version available or stop times aren't loaded,
     *    start loading stop times immediately.
     */
    fun init(filesDir: File) {
        synchronized(this) {
            if (initState != 0)
                return
            initState = 1
        }
        Log.d(TAG, "init: initializing")

        removeLegacyStopTimesDirectory(filesDir)

        val scheduleFile = File(filesDir, SCHEDULE_NAME)

        try {
            _downloadState.value = DownloadState(DownloadOperation.LOADING_CACHED)
            _instance.value = CachedScheduleIO.load(scheduleFile, realtimeDispatcher)
            _downloadState.value = null
            initState = 2
            parentScope.launch { update0(filesDir) }
        } catch (e: Exception) {
            if (e is FileNotFoundException)
                Log.i(TAG, "init: no cached schedule found")
            else
                Log.e(TAG, "init: an error occurred while loading the schedule", e)
            parentScope.launch {
                initCachedSchedule(filesDir)
                if (initState == 2)
                    update0(filesDir)
                else initState = 0
            }
        }
    }

    private fun removeLegacyStopTimesDirectory(filesDir: File) {
        val stopTimes = File(filesDir, LEGACY_STOP_TIMES)
        val stopTimesIsDirectory = stopTimes.isDirectory
        if (stopTimesIsDirectory)
            stopTimes.deleteRecursively()

        val newStopTimes = File(filesDir, LEGACY_NEW_STOP_TIMES)
        val newStopTimesIsDirectory = newStopTimes.isDirectory
        if (newStopTimesIsDirectory)
            newStopTimes.deleteRecursively()

        if (stopTimesIsDirectory || newStopTimesIsDirectory) {
            val scheduleFile = File(filesDir, SCHEDULE_NAME)
            if (scheduleFile.isFile)
                scheduleFile.renameTo(File(filesDir, DOWNLOADED_SCHEDULE))

            val newScheduleFile = File(filesDir, NEW_SCHEDULE_NAME)
            if (newScheduleFile.isFile)
                newScheduleFile.renameTo(File(filesDir, NEW_DOWNLOADED_SCHEDULE))
        }
    }

    fun update(filesDir: File): Job {
        synchronized(this) {
            if (initState == 1)
                return Job().also { it.complete() }
            initState = 1
        }
        return parentScope.launch { update0(filesDir) }
    }

    private fun initCachedSchedule(filesDir: File) {
        val today = localEpochDate()
        val scheduleFile = File(filesDir, SCHEDULE_NAME)
        val newScheduleFile = File(filesDir, NEW_SCHEDULE_NAME)
        val downloadFile = File(filesDir, DOWNLOADED_SCHEDULE)
        val newDownloadFile = File(filesDir, NEW_DOWNLOADED_SCHEDULE)

        if (CachedScheduleIO.getFeedInfoOrNull(newScheduleFile)?.started(today) == true) {
            newScheduleFile.renameTo(scheduleFile)
            try {
                _downloadState.value = DownloadState(DownloadOperation.LOADING_CACHED)
                _instance.value = CachedScheduleIO.load(scheduleFile, realtimeDispatcher)
                _downloadState.value = null
                initState = 2
                return
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "initCachedSchedule: an error occurred while loading the new schedule",
                    e
                )
            }
        }
        if (GtfsScheduleLoader.getFeedInfoOrNull(newDownloadFile)?.started(today) == true)
            newDownloadFile.renameTo(downloadFile)

        if (!downloadFile.isFile) {
            _downloadState.value = DownloadState(DownloadOperation.DOWNLOADING)
            val result =
                GtfsScheduleLoader.download(downloadFile, onDownloadProgress = onDownloadProgress)
            _lastDownloadError.value = result.errorType
            result.exception?.let { e ->
                Log.e(TAG, "init: error downloading schedule", e)
            }
            if (result.errorType != null) {
                loadEarlierSchedule(
                    today,
                    downloadFile,
                    scheduleFile,
                    result.version
                )
                return
            }

            val started = GtfsScheduleLoader.getFeedInfoOrNull(downloadFile)?.started(today)
            if (started != true) {
                if (started == false)
                    downloadFile.renameTo(newDownloadFile)
                loadEarlierSchedule(
                    today,
                    downloadFile,
                    scheduleFile,
                    result.version
                )
                return
            }
        }

        if (downloadFile.isFile)
            initDownloadFile(downloadFile, scheduleFile)
    }

    private fun loadEarlierSchedule(
        today: Long,
        downloadFile: File,
        scheduleFile: File,
        newestVersion: String?,
    ) {
        var previousVersion = newestVersion ?: return

        operator fun String.dec(): String {
            val buff = toCharArray()
            for (i in buff.indices.reversed()) {
                if (buff[i] != '0') {
                    buff[i]--
                    break
                }
                buff[i] = '9'
            }
            return String(buff)
        }

        repeat(5) {
            _downloadState.value = DownloadState(DownloadOperation.DOWNLOADING)
            val result = GtfsScheduleLoader.download(
                downloadFile, link = GtfsScheduleLoader.versionLink(--previousVersion),
                onDownloadProgress = onDownloadProgress,
            )
            _lastDownloadError.value = result.errorType
            result.exception?.let { e ->
                Log.e(TAG, "init: error downloading schedule", e)
            }
            if (result.errorType != null)
                return@repeat // continue loop

            val started = GtfsScheduleLoader.getFeedInfoOrNull(downloadFile)?.started(today)
            if (started != true)
                return@repeat // continue loop

            if (initDownloadFile(downloadFile, scheduleFile))
                return // finally found a working schedule, break loop
        }
    }

    private fun initDownloadFile(downloadFile: File, scheduleFile: File) =
        try {
            _downloadState.value = DownloadState(DownloadOperation.LOADING_GTFS)
            val gtfsSchedule =
                GtfsScheduleLoader.load(downloadFile, realtimeDispatcher, onTripLoadProgress)
            CachedScheduleIO.save(gtfsSchedule, scheduleFile)
            _instance.value = CachedScheduleIO.minimize(
                gtfsSchedule,
                scheduleFile,
                realtimeDispatcher
            )
            _downloadState.value = null
            initState = 2
            true
        } catch (e: Exception) {
            Log.e(TAG, "initCachedSchedule: an error occurred while initializing the schedule", e)
            false
        }

    private fun update0(filesDir: File) {
        val today = localEpochDate()
        val scheduleFile = File(filesDir, SCHEDULE_NAME)
        val newScheduleFile = File(filesDir, NEW_SCHEDULE_NAME)
        val downloadFile = File(filesDir, DOWNLOADED_SCHEDULE)
        val newDownloadFile = File(filesDir, NEW_DOWNLOADED_SCHEDULE)

        val cachedVersion = CachedScheduleIO.getFeedInfoOrNull(scheduleFile)?.version
        val downloadedVersion = GtfsScheduleLoader.getFeedInfoOrNull(downloadFile)?.version
        val newCachedFeedInfo = CachedScheduleIO.getFeedInfoOrNull(newScheduleFile)
        var newDownloadedVersion = GtfsScheduleLoader.getFeedInfoOrNull(newDownloadFile)?.version

        val result = GtfsScheduleLoader.download(
            newDownloadFile, newCachedFeedInfo?.version, newDownloadedVersion,
            cachedVersion, downloadedVersion,
            onDownloadProgress = onDownloadProgress,
        )
        _lastDownloadError.value = result.errorType
        result.exception?.let { e ->
            Log.e(TAG, "update: error downloading schedule", e)
        }
        newDownloadedVersion = result.version

        if (newDownloadedVersion != null && (newCachedFeedInfo == null ||
                    newDownloadedVersion > newCachedFeedInfo.version)
        ) try {
            _downloadState.value = DownloadState(DownloadOperation.LOADING_GTFS)
            val gtfsSchedule =
                GtfsScheduleLoader.load(newDownloadFile, realtimeDispatcher, onTripLoadProgress)
            CachedScheduleIO.save(gtfsSchedule, newScheduleFile)

            if (gtfsSchedule.feedInfo.started(today)) {
                _instance.value = CachedScheduleIO.minimize(
                    gtfsSchedule,
                    scheduleFile,
                    realtimeDispatcher,
                )
                newScheduleFile.renameTo(scheduleFile)
                newDownloadFile.renameTo(downloadFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "update: failed to update schedule", e)
        } else if (newCachedFeedInfo?.started(today) == true) try {
            newScheduleFile.renameTo(scheduleFile)
            newDownloadFile.renameTo(downloadFile)
            _downloadState.value = DownloadState(DownloadOperation.LOADING_CACHED)
            _instance.value = CachedScheduleIO.load(scheduleFile, realtimeDispatcher)
        } catch (e: Exception) {
            Log.e(TAG, "update: failed to init new schedule", e)
        }

        _downloadState.value = null
    }

    fun getNewScheduleFile(filesDir: File) = File(filesDir, NEW_SCHEDULE_NAME)

    /*fun getNewScheduleFile(filesDir: File) = File(filesDir, NEW_SCHEDULE_NAME)

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
                output.channel.transferFrom(
                   Channels.newChannel(inputStream),
                   0L,
                   Long.MAX_VALUE
                )
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
    ): DownloadResult {
       val url = try {
          URL(link).also { Log.d(TAG, "URL initialized successfully.") }
       } catch (e: MalformedURLException) {
          Log.w(TAG, "Malformed URL.", e)
          return DownloadResult(ErrorType.MALFORMED_URL)
       }

       val connection = try {
          url.openConnection().also {
             Log.d(TAG, "Connection opened successfully.")
             it.connect()
             Log.d(TAG, "Connected successfully.")
          }
       } catch (e: IOException) {
          Log.w(TAG, "IOException occurred while opening connection.", e)
          return DownloadResult(ErrorType.OPENING_CONNECTION)
       }

       val fieldValue = connection.getHeaderField("Content-Disposition")
          ?: run { // no content, do not download anything
             Log.d(TAG, "No content, not downloading.")
             return DownloadResult(ErrorType.NO_CONTENT)
          }

       Log.d(TAG, "fieldValue exists: $fieldValue")

       val index = fieldValue.indexOf("filename=")
       if (index == -1) {
          Log.d(TAG, "No filename")
          // no file name, do not download
          return DownloadResult(ErrorType.NO_FILENAME)
       }
       val newVersion =
          fieldValue.substring(index + 9).replace("\"", "").feedVersion
       lastCheckedLatestVersion = newVersion
       Log.d(TAG, "Found version: $newVersion")

       var newFeedInfo: FeedInfo? = null

       if (newVersion != cachedVersion) {

          if (newZipFile.isFile) {
             try {
                newFeedInfo = newZipFile.feedInfo

                // do not download if the new schedule is already up-to-date.
                if (newVersion == newFeedInfo.version)
                   return DownloadResult(newFeedInfo, ErrorType.UP_TO_DATE)
             } catch (_: IOException) {
                // new schedule is invalid, delete it
                newZipFile.delete()
             }
          }

          var success = false
          try {
             Log.d(
                TAG,
                "New version exists, downloading it${Typography.ellipsis}"
             )

             connection.inputStream.use { inputStream ->
                FileOutputStream(newZipFile).use { output ->
                   output.channel.transferFrom(
                      Channels.newChannel(inputStream),
                      0L,
                      Long.MAX_VALUE,
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
          return DownloadResult(ErrorType.UP_TO_DATE)
       }

       return DownloadResult(newFeedInfo)
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
                download(
                   link,
                   originalSchedule?.feedInfo?.version,
                   newZipFile,
                   newStopTimes
                )
             var newFeedInfo = downloadResult.newFeedInfo
             errorType = downloadResult.errorType

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
                   newZipFile.delete()
                   return@downloading
                }

             val startsIn = newFeedInfo.startDate.toEpochDay() - localEpochDate()

             // new schedule started
             if (startsIn <= 0L) {
                Log.d(TAG, "New schedule started.")
                // original schedule doesn't exist, load the new one immediately
                if (originalSchedule == null) {
                   Log.d(
                      TAG,
                      "No original schedule found, loading the new one immediately."
                   )
                   replaceSchedule(filesDir, newZipFile, newStopTimes)
                   return@downloading
                }
                Log.d(TAG, "Found original schedule.")

                // new schedule is completely loaded, replace the old one with it immediately
                if (TripsLoader.tripsLoaded(newStopTimes)) {
                   Log.d(
                      TAG,
                      "New schedule loaded, replacing the old one with it."
                   )
                   replaceSchedule(filesDir, newZipFile, newStopTimes)
                   return@downloading
                }

                Log.d(TAG, "Loading new schedule...")
                try {
                   // new schedule isn't loaded. load it and then replace the old one with it
                   val builder = ScheduleBuilder(newZipFile, newStopTimes)
                   replaceSchedule(filesDir, newZipFile, newStopTimes, builder)
                } catch (e: IOException) {
                   Log.e(TAG, "ScheduleBuilder failed", e)
                }
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
                   if (downloadRecursively(
                         versionLink(previousVersion),
                         originalZipFile
                      )
                   ) {
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
                try {
                   TripsLoader.loadTrips(
                      newZipFile,
                      newStopTimes,
                      TripsLoader.PriorityLevel.Hidden
                   )
                } catch (e: IOException) {
                   Log.e(TAG, "Failed to load new trips hidden", e)
                }
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
       (instance as? LoadedSchedule)?.cancel()

       val originalZipFile = File(filesDir, SCHEDULE_NAME)
       val originalStopTimes = File(filesDir, STOP_TIMES)
       originalZipFile.delete()
       originalStopTimes.deleteRecursively()
       newZipFile.renameTo(originalZipFile)
       newStopTimes.renameTo(originalStopTimes)
       try {
          instance = LoadedSchedule(
             parentScope,
             originalZipFile,
             originalStopTimes,
             builder
          )
       } catch (e: Exception) {
          Log.e(TAG, "replaceSchedule: failed to init schedule", e)
       }
    }*/

}
