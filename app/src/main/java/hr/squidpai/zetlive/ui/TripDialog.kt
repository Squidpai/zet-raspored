package hr.squidpai.zetlive.ui

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.*
import hr.squidpai.zetlive.gtfs.*

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

    Text(if (route != null) "${route.shortName} smjer ${trip.headsign}" else LOADING_TEXT)

    // TODO add option for toggling between relative and absolute time
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

    LazyColumn(
      modifier = modifier.fillMaxWidth(),
      state = rememberLazyListState((nextStopIndex - 4).coerceAtLeast(0))
    ) {
      items(trip.stops.size) {
        val stop = stops.list[trip.stops[it].toStopId()]
        val departure = trip.departures[it]
        val offsetDeparture = departure + timeOffset / MILLIS_IN_SECONDS
        val delay = delays[it]

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = stop != null) {
              if (stop != null) onStopSelected(stop)
            },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val filled = MaterialTheme.colorScheme.primary
          val notFilled = MaterialTheme.colorScheme.onSurface

          val fillCircle = it < nextStopIndex || it == 0
          val circleTint = if (fillCircle) filled else notFilled

          Canvas(Modifier.size(48.dp)) {
            val size = size.width // or height, they are equal
            if (it != 0) {
              val prefillRatio = ((nextStopValue - (it + 1 - 0.5f)) * 2f).coerceIn(0f, 1f)
              val prefillLength = prefillRatio * size * 1 / 3
              if (prefillRatio != 0f) drawLine(
                color = filled,
                start = Offset(size / 2, 0f),
                end = Offset(size / 2, prefillLength),
                strokeWidth = size / 16,
              )
              if (prefillRatio != 1f) drawLine(
                color = notFilled,
                start = Offset(size / 2, prefillLength),
                end = Offset(size / 2, size * 1 / 3),
                strokeWidth = size / 16,
              )
            }
            drawCircle(
              color = circleTint,
              radius = size / 6,
              style = if (fillCircle) Fill else Stroke(width = size / 16),
            )
            if (it != trip.stops.lastIndex) {
              val prefillRatio = ((nextStopValue - (it + 1)) * 2f).coerceIn(0f, 1f)
              val prefillLength = size * prefillRatio * 1 / 3
              if (prefillRatio != 0f) drawLine(
                color = filled,
                start = Offset(size / 2, size * 2 / 3),
                end = Offset(size / 2, size * 2 / 3 + prefillLength),
                strokeWidth = size / 16,
              )
              if (prefillRatio != 1f) drawLine(
                color = notFilled,
                start = Offset(size / 2, size * 2 / 3 + prefillLength),
                end = Offset(size / 2, size),
                strokeWidth = size / 16,
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
                if (stopTimeUpdate != null) {
                  val t = offsetDeparture + delay - time / 1000
                  when {
                    t < 60 -> append("za 0 min")
                    t < 3600 -> {
                      append("za ")
                      append((t / 60).toString())
                      append(" min")
                    }

                    offsetDeparture / 60 != (offsetDeparture + delay) / 60 -> {
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

                    else -> append(departure.timeToString())
                  }
                } else {
                  append(departure.timeToString())
                }
              },
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }
  }
)