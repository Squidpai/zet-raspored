package hr.squidpai.zetlive.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.*
import hr.squidpai.zetlive.Data.TripTimeType.*
import hr.squidpai.zetlive.gtfs.*
import kotlin.math.max

@Suppress("unused")
private const val TAG = "TripDialog"

@Composable
fun TripDialog(
  onDismissRequest: () -> Unit,
  trip: Trip,
  timeOffset: Long,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  TripDialog(
    onDismissRequest, trip, timeOffset,
    onStopSelected = {
      context.startActivity(
        Intent(context, StopScheduleActivity::class.java)
          .putExtra(StopScheduleActivity.EXTRA_STOP, it.id.value)
      )
    }, modifier
  )
}

@Composable
fun TripDialog(
  onDismissRequest: () -> Unit,
  trip: Trip,
  timeOffset: Long,
  onStopSelected: (Stop) -> Unit,
  modifier: Modifier = Modifier,
) = AlertDialog(
  onDismissRequest = onDismissRequest,
  confirmButton = {
    TextButton(onClick = onDismissRequest) {
      Text("Zatvori")
    }
  },
  modifier = modifier,
  title = {
    val schedule = Schedule.instance

    val route = schedule.routes?.list?.get(key = trip.routeId)

    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        if (route != null) "${route.shortName} smjer ${trip.headsign}" else LOADING_TEXT,
        Modifier.weight(1f),
      )

      val tripTimeType = Data.tripTimeType
      val (icon, contentDescription) = when (tripTimeType.next) {
        Default -> Symbols.TimeAuto to "Pokaži vrijeme uobičajeno"
        Absolute -> Symbols.Schedule to "Pokaži uvijek u koliko sati dolazi vozilo"
        Relative -> Symbols.ClockLoader10 to "Pokaži uvijek za koliko minuta dolazi vozilo"
      }
      IconButton(icon, contentDescription) {
        Data.updateData { this.tripTimeType = tripTimeType.next }
      }
    }

    // TODO add option for tracking a route's travel
  },
  text = text@{
    val stops = Schedule.instance.stops

    if (stops == null) {
      CircularProgressIndicator()
      return@text
    }

    val stopTimeUpdate = trip.tripId.let { id ->
      Live.instance.findForTrip(id)?.tripUpdate?.stopTimeUpdateList
    }

    //val firstUpdate = stopTimeUpdate?.fastMinByOrNull { it.stopSequence }

    val time = if (timeOffset != 0L) localCurrentTimeMillis() else localEpochTime().toLong()
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
      else -> nextStopIndex + getArrivalLineRatio(trip.departures, nextStopIndex, delays, timeOfDay)
    }

    val tripTimeType = Data.tripTimeType

    LazyColumn(
      modifier = modifier.fillMaxWidth(),
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
                val prefillRatio = ((nextStopValue - (it + 1 - 0.5f)) * 2f).coerceIn(0f, 1f)
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
                val prefillRatio = ((nextStopValue - (it + 1)) * 2f).coerceIn(0f, 1f)
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
                MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface, fraction = .36f
              ),
            ) else Column {
              Text(stopName)
              Text(
                buildAnnotatedString {
                  val t = offsetDeparture + delay - time / 1000
                  if (tripTimeType != Absolute && (tripTimeType == Relative || t < 3600)) {
                    if (t < 60)
                      append("za 0 min")
                    else {
                      append("za ")
                      if (t >= 3600) {
                        append((t / 3600).toString())
                        append(" hr ")
                      }
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
                    }
                    else append(departure.timeToString())
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
              if (stop != null) onStopSelected(stop)
            },
          measurePolicy = TripRowMeasurePolicy,
        )
      }
    }
  }
)

private data object TripRowMeasurePolicy : MeasurePolicy {
  override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
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
