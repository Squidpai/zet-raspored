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
import android.graphics.Paint
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.collection.IntObjectMap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.MILLIS_IN_DAY
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.R
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.SECONDS_IN_HOUR
import hr.squidpai.zetlive.associateWith
import hr.squidpai.zetlive.get
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Love
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.Trip
import hr.squidpai.zetlive.gtfs.getArrivalLineRatio
import hr.squidpai.zetlive.gtfs.getDelayByStop
import hr.squidpai.zetlive.gtfs.getDelayByStopForTrip
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import hr.squidpai.zetlive.ui.composables.RouteSlider
import hr.squidpai.zetlive.utcEpochDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color.Companion as ComposeColor


class NotificationTrackerService : Service(), Live.UpdateListener {

   companion object {
      private const val TAG = "NotificationTrackerService"

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
   private var selectedDate = 0
   private lateinit var trip: Trip
   private lateinit var stopNames: IntObjectMap<String>

   private lateinit var titleText: String

   val mainScope = CoroutineScope(Dispatchers.Main)

   override fun onUpdated(live: Live) {
      mainScope.launch {
         val view = RemoteViews(packageName, R.layout.layout_notification_tracker)

         val currentTimeMillis = localCurrentTimeMillis()
         val dateDifference = (currentTimeMillis / MILLIS_IN_DAY).toInt() - selectedDate

         val offsetTime = (currentTimeMillis % MILLIS_IN_DAY +
               dateDifference * MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS

         val delays = live.findForTripIgnoringServiceId(trip.tripId)?.tripUpdate
            ?.takeIf {
               abs(
                  it.timestamp - (trip.departures.first() +
                        (utcEpochDate() - dateDifference) * SECONDS_IN_DAY)
               ) < 12 * SECONDS_IN_HOUR
            }?.stopTimeUpdateList.getDelayByStop()

         val nextStopIndex = trip.findNextStopIndex(offsetTime, delays)

         val nextStopRatio = when (nextStopIndex) {
            0 -> 0f
            trip.departures.size -> trip.departures.size.toFloat()
            else -> nextStopIndex - 1 + getArrivalLineRatio(
               trip.departures,
               nextStopIndex,
               delays,
               offsetTime,
            )
         }
         /*view.setProgressBar(
            /* viewId = */ R.id.liveTravelSlider,
            /* max = */ MAX_SLIDER_VALUE,
            /* progress = */ (nextStopRatio * MAX_SLIDER_VALUE).toInt(),
            /* indeterminate = */ false,
         )*/
         val bitmap = useVirtualDisplay(applicationContext) { display ->

            val metrics = resources.displayMetrics
            /*@Suppress("DEPRECATION")
            ContextCompat.getDisplayOrDefault(this@NotificationTrackerService)
               .getMetrics(metrics)*/
            val width = metrics.widthPixels -
                  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96f, metrics)
            val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, metrics)

            println("Width: $width")

            captureComposable(
               context = this@NotificationTrackerService,
               size = IntSize(width.toInt(), height.toInt()),
               display = display
            ) {
               LaunchedEffect(Unit) { capture() }

               AppTheme {
                  Surface(
                     modifier = Modifier.fillMaxSize(),
                     color = ComposeColor.Unspecified,
                     contentColor = MaterialTheme.colorScheme.onSurface,
                  ) {
                     RouteSlider(value = nextStopRatio, departures = trip.departures)
                  }
               }
            }
         }
         view.setImageViewBitmap(R.id.sliderImage, bitmap)

         val textColor = fetchTextColor()
         view.setTextColor(R.id.titleText, textColor)
         view.setTextColor(R.id.currentStopText, textColor)
         view.setTextColor(R.id.nextStopText, textColor)
         view.setTextColor(R.id.stopsAfterText, textColor)
         view.setTextColor(R.id.firstStopText, textColor)

         val highlightNextStop: Boolean

         when (nextStopIndex) {
            0 -> {
               highlightNextStop = false
               view.setTextViewText(R.id.currentStopText, stopNames[trip.stops[0]])
               view.setViewVisibility(R.id.arrowImage, View.GONE)
               view.setTextViewText(
                  R.id.nextStopText,
                  "  ${Typography.bullet} ${stopNames[trip.stops[1]]}"
               )

            }

            trip.stops.size -> {
               highlightNextStop = false
               view.setTextViewText(R.id.currentStopText, stopNames[trip.stops.last()])
               view.setViewVisibility(R.id.arrowImage, View.GONE)
               view.setTextViewText(R.id.nextStopText, "")
            }

            else -> {
               highlightNextStop = Data.highlightNextStop
               if (highlightNextStop)
                  view.setViewVisibility(R.id.currentStopText, View.GONE)
               else
                  view.setTextViewText(
                     R.id.currentStopText,
                     stopNames[trip.stops[nextStopIndex - 1]]
                  )
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

         view.setFloat(
            if (highlightNextStop) R.id.currentStopText else R.id.nextStopText,
            "setAlpha", .36f,
         )
         view.setInt(
            if (highlightNextStop) R.id.nextStopText else R.id.currentStopText,
            "setPaintFlags", Paint.FAKE_BOLD_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG,
         )
         view.setFloat(R.id.stopsAfterText, "setAlpha", .36f)

         view.setTextViewText(R.id.titleText, titleText)

         if (nextStopIndex == 0) {
            val departureTime =
               (trip.departures[0] + live.getDelayByStopForTrip(trip.tripId)[0]).let { departure ->
                  val relativeTime = departure - offsetTime
                  if (relativeTime <= 0) -1
                  else if (relativeTime <= 15 * 60) -relativeTime - 1
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
            this@NotificationTrackerService, 666,
            notificationBuilder.setCustomContentView(view).build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
               ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
         )
      }
   }

   private fun isDarkMode() =
      resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

   private fun fetchAccentColor(): Int {
      val isDarkMode = isDarkMode()

      return if (Build.VERSION.SDK_INT >= 34)
         resources.getColor(
            if (isDarkMode) android.R.color.system_primary_dark
            else android.R.color.system_primary_light,
            theme,
         )
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
         resources.getColor(
            if (isDarkMode) android.R.color.system_accent1_200
            else android.R.color.system_accent1_600,
            theme,
         )
      else (if (isDarkMode) DarkColors else LightColors).primary.toArgb()
   }

   private fun fetchTextColor() =
      if (isDarkMode()) AndroidColor.WHITE
      else AndroidColor.BLACK

   override fun onConfigurationChanged(newConfig: Configuration) {
      super.onConfigurationChanged(newConfig)

      onUpdated(Live.instance)
   }

   override fun onBind(intent: Intent?) = null

   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
      intent!! // guaranteed to not be null because of the START_REDELIVER_INTENT return result
      val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)
      val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
      selectedDate = intent.getIntExtra(EXTRA_SELECTED_DATE, 0)

      if (routeId == -1 || tripId == null) {
         if (routeId == -1)
            Log.w(TAG, "onStartCommand: no routeId given, stopping service")
         if (tripId == null)
            Log.w(TAG, "onStartCommand: no tripId given, stopping service")

         stopSelf()
         return START_REDELIVER_INTENT
      }

      val schedule = Schedule.loadedInstance
         ?: run {
            Log.w(TAG, "onStartCommand: schedule not loaded")
            return START_REDELIVER_INTENT
         }
      val trips = schedule.getTripsOfRoute(routeId).value
      trip = trips?.list?.get(tripId)
         ?: run {
            Log.w(TAG, "onStartCommand: trip not found")
            return START_REDELIVER_INTENT
         }
      val headsign = trip.headsign ?: trips.commonHeadsignByDay[trip.serviceId]
         ?.get(trip.directionId) ?: run {
         Log.w(TAG, "onStartCommand: trip headsign not found")
      }
      val stopsList = schedule.stops.list
      stopNames = trip.stops.associateWith { stopsList[it.toStopId()]?.name.orLoading() }

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
                  .putExtra(EXTRA_SELECTED_DATE, selectedDate),
               /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
         )
         .addAction(0, "Prestani pratiti", deleteIntent)
         .setDeleteIntent(deleteIntent)
         .setOngoing(false)

      titleText = buildString {
         append(routeId)
         if (trip.stops.first() != trips.commonFirstStopByDay[trip.serviceId]?.get(trip.directionId)?.value)
            append(' ').append(stopNames[trip.stops.first()])
         append(" smjer ")
         append(headsign)
         val specialLabel = Love.giveMeTheSpecialTripLabel(trip)
            ?.let { it.first ?: it.second }
         if (specialLabel != null)
            append(", ").append(specialLabel)
      }

      onUpdated(Live.instance)
      Live.setNotificationTrackerListener(this)

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