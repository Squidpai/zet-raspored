package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.transit.realtime.GtfsRealtime
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import hr.squidpai.zetlive.*
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread

/** Class containing GTFS realtime data. */
class Live private constructor(val feedMessage: FeedMessage?) {

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

  companion object {
    private const val TAG = "Live"

    private const val LINK = "https://www.zet.hr/gtfs-rt-protobuf"

    /**
     * Amount of time (in milliseconds) to wait before reloading the live data.
     */
    private const val MAX_AGE = 15_000

    // There are two blank live instances, so when the live data is
    // not available, the instance state swaps between the two
    // blank instances to force recomposition and update live travels.
    private val blankLive1 = Live(null)
    private val blankLive2 = Live(null)

    var instance by mutableStateOf(blankLive1)
      private set

    private var instanceCreated = 0L

    private var paused = false

    private var thread: Thread? = null

    private val threadLock = Object()

    /**
     * Stops updating live data.
     */
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

    /**
     * Initializes the live data loading.
     */
    fun initialize() {
      if (thread == null) thread = thread(isDaemon = true) {
        while (true) {
          init0()

          val sleepAmount = MAX_AGE - (System.currentTimeMillis() - instanceCreated)
          if (sleepAmount > 0) Thread.sleep(sleepAmount)

          synchronized(threadLock) {
            while (paused) {
              Log.d(TAG, "Live data paused.")
              threadLock.wait()
            }
          }
        }
      }
    }

    private fun init0() {
      try {
        Log.d(TAG, "Initializing live data.")
        instance = Live(FeedMessage.parseFrom(URL(LINK).openStream()))
      } catch (e: IOException) {
        Log.w(TAG, "Failed to load live data; $e")
        // Alternate between the two blank live instances to ensure live schedules
        // are always updated every 15 seconds (by forcing recomposition).
        instance = if (instance === blankLive1) blankLive2 else blankLive1
      }
      instanceCreated = System.currentTimeMillis()
    }
  }
}
