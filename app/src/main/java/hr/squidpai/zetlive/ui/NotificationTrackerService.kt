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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.R
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.gtfs.getLiveDisplayData
import hr.squidpai.zetlive.timeToString
import hr.squidpai.zetlive.ui.composables.drawRouteSlider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class NotificationTrackerService : Service() {

   companion object {
      private const val TAG = "NotificationTrackerService"

      private const val CHANNEL_ID = "trackingChannel"

      const val ACTION_REMOVE_NOTIFICATION =
         "hr.squidpai.zetlive.REMOVE_NOTIFICATION"

      fun createNotificationChannel(context: Context) {
         val name = "Praćenje polazaka u obavijestima"
         val importance = NotificationManager.IMPORTANCE_DEFAULT
         val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
         val notificationManager =
            context.getSystemService<NotificationManager>()!!
         notificationManager.createNotificationChannel(mChannel)
      }
   }

   private lateinit var notificationBuilder: NotificationCompat.Builder
   private var selectedDate = 0L
   private lateinit var trip: Trip

   private lateinit var titleText: String

   private val interrupt = CancellationException()

   private fun launchJob() = CoroutineScope(Dispatchers.Default).launch {
      while (true) try {
         val view =
            RemoteViews(packageName, R.layout.layout_notification_tracker)

         val (realtimeDepartures, timeOfDay, nextStopIndex, nextStopValue) =
            getLiveDisplayData(trip, selectedDate)

         val isCancelled = realtimeDepartures == null

         val metrics = resources.displayMetrics
         val width = metrics.widthPixels - TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            96f,
            metrics,
         )
         val height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            metrics,
         )
         val imageBitmap = ImageBitmap(width.toInt(), height.toInt())
         val canvas = Canvas(imageBitmap)
         val drawScope = CanvasDrawScope()
         drawScope.drawContext.canvas = canvas
         drawScope.drawContext.density = Density(metrics.density)
         drawScope.drawContext.size = Size(width, height)
         drawScope.drawRouteSlider(
            value = if (isCancelled) -2f else nextStopValue,
            departures = trip.departures,
            passedTrackColor = colorScheme.primary,
            notPassedTrackColor = colorScheme.surfaceVariant,
            passedStopColor = colorScheme.onPrimary,
            notPassedStopColor = colorScheme.onSurfaceVariant,
            nextStopColor = colorScheme.onSurface,
         )
         view.setImageViewBitmap(R.id.sliderImage, imageBitmap.asAndroidBitmap())

         val textColor = colorScheme.onSurface.toArgb()
         view.setTextColor(R.id.titleText, textColor)
         view.setTextColor(R.id.currentStopText, textColor)
         view.setTextColor(R.id.nextStopText, textColor)
         view.setTextColor(R.id.stopsAfterText, textColor)
         view.setTextColor(R.id.firstStopText, textColor)

         val highlightNextStop: Boolean

         when (nextStopIndex) {
            0 -> {
               highlightNextStop = false
               view.setTextViewText(R.id.currentStopText, trip.stops[0].name)
               view.setViewVisibility(R.id.arrowImage, View.GONE)
               view.setTextViewText(
                  R.id.nextStopText,
                  "  ${Typography.bullet} ${trip.stops[1].name}"
               )

            }

            trip.stops.size -> {
               highlightNextStop = false
               view.setTextViewText(
                  R.id.currentStopText,
                  trip.stops.last().name
               )
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
                     trip.stops[nextStopIndex - 1].name
                  )
               view.setTextViewText(
                  R.id.nextStopText,
                  trip.stops[nextStopIndex].name
               )
               view.setInt(
                  R.id.arrowImage,
                  "setColorFilter",
                  colorScheme.primary.toArgb(),
               )
            }
         }
         view.setTextViewText(R.id.stopsAfterText, buildString {
            for (i in (nextStopIndex + 1).coerceAtLeast(2)..<trip.stops.size) {
               append(' ').append(Typography.bullet).append(' ')
               append(trip.stops[i].name)
            }
         })

         if (isCancelled) {
            view.setFloat(R.id.currentStopText, "setAlpha", .36f)
            view.setFloat(R.id.nextStopText, "setAlpha", .36f)
            view.setFloat(R.id.titleText, "setAlpha", .36f)
         } else {
            view.setFloat(
               if (highlightNextStop) R.id.currentStopText else R.id.nextStopText,
               "setAlpha", .36f,
            )
            view.setInt(
               if (highlightNextStop) R.id.nextStopText else R.id.currentStopText,
               "setPaintFlags",
               Paint.FAKE_BOLD_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG,
            )
         }
         view.setFloat(R.id.stopsAfterText, "setAlpha", .36f)

         view.setTextViewText(R.id.titleText, titleText)

         if (nextStopIndex == 0 || isCancelled) {
            val departureTime = if (isCancelled) 0
            // isCancelled = realtimeDepartures == null
            else (realtimeDepartures!![0]).let { departure ->
               val difference = TimeOfDay(departure).minusMinutes(timeOfDay)

               if (difference <= 0) -1
               else if (difference <= 15) -difference - 1
               else departure
            }

            view.setViewVisibility(R.id.firstStopText, View.VISIBLE)
            view.setTextViewText(
               R.id.firstStopText,
               if (isCancelled) "otkazano"
               else if (departureTime >= 0) "kreće u ${departureTime.timeToString()}"
               else "kreće za ${-departureTime - 1} min",
            )
         }

         ServiceCompat.startForeground(
            this@NotificationTrackerService, 666,
            notificationBuilder.setCustomContentView(view).build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
               ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
         )
         delay(5000L)
      } catch (e: CancellationException) {
         println(e)
         if (e !== interrupt)
            break
      }
   }

   private fun isDarkMode() =
      resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

   private var colorScheme = LightColors

   private fun updateColorScheme() {
      val isDarkMode = isDarkMode()

      colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         if (isDarkMode) dynamicDarkColorScheme(this)
         else dynamicLightColorScheme(this)
      } else if (isDarkMode) DarkColors else LightColors
   }

   private lateinit var job: Job

   override fun onConfigurationChanged(newConfig: Configuration) {
      super.onConfigurationChanged(newConfig)

      updateColorScheme()
      if (::job.isInitialized)
         job.cancel(interrupt)
   }

   override fun onBind(intent: Intent?) = null

   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
      intent!! // guaranteed to not be null because of the START_REDELIVER_INTENT return result
      val routeId = intent.getStringExtra(EXTRA_ROUTE_ID)
      val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
      selectedDate = intent.getLongExtra(EXTRA_SELECTED_DATE, 0L)

      if (routeId == null || tripId == null) {
         if (routeId == null)
            Log.w(TAG, "onStartCommand: no routeId given, stopping service")
         if (tripId == null)
            Log.w(TAG, "onStartCommand: no tripId given, stopping service")

         stopSelf()
         return START_REDELIVER_INTENT
      }

      val schedule = ScheduleManager.instance
         ?: run {
            Log.w(TAG, "onStartCommand: schedule not loaded")
            return START_REDELIVER_INTENT
         }
      val trips = schedule.routes[routeId]?.trips
      trip = trips?.get(tripId)
         ?: run {
            Log.w(TAG, "onStartCommand: trip not found")
            return START_REDELIVER_INTENT
         }

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
               /* context = */
               this,
               /* requestCode = */
               0,
               /* intent = */
               Intent(this, TripDialogActivity::class.java)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  .putExtra(EXTRA_ROUTE_ID, routeId)
                  .putExtra(EXTRA_TRIP_ID, tripId)
                  .putExtra(EXTRA_SELECTED_DATE, selectedDate),
               /* flags = */
               PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
         )
         .addAction(0, "Prestani pratiti", deleteIntent)
         .setDeleteIntent(deleteIntent)
         .setOngoing(false)

      titleText = buildString {
         append(routeId)
         if (!trip.isFirstStopCommon)
            append(' ').append(trip.stops.first().name)
         append(" smjer ")
         append(trip.headsign)
         val specialLabel = Love.giveMeTheSpecialTripLabel(trip)
            ?.let { it.first ?: it.second }
         if (specialLabel != null)
            append(", ").append(specialLabel)
      }

      updateColorScheme()

      job = launchJob()
      ScheduleManager.realtimeDispatcher.addListener(TAG)

      return START_REDELIVER_INTENT
   }

   override fun onTaskRemoved(rootIntent: Intent?) {
      ScheduleManager.realtimeDispatcher.removeListener(TAG)
      job.cancel()
   }

   override fun onDestroy() {
      ScheduleManager.realtimeDispatcher.removeListener(TAG)
      job.cancel()
   }

   class NotificationRemoveReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
         if (intent.action != ACTION_REMOVE_NOTIFICATION)
            return

         context.stopService(
            Intent(
               context,
               NotificationTrackerService::class.java
            )
         )
         context.getSystemService<NotificationManager>()!!.cancel(666)
         ScheduleManager.realtimeDispatcher.removeListener(TAG)
      }
   }

}