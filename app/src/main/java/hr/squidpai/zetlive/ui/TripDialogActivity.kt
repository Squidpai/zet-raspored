package hr.squidpai.zetlive.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hr.squidpai.zetlive.LOADING_TEXT
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.Trip
import hr.squidpai.zetlive.gtfs.TripId
import hr.squidpai.zetlive.gtfs.getArrivalLineRatio
import hr.squidpai.zetlive.gtfs.getDelayByStop
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.localEpochTime
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import kotlin.math.max

class TripDialogActivity : ComponentActivity() {

   companion object {
      private const val TAG = "TripDialogActivity"

      fun selectTrip(context: Context, trip: Trip, timeOffset: Long) {
         context.startActivity(
            Intent(context, TripDialogActivity::class.java)
               .putExtra(EXTRA_ROUTE_ID, trip.routeId)
               .putExtra(EXTRA_TRIP_ID, trip.tripId)
               .putExtra(EXTRA_TIME_OFFSET, timeOffset)
         )
      }
   }

   private var routeId = -1
   private lateinit var tripId: TripId
   private var timeOffset = 0L

   private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
   ) { isGranted ->
      if (isGranted) {
         startNotificationTracking()
         finish()
      } else
         Toast.makeText(
            this,
            "Ne može se postaviti obavijest${Typography.mdash}odbijena je " +
                  "dozvola za postavljanje obavijesti",
            Toast.LENGTH_LONG
         ).show()
   }

   private fun trackInNotifications() {
      if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
         ) == PackageManager.PERMISSION_GRANTED
      ) {
         startNotificationTracking()
         finish()
         return
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
         requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
   }

   private fun startNotificationTracking() {
      startForegroundService(
         Intent(this, NotificationTrackerService::class.java)
            .putExtra(EXTRA_ROUTE_ID, routeId)
            .putExtra(EXTRA_TRIP_ID, tripId)
            .putExtra(EXTRA_TIME_OFFSET, timeOffset)
      )
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)
      if (routeId == -1) {
         Log.w(TAG, "onCreate: no routeId given, finishing activity early")
         finish()
         return
      }

      tripId = intent.getStringExtra(EXTRA_TRIP_ID)
         ?: run {
            Log.w(TAG, "onCreate: no tripId given, finishing activity early")
            finish()
            return
         }

      timeOffset = intent.getLongExtra(EXTRA_TIME_OFFSET, 0L)

      setContent {
         AppTheme {
            var isAbsoluteTime by remember { mutableStateOf(false) }

            val schedule = Schedule.instance
            val trip = schedule.getTripsOfRoute(routeId).value?.list?.get(key = tripId)

            AlertDialog(
               onDismissRequest = ::finish,
               confirmButton = {
                  TextButton(onClick = ::finish) {
                     Text("Zatvori")
                  }
               },
               title = {
                  val route = schedule.routes?.list?.get(key = routeId)

                  Row(verticalAlignment = Alignment.CenterVertically) {
                     Text(
                        text = if (route != null && trip != null)
                           "${route.shortName} smjer ${trip.headsign}" else LOADING_TEXT,
                        modifier = Modifier.weight(1f),
                     )

                     val icon = if (isAbsoluteTime) Symbols.ClockLoader10 else Symbols.Schedule
                     IconButton(
                        icon, "Promjeni prikaz vremena",
                        onClick = { isAbsoluteTime = !isAbsoluteTime },
                     )
                     Box {
                        var expanded by remember { mutableStateOf(false) }

                        IconButton(
                           Symbols.MoreVert, "Dodatke opcije",
                           onClick = { expanded = true },
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                           DropdownMenuItem(
                              text = { Text("Prati u obavijestima") },
                              onClick = { trackInNotifications() }
                           )
                        }
                     }
                  }
               },
               text = text@{
                  val stops = Schedule.instance.stops

                  if (stops == null || trip == null) {
                     CircularProgressIndicator()
                     return@text
                  }

                  val stopTimeUpdate = trip.tripId.let { id ->
                     Live.instance.findForTrip(id)?.tripUpdate?.stopTimeUpdateList
                  }

                  //val firstUpdate = stopTimeUpdate?.fastMinByOrNull { it.stopSequence }

                  val time =
                     if (timeOffset != 0L) localCurrentTimeMillis() else localEpochTime().toLong()
                  val timeOfDay = ((time - timeOffset) / MILLIS_IN_SECONDS).toInt()

                  //val liveStopId = firstUpdate?.stopId?.toStopId()

                  val delays = stopTimeUpdate.getDelayByStop()

                  val nextStopIndex = trip.findNextStopIndex(timeOfDay, delays)
                  /*run nextStopIndex@{
                stopTime.departures.forEachIndexed { index, departure ->
                  if (time < timeOffset + (delays[index] + departure) * MILLIS_IN_SECONDS) {
                    return@nextStopIndex index
                  }
                }
                return@nextStopIndex stopTime.stops.size
              }*/

                  val nextStopValue = when (nextStopIndex) {
                     0 -> 0f
                     trip.departures.size -> trip.departures.size.toFloat()
                     else -> nextStopIndex + getArrivalLineRatio(
                        trip.departures,
                        nextStopIndex,
                        delays,
                        timeOfDay
                     )
                  }

                  LazyColumn(
                     modifier = Modifier.fillMaxWidth(),
                     state = rememberLazyListState((nextStopIndex - 4).coerceAtLeast(0))
                  ) {
                     items(trip.stops.size) {
                        val stop = stops.list[trip.stops[it].toStopId()]
                        val departure = trip.departures[it]
                        val offsetDeparture = departure + timeOffset / MILLIS_IN_SECONDS
                        val delay = delays[it]

                        Layout(
                           content = {
                              val filled = MaterialTheme.colorScheme.primary
                              val notFilled = MaterialTheme.colorScheme.onSurface

                              val fillCircle = it < nextStopIndex || it == 0
                              val circleTint = if (fillCircle) filled else notFilled

                              Canvas(Modifier) {
                                 val (width, height) = size
                                 if (it != 0) {
                                    val prefillRatio =
                                       ((nextStopValue - (it + 1 - 0.5f)) * 2f).coerceIn(0f, 1f)
                                    val prefillLength = prefillRatio * (height / 2 - width / 6)
                                    if (prefillRatio != 0f) drawLine(
                                       color = filled,
                                       start = Offset(width / 2, 0f),
                                       end = Offset(width / 2, prefillLength),
                                       strokeWidth = width / 16,
                                    )
                                    if (prefillRatio != 1f) drawLine(
                                       color = notFilled,
                                       start = Offset(width / 2, prefillLength),
                                       end = Offset(width / 2, (height / 2 - width / 6)),
                                       strokeWidth = width / 16,
                                    )
                                 }
                                 drawCircle(
                                    color = circleTint,
                                    radius = width / 6,
                                    style = if (fillCircle) Fill else Stroke(width = width / 16),
                                 )
                                 if (it != trip.stops.lastIndex) {
                                    val prefillRatio =
                                       ((nextStopValue - (it + 1)) * 2f).coerceIn(0f, 1f)
                                    val prefillLength = prefillRatio * (height / 2 - width / 6)
                                    val beginY = (height / 2 + width / 6)
                                    if (prefillRatio != 0f) drawLine(
                                       color = filled,
                                       start = Offset(width / 2, beginY),
                                       end = Offset(width / 2, beginY + prefillLength),
                                       strokeWidth = width / 16,
                                    )
                                    if (prefillRatio != 1f) drawLine(
                                       color = notFilled,
                                       start = Offset(width / 2, beginY + prefillLength),
                                       end = Offset(width / 2, height),
                                       strokeWidth = width / 16,
                                    )
                                 }
                              }

                              val stopName = stop?.name.orLoading()

                              if (it < nextStopIndex) Text(
                                 text = stopName,
                                 color = lerp(
                                    MaterialTheme.colorScheme.onSurface,
                                    MaterialTheme.colorScheme.surface,
                                    fraction = .36f
                                 ),
                              ) else Column {
                                 Text(stopName)
                                 Text(
                                    buildAnnotatedString {
                                       val t = offsetDeparture + delay - time / 1000
                                       if (!isAbsoluteTime && t < 3600) {
                                          if (t < 60)
                                             append("za 0 min")
                                          else {
                                             append("za ")
                                             append((t / 60 % 60).toString())
                                             append(" min")
                                          }
                                       } else {
                                          if (offsetDeparture / 60 != (offsetDeparture + delay) / 60) {
                                             withStyle(
                                                SpanStyle(
                                                   textDecoration = TextDecoration.LineThrough,
                                                   fontWeight = FontWeight.Normal
                                                )
                                             ) {
                                                append(departure.timeToString())
                                             }
                                             append(' ')
                                             append((departure + delay).timeToString())
                                          } else append(departure.timeToString())
                                       }
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                 )
                              }
                           },
                           modifier = Modifier
                              .fillMaxWidth()
                              .clip(MaterialTheme.shapes.large)
                              .clickable(enabled = stop != null) {
                                 if (stop != null) startActivity(
                                    Intent(
                                       this@TripDialogActivity,
                                       StopScheduleActivity::class.java,
                                    ).putExtra(StopScheduleActivity.EXTRA_STOP, stop.id.value)
                                 )
                              },
                           measurePolicy = TripRowMeasurePolicy,
                        )
                     }
                  }
               }
            )
         }
      }
   }

   override fun onPause() {
      super.onPause()
      Live.pauseLiveData()
   }

   override fun onResume() {
      super.onResume()
      Live.resumeLiveData()
   }

   private data object TripRowMeasurePolicy : MeasurePolicy {
      override fun MeasureScope.measure(
         measurables: List<Measurable>,
         constraints: Constraints
      ): MeasureResult {
         val px48 = 48.dp.roundToPx()

         val stopPlaceable = measurables[1].measure(
            constraints.copy(
               minWidth = (constraints.minWidth - px48).coerceAtLeast(0),
               maxWidth = (constraints.maxWidth - px48).coerceAtLeast(0),
            )
         )

         val height = max(stopPlaceable.height, px48)

         val canvasPlaceable = measurables[0].measure(Constraints.fixed(px48, height))

         return layout(px48 + stopPlaceable.width, height) {
            canvasPlaceable.place(0, 0)
            stopPlaceable.place(48.dp.roundToPx(), (height - stopPlaceable.height) / 2)
         }
      }
   }

}