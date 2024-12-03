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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import hr.squidpai.zetlive.MILLIS_IN_DAY
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.SECONDS_IN_HOUR
import hr.squidpai.zetlive.get
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.PreciseDelayByStop
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.Trip
import hr.squidpai.zetlive.gtfs.TripId
import hr.squidpai.zetlive.gtfs.getArrivalLineRatio
import hr.squidpai.zetlive.gtfs.getDelayByStop
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.localEpochDate
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import hr.squidpai.zetlive.ui.composables.HintIconButton
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.utcEpochDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TripDialogActivity : ComponentActivity() {

   companion object {
      private const val TAG = "TripDialogActivity"

      fun show(context: Context, trip: Trip, selectedDate: Int) {
         context.startActivity(
            Intent(context, TripDialogActivity::class.java)
               .putExtra(EXTRA_ROUTE_ID, trip.routeId)
               .putExtra(EXTRA_TRIP_ID, trip.tripId)
               .putExtra(EXTRA_SELECTED_DATE, selectedDate)
         )
      }
   }

   private var routeId = -1
   private lateinit var tripId: TripId
   private var selectedDate = 0

   private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
   ) { isGranted ->
      if (isGranted) {
         startNotificationTracking()
         finish()
      } else
         Toast.makeText(
            this,
            "Ne može se postaviti obavijest${Typography.mdash}" +
                  "odbijena je dozvola za postavljanje obavijesti.",
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
            .putExtra(EXTRA_SELECTED_DATE, selectedDate)
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

      selectedDate = intent.getIntExtra(EXTRA_SELECTED_DATE, 0)
      if (selectedDate == 0)
         selectedDate = localEpochDate().toInt()

      setContent {
         AppTheme {
            var isAbsoluteTime by remember { mutableStateOf(false) }

            val schedule = Schedule.loadedInstance
            val trips = schedule?.getTripsOfRoute(routeId)?.value
            val trip = trips?.list?.get(key = tripId)

            AlertDialog(
               onDismissRequest = ::finish,
               confirmButton = {
                  TextButton(onClick = ::finish) {
                     Text("Zatvori")
                  }
               },
               title = {
                  val route = schedule?.routes?.list?.get(key = routeId)

                  Row(verticalAlignment = Alignment.CenterVertically) {
                     Text(
                        text = if (route != null && trip != null)
                           "${route.shortName} smjer ${
                              trip.headsign ?: trips.commonHeadsignByDay[trip.serviceId]?.get(
                                 trip.directionId
                              ).orLoading()
                           }" else LOADING_TEXT,
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
                  val stops = schedule?.stops

                  if (stops == null || trip == null) {
                     CircularProgressIndicator()
                     return@text
                  }

                  val currentTimeMillis = localCurrentTimeMillis()
                  val dateDifference = (currentTimeMillis / MILLIS_IN_DAY).toInt() - selectedDate

                  val offsetTime = (currentTimeMillis % MILLIS_IN_DAY +
                        dateDifference * MILLIS_IN_DAY).toInt() / MILLIS_IN_SECONDS

                  val tripUpdate = trip.tripId.let { id ->
                     Live.instance.findForTripIgnoringServiceId(id)?.tripUpdate
                        ?.takeIf {
                           abs(
                              it.timestamp - (trip.departures.first() +
                                    (utcEpochDate() - dateDifference) * SECONDS_IN_DAY)
                           ) < 12 * SECONDS_IN_HOUR
                        }
                  }

                  val delays = tripUpdate?.stopTimeUpdateList.getDelayByStop()

                  val isLive = delays is PreciseDelayByStop

                  val nextStopIndex = trip.findNextStopIndex(offsetTime, delays)

                  val nextStopValue = when (nextStopIndex) {
                     0 -> 0f
                     trip.departures.size -> trip.departures.size.toFloat()
                     else -> nextStopIndex + getArrivalLineRatio(
                        trip.departures,
                        nextStopIndex,
                        delays,
                        offsetTime,
                     )
                  }

                  LazyColumn(
                     modifier = Modifier.fillMaxWidth(),
                     state = rememberLazyListState((nextStopIndex - 4).coerceAtLeast(0))
                  ) {
                     items(trip.stops.size) {
                        val stop = stops.list[trip.stops[it].toStopId()]
                        val departure = trip.departures[it]
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
                                    if (prefillRatio != 1f) drawLine(
                                       color = notFilled,
                                       start = Offset(width / 2, prefillLength),
                                       end = Offset(width / 2, (height / 2 - width / 6)),
                                       strokeWidth = width / 16,
                                       cap = StrokeCap.Round,
                                    )
                                    if (prefillRatio != 0f) drawLine(
                                       color = filled,
                                       start = Offset(width / 2, 0f),
                                       end = Offset(width / 2, prefillLength),
                                       strokeWidth = width / 16,
                                       cap = StrokeCap.Round,
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
                                    if (prefillRatio != 1f) drawLine(
                                       color = notFilled,
                                       start = Offset(width / 2, beginY + prefillLength),
                                       end = Offset(width / 2, height),
                                       strokeWidth = width / 16,
                                       cap = StrokeCap.Round,
                                    )
                                    if (prefillRatio != 0f) drawLine(
                                       color = filled,
                                       start = Offset(width / 2, beginY),
                                       end = Offset(width / 2, beginY + prefillLength),
                                       strokeWidth = width / 16,
                                       cap = StrokeCap.Round,
                                    )
                                 }
                              }

                              val stopName = stop?.name.orLoading()

                              Column {
                                 val passed = it < nextStopIndex

                                 val stopColor: Color
                                 val timeColor: Color
                                 if (passed) {
                                    stopColor = lerp(
                                       MaterialTheme.colorScheme.onSurface,
                                       MaterialTheme.colorScheme.surface,
                                       fraction = .36f
                                    )
                                    timeColor = stopColor
                                 } else {
                                    stopColor = Color.Unspecified
                                    timeColor = MaterialTheme.colorScheme.primary
                                 }
                                 Text(stopName, color = stopColor)
                                 if (!passed || isAbsoluteTime) Text(
                                    buildAnnotatedString {
                                       val t = departure + delay - offsetTime
                                       if (!isAbsoluteTime && t < 3600) {
                                          if (t < 60)
                                             append("za 0 min")
                                          else {
                                             append("za ")
                                             append((t / 60 % 60).toString())
                                             append(" min")
                                          }
                                       } else {
                                          if (departure / 60 != (departure + delay) / 60) {
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
                                    color = timeColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                 )
                              }

                              if (isLive && it == (nextStopIndex - 1).coerceAtLeast(0))
                                 HintIconButton(
                                    Symbols.MyLocation,
                                    contentDescription = null,
                                    tooltipTitle = "GPS praćenje",
                                    tooltipText = "Ovom vozilu moguće je pratiti lokaciju. " +
                                          "Raspored prikazan ovdje prilagođen je prema " +
                                          "lokaciji vozila."
                                 )
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

         val iconPlaceable = measurables.getOrNull(2)
            ?.takeIf { constraints.maxWidth >= px48 * 3 }
            ?.measure(Constraints.fixed(px48, min(px48, constraints.maxHeight)))

         val stopWidthOffset = if (iconPlaceable != null) px48 * 2 else px48

         val stopPlaceable = measurables[1].measure(
            constraints.copy(
               minWidth = (constraints.minWidth - stopWidthOffset).coerceAtLeast(0),
               maxWidth = (constraints.maxWidth - stopWidthOffset).coerceAtLeast(0),
            )
         )

         val height = max(stopPlaceable.height, px48)

         val canvasPlaceable = measurables[0].measure(Constraints.fixed(px48, height))

         return layout(constraints.maxWidth, height) {
            canvasPlaceable.place(0, 0)
            stopPlaceable.place(px48, (height - stopPlaceable.height) / 2)
            iconPlaceable?.place(constraints.maxWidth - px48, (height - px48) / 2)
         }
      }
   }

}