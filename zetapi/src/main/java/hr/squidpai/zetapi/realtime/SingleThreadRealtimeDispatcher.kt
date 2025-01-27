package hr.squidpai.zetapi.realtime

import com.google.transit.realtime.GtfsRealtime
import hr.squidpai.zetapi.TripId
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

public class SingleThreadRealtimeDispatcher(
   public var onDownloadResult: (IOException?) -> Unit = { },
   downloadWait: Duration = defaultDownloadWait,
   failedDownloadWait: Duration = defaultFailedDownloadWait,
   oldDataRetention: Duration = defaultOldDataRetention,
) : Thread(), RealtimeDispatcher {

   public companion object {
      public val defaultDownloadWait: Duration = 15.seconds
      public val defaultFailedDownloadWait: Duration = 10.seconds
      public val defaultOldDataRetention: Duration = 1.hours
   }

   private val downloadWaitMillis = downloadWait.inWholeMilliseconds
   private val failedDownloadWaitMillis = failedDownloadWait.inWholeMilliseconds
   private val oldDataRetentionMillis = oldDataRetention.inWholeMilliseconds

   private var feedMessage: GtfsRealtime.FeedMessage? = null
   private var messageCreatedMillis = 0L

   private var running = true

   private val keys = HashSet<Any>()
   private val lock = Object()

   override fun getForTrip(tripId: TripId): RealtimeData =
      feedMessage?.let { RealtimeData.searchForLinearly(it, tripId) }
         ?: RealtimeData.None

   override fun run() {
      while (running) {
         val sleepTime = try {
            feedMessage = RealtimeDispatcher.download()
            messageCreatedMillis = System.currentTimeMillis()
            onDownloadResult(null)

            downloadWaitMillis
         } catch (e: IOException) {
            onDownloadResult(e)

            if (System.currentTimeMillis() >=
               messageCreatedMillis + oldDataRetentionMillis)
               feedMessage = null

            failedDownloadWaitMillis
         }

         try {
            sleep(sleepTime)
         } catch (_: InterruptedException) {
         }

         synchronized(lock) {
            while (keys.isEmpty() && running) lock.wait()
         }
      }
   }

   public fun updateImmediately() {
      interrupt()
   }

   public fun removeListener(key: Any) {
      synchronized(lock) { keys -= key }
   }

   public fun addListener(key: Any) {
      synchronized(lock) {
         val notify = keys.isEmpty()
         keys += key
         if (notify) lock.notify()
      }
   }

   public fun killDispatcher() {
      running = false
      interrupt()
      synchronized(lock) { lock.notify() }
   }

}