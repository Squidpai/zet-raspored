package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.transit.realtime.GtfsRealtime
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import hr.squidpai.zetlive.MILLIS_IN_HOURS
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread

/** Class containing GTFS realtime data. */
class Live private constructor(
   val feedMessage: FeedMessage?,
   /** A live instance is considered cached if a new live instance failed to download. */
   val isCached: Boolean,
) {

   /**
    * Returns the first [GtfsRealtime.FeedEntity] whose tripId matches [tripId].
    */
   fun findForTrip(tripId: TripId) =
      feedMessage?.entityList?.find { it.tripUpdate.trip.tripId == tripId }

   /**
    * Performs [action] on each element whose routeId matches [routeId].
    */
   inline fun forEachOfRoute(routeId: RouteId, action: (GtfsRealtime.FeedEntity) -> Unit) {
      feedMessage?.entityList?.forEach { entity ->
         if (entity.tripUpdate.trip.routeId.toInt() == routeId) {
            action(entity)
         }
      }
   }

   private fun cached() = Live(feedMessage, isCached = true)

   fun interface UpdateListener {
      fun onUpdated(live: Live)
   }

   companion object {
      private const val TAG = "Live"

      private const val LINK = "https://www.zet.hr/gtfs-rt-protobuf"

      /** Amount of time (in milliseconds) to wait before reloading the live data. */
      private const val MAX_AGE = 15_000

      /**
       * Default live instance loaded before initialization.
       *
       * `isCached` is `false` so the "no internet" icon is not displayed
       * until an actual error occurs.
       */
      private val blankLive0 = Live(null, isCached = false)

      // There are two blank live instances, so when the live data is
      // not available, the instance state swaps between the two
      // blank instances to force recomposition and update live travels.
      private val blankLive1 = Live(null, isCached = true)
      private val blankLive2 = Live(null, isCached = true)

      var instance by mutableStateOf(blankLive0)
         private set

      private var instanceCreated = 0L

      private var paused = false

      private var thread: Thread? = null

      private val threadLock = Object()

      private var notificationTrackerListener: UpdateListener? = null

      /** Stops updating live data. */
      fun pauseLiveData() {
         paused = true
      }

      /**
       * Resumes updating live data.
       *
       * Note that [initialize] must be called to start loading live data.
       */
      fun resumeLiveData() {
         paused = false
         synchronized(threadLock) { threadLock.notify() }
      }

      fun setNotificationTrackerListener(listener: UpdateListener) {
         notificationTrackerListener = listener
         synchronized(threadLock) { threadLock.notify() }
      }

      fun removeNotificationTrackerListener() {
         notificationTrackerListener = null
      }

      private var onForceUpdate: (() -> Unit)? = null

      /** Initializes the live data loading. */
      fun initialize() {
         if (thread == null) thread = thread(isDaemon = true) {
            while (true) {
               val newInstance = init0()
               notificationTrackerListener?.onUpdated(newInstance)
               onForceUpdate?.let {
                  it()
                  onForceUpdate = null
               }
               instance = newInstance

               val sleepAmount = MAX_AGE - (System.currentTimeMillis() - instanceCreated)
               if (sleepAmount > 0) Thread.sleep(sleepAmount)

               synchronized(threadLock) {
                  while (paused && notificationTrackerListener == null && onForceUpdate == null) {
                     Log.d(TAG, "Live data paused.")
                     threadLock.wait()
                  }
               }
            }
         }
      }

      fun updateNow(onFinish: () -> Unit) {
         onForceUpdate = onFinish
         synchronized(threadLock) { threadLock.notify() }
      }

      private fun init0(): Live {
         val newInstance =
            try {
               Log.d(TAG, "Initializing live data.")
               Live(FeedMessage.parseFrom(URL(LINK).openStream()), isCached = false)
            } catch (e: IOException) {
               Log.w(TAG, "Failed to load live data; $e")
               val instance = instance
               when {
                  // if the last loaded live instance is not older than an hour, keep it
                  // since it probably better represents the current schedule than no live instance
                  instance.feedMessage != null && System.currentTimeMillis() -
                        instance.feedMessage.header.timestamp < 1 * MILLIS_IN_HOURS ->
                     instance.cached()

                  // Alternate between the two blank live instances to ensure live schedules
                  // are always updated every 15 seconds (by forcing recomposition).
                  instance === blankLive1 -> blankLive2
                  else -> blankLive1
               }
            }
         instanceCreated = System.currentTimeMillis()
         return newInstance
      }
   }
}
