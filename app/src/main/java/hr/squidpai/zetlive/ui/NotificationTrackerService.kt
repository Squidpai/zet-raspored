package hr.squidpai.zetlive.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.collection.IntObjectMap
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.R
import hr.squidpai.zetlive.associateWith
import hr.squidpai.zetlive.get
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Love
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.Trip
import hr.squidpai.zetlive.gtfs.getArrivalLineRatio
import hr.squidpai.zetlive.gtfs.getDelayByStop
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.localEpochTime
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString


class NotificationTrackerService : Service(), Live.UpdateListener {

   companion object {
      private const val TAG = "NotificationTrackerService"

      private const val MAX_SLIDER_VALUE = 100_000_000

      private const val CHANNEL_ID = "trackingChannel"

      const val ACTION_REMOVE_NOTIFICATION = "hr.squidpai.zetlive.REMOVE_NOTIFICATION"

      fun createNotificationChannel(context: Context) {
         val name = "Praćenje polazaka u obavijestima"
         val importance = NotificationManager.IMPORTANCE_DEFAULT
         val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
         val notificationManager = context.getSystemService<NotificationManager>()!!
         notificationManager.createNotificationChannel(mChannel)
      }
   }

   private lateinit var notificationBuilder: NotificationCompat.Builder
   private var timeOffset = 0L
   private lateinit var trip: Trip
   private lateinit var stopNames: IntObjectMap<String>

   /*private var overriddenFirstStopLabel: String? = null
   private var specialLabel: String? = null
   private var headsignLabel: String? = null*/
   private lateinit var titleText: String

   override fun onUpdated(live: Live) {
      if (!::trip.isInitialized || !::stopNames.isInitialized)
         return
      val trip = trip
      val stopNames = stopNames
      val view = RemoteViews(packageName, R.layout.layout_notification_tracker)

      val time = if (timeOffset != 0L) localCurrentTimeMillis() else localEpochTime().toLong()
      val timeOfDay = ((time - timeOffset) / MILLIS_IN_SECONDS).toInt()

      val delays = live.findForTrip(trip.tripId)?.tripUpdate?.stopTimeUpdateList.getDelayByStop()

      val nextStopIndex = trip.findNextStopIndex(timeOfDay, delays)

      val nextStopRatio = when (nextStopIndex) {
         0 -> 0f
         trip.departures.size -> 1f
         else -> (nextStopIndex + getArrivalLineRatio(
            trip.departures,
            nextStopIndex,
            delays,
            timeOfDay
         )) / trip.departures.size
      }
      view.setProgressBar(
         /* viewId = */ R.id.liveTravelSlider,
         /* max = */ MAX_SLIDER_VALUE,
         /* progress = */ (nextStopRatio * MAX_SLIDER_VALUE).toInt(),
         /* indeterminate = */ false,
      )

      when (nextStopIndex) {
         0 -> {
            view.setTextViewText(R.id.currentStopText, stopNames[trip.stops[0]])
            view.setViewVisibility(R.id.arrowImage, View.GONE)
            view.setTextViewText(
               R.id.nextStopText,
               " ${Typography.bullet} ${stopNames[trip.stops[1]]}"
            )

         }

         trip.stops.size -> {
            view.setTextViewText(R.id.currentStopText, stopNames[trip.stops.last()])
            view.setViewVisibility(R.id.arrowImage, View.GONE)
            view.setTextViewText(R.id.nextStopText, "")
         }

         else -> {
            view.setTextViewText(R.id.currentStopText, stopNames[trip.stops[nextStopIndex - 1]])
            view.setTextViewText(R.id.nextStopText, stopNames[trip.stops[nextStopIndex]])
            view.setInt(R.id.arrowImage, "setColorFilter", fetchAccentColor())
         }
      }
      view.setTextViewText(R.id.stopsAfterText, buildString {
         for (i in (nextStopIndex + 1).coerceAtLeast(2)..<trip.stops.size) {
            append(' ').append(Typography.bullet).append(' ')
            append(stopNames[trip.stops[i]])
         }
      })

      view.setTextViewText(R.id.titleText, titleText)

      if (nextStopIndex == 0) {
         val departureTime = trip.departures[0].let { departure ->
            if (departure <= timeOfDay) -1
            else if (departure - timeOfDay <= 15 * 60) timeOfDay - departure - 1
            else departure
         }

         view.setViewVisibility(R.id.firstStopText, View.VISIBLE)
         view.setTextViewText(
            R.id.firstStopText,
            if (departureTime >= 0) "kreće u ${departureTime.timeToString()}"
            else "kreće za ${(-departureTime - 1) / 60} min",
         )
      }

      ServiceCompat.startForeground(
         this, 666, notificationBuilder.setCustomContentView(view).build(),
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
      )
   }

   private fun fetchAccentColor(): Int {
      if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
         Configuration.UI_MODE_NIGHT_YES
      ) {
         return Color.WHITE
      }
      return Color.BLACK
   }

   override fun onBind(intent: Intent?) = null

   override fun onCreate() {
      Live.setNotificationTrackerListener(this)
   }

   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
      intent!! // guaranteed to not be null because of the START_REDELIVER_INTENT return result
      val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)
      val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
      timeOffset = intent.getLongExtra(EXTRA_TIME_OFFSET, 0L)

      if (routeId == -1 || tripId == null) {
         if (routeId == -1)
            Log.w(TAG, "onStartCommand: no routeId given, stopping service")
         if (tripId == null)
            Log.w(TAG, "onStartCommand: no tripId given, stopping service")
         stopSelf()
         return START_REDELIVER_INTENT
      }

      val schedule = Schedule.instance
      val trips = schedule.getTripsOfRoute(routeId).value
      trip = trips?.list?.get(tripId)
         ?: run {
            Log.w(TAG, "onStartCommand: trip not found")
            return START_REDELIVER_INTENT
         }
      val stopsList = schedule.stops?.list
      stopNames = trip.stops.associateWith { stopsList?.get(it.toStopId())?.name.orLoading() }

      val deleteIntent = PendingIntent.getBroadcast(
         /* context = */ this,
         /* requestCode = */ 0,
         /* intent = */
         Intent(this, NotificationRemoveReceiver::class.java)
            .setAction(ACTION_REMOVE_NOTIFICATION),
         /* flags = */ PendingIntent.FLAG_IMMUTABLE,
      )

      notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
         .setSmallIcon(R.drawable.notification_app_icon)
         .setShowWhen(false)
         .setAllowSystemGeneratedContextualActions(false)
         .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
         .setStyle(NotificationCompat.DecoratedCustomViewStyle())
         .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
         .setSilent(true)
         .setContentIntent(
            PendingIntent.getActivity(
               /* context = */ this,
               /* requestCode = */ 0,
               /* intent = */
               Intent(this, TripDialogActivity::class.java)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  .putExtra(EXTRA_ROUTE_ID, routeId)
                  .putExtra(EXTRA_TRIP_ID, tripId)
                  .putExtra(EXTRA_TIME_OFFSET, timeOffset),
               /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
         )
         .addAction(0, "Prestani pratiti", deleteIntent)
         .setDeleteIntent(deleteIntent)
         .setOngoing(false)

      titleText = buildString {
         append(routeId)
         if (trip.stops.first() != trips.commonFirstStop[trip.directionId].value)
            append(' ').append(stopNames[trip.stops.first()])
         append(" smjer ")
         append(trip.headsign)
         val specialLabel = Love.giveMeTheSpecialTripLabel(trip)
            ?.let { it.first ?: it.second }
         if (specialLabel != null)
            append(", ").append(specialLabel)
      }

      onUpdated(Live.instance)

      return START_REDELIVER_INTENT
   }

   override fun onTaskRemoved(rootIntent: Intent?) {
      Live.removeNotificationTrackerListener()
   }

   override fun onDestroy() {
      Live.removeNotificationTrackerListener()
   }

   class NotificationRemoveReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
         if (intent.action != ACTION_REMOVE_NOTIFICATION)
            return

         context.stopService(Intent(context, NotificationTrackerService::class.java))
         context.getSystemService<NotificationManager>()!!.cancel(666)
         Live.removeNotificationTrackerListener()
      }
   }

}