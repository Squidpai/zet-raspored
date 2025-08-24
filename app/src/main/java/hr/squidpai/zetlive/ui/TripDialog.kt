package hr.squidpai.zetlive.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetlive.gtfs.getUpdatingLiveDisplayData
import hr.squidpai.zetlive.gtfs.preferredHeadsign
import hr.squidpai.zetlive.gtfs.preferredName
import hr.squidpai.zetlive.localEpochDate
import hr.squidpai.zetlive.ui.composables.HintIconButton
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.ui.composables.disabled
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

data class TripDialogData(
   val trip: Trip,
   val selectedDate: Long = localEpochDate(),
)

fun showTripDialog(context: Context, data: TripDialogData) {
   (context as? BaseAppActivity)?.showTripDialog(data)
}

fun showTripDialog(context: Context, trip: Trip, selectedDate: Long) =
   showTripDialog(context, TripDialogData(trip, selectedDate))

@Composable
fun TripDialog(
   onDismissRequest: () -> Unit,
   onStopClicked: (Stop) -> Unit,
   onTrackInNotificationsRequest: () -> Unit,
   data: TripDialogData,
) {
   var isAbsoluteTime by remember { mutableStateOf(true) }

   AlertDialog(
      onDismissRequest = onDismissRequest,
      confirmButton = {
         TextButton(onClick = onDismissRequest) {
            Text("Zatvori")
         }
      },
      title = {
         Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
               text = "${data.trip.route.shortName} smjer ${data.trip.preferredHeadsign}",
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
                  Symbols.MoreVert, "Dodatne opcije",
                  onClick = { expanded = true },
               )
               DropdownMenu(
                  expanded = expanded,
                  onDismissRequest = { expanded = false }) {
                  DropdownMenuItem(
                     text = { Text("Prati u obavijestima") },
                     onClick = onTrackInNotificationsRequest,
                  )
               }
            }
         }
      },
      text = {
         DialogContent(data, onStopClicked, isAbsoluteTime)
      }
   )
}

@Composable
private fun DialogContent(
   data: TripDialogData,
   onStopClicked: (Stop) -> Unit,
   isAbsoluteTime: Boolean,
) {
   val (realtimeDepartures, timeOfDay, nextStopIndex, nextStopValue) =
      getUpdatingLiveDisplayData(data.trip, data.selectedDate)
   val isCancelled = realtimeDepartures == null

   LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      state = rememberLazyListState(
         (nextStopIndex - 4).coerceAtLeast(0)
      ),
   ) {
      items(data.trip.stops.size) { i ->
         val stop = data.trip.stops[i]
         val departure = TimeOfDay(data.trip.departures[i])
         val realtimeDeparture = realtimeDepartures?.get(i)
            ?.let { TimeOfDay(it) } ?: departure

         Layout(
            content = {
               LineCanvas(
                  index = i,
                  nextStopIndex,
                  nextStopValue,
                  isCancelled,
                  lastIndex = data.trip.stops.lastIndex,
               )

               Column {
                  val passed = i < nextStopIndex

                  val stopColor: Color
                  val timeColor: Color
                  if (passed || isCancelled) {
                     stopColor = MaterialTheme.colorScheme.disabled
                     timeColor = stopColor
                  } else {
                     stopColor = Color.Unspecified
                     timeColor = MaterialTheme.colorScheme.primary
                  }
                  Text(stop.preferredName, color = stopColor)
                  Text(
                     buildAnnotatedString {
                        val t = realtimeDeparture.minusSeconds(timeOfDay)
                        if (isCancelled) {
                           withStyle(
                              SpanStyle(
                                 textDecoration = TextDecoration.LineThrough,
                                 fontWeight = FontWeight.Normal
                              )
                           ) {
                              append(departure.toStringHHMM())
                           }
                           append(" otkazano")
                        } else if (!isAbsoluteTime) {
                           append(if (t < 0) "prije" else "za")
                           val timeUntilDeparture = t.absoluteValue.seconds
                           timeUntilDeparture.toComponents { days, hours, minutes, _, _ ->
                              if (days != 0L) {
                                 append(' ')
                                  if (days == 1L)
                                      append("1 dan")
                                  else {
                                      append(days.toString())
                                      append(" dana")
                                  }
                              }
                              if (hours != 0) {
                                 append(' ')
                                 append(hours.toString())
                                 append(" hr")
                              }
                              if (minutes > 0 || days == 0L && hours == 0) {
                                 append(' ')
                                 append(minutes.toString())
                                 append(" min")
                              }
                           }
                        } else {
                           if (realtimeDeparture.minusMinutes(departure) != 0) {
                              withStyle(
                                 SpanStyle(
                                    textDecoration = TextDecoration.LineThrough,
                                    fontWeight = FontWeight.Normal
                                 )
                              ) {
                                 append(departure.toStringHHMM())
                              }
                              append(' ')
                              append(realtimeDeparture.toStringHHMM())
                           } else append(departure.toStringHHMM())
                        }
                     },
                     color = timeColor,
                     fontWeight = FontWeight.Bold,
                     style = MaterialTheme.typography.bodySmall,
                  )
               }

               if (realtimeDepartures != null &&
                  data.trip.departures !== realtimeDepartures &&
                  i == (nextStopIndex - 1).coerceAtLeast(0)
               ) HintIconButton(
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
               .clickable { onStopClicked(stop) },
            measurePolicy = TripRowMeasurePolicy,
         )
      }
   }
}

@Composable
private fun LineCanvas(
   index: Int,
   nextStopIndex: Int,
   nextStopValue: Float,
   isCancelled: Boolean,
   lastIndex: Int,
) {
   val notFilled: Color
   val filled: Color

   if (isCancelled) {
      notFilled = MaterialTheme.colorScheme.disabled
      filled = notFilled
   } else {
      notFilled = MaterialTheme.colorScheme.onSurface
      filled = MaterialTheme.colorScheme.primary
   }

   val fillCircle = index < nextStopIndex || index == 0 && !isCancelled
   val circleTint = if (fillCircle) filled else notFilled

   Canvas(Modifier) {
      val (width, height) = size
      if (index != 0) {
         val prefillRatio =
            ((nextStopValue - (index - 0.5f)) * 2f).coerceIn(0f, 1f)
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
      if (index != lastIndex) {
         val prefillRatio =
            ((nextStopValue - index) * 2f).coerceIn(0f, 1f)
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
