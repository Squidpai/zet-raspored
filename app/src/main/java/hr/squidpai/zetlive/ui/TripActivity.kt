package hr.squidpai.zetlive.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMinByOrNull
import hr.squidpai.zetlive.*
import hr.squidpai.zetlive.gtfs.*

/**
 * Does the same thing as [TripDialog], except in Activity form.
 *
 * Kept here in case I ever change my mind about using it.
 */
@Deprecated("Use TripDialog instead.")
class TripActivity : ComponentActivity() {

  companion object {
    private const val TAG = "TripActivity"

    const val EXTRA_TRIP = "hr.squidpai.zetlive.extra.TRIP"
    const val EXTRA_DATE = "hr.squidpai.zetlive.extra.DATE"
  }

  private lateinit var tripId: String
  private var timeOffset = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val tripId = intent.getStringExtra(EXTRA_TRIP)
    timeOffset = intent.getLongExtra(EXTRA_DATE, 0L) * MILLIS_IN_DAY

    if (tripId == null) {
      Log.w(TAG, "No trip id given, finishing activity early.")

      finish()
      return
    }
    this.tripId = tripId

    setContent {
      AppTheme {
        val schedule = Schedule.instance

        val routeId = ZippedTrip.getRouteIdFromTripId(tripId)
        val route = schedule.routes?.list?.get(key = routeId)
        val stopTime = schedule.getTripsOfRoute(routeId).value?.list?.get(tripId)
        val stops = schedule.stops

        Scaffold(topBar = { MyTopAppBar(route?.shortName, stopTime?.headsign) }) { padding ->
          if (stopTime != null && stops != null)
            TripContent(stopTime, stops, Modifier.padding(padding))
          else
            CircularLoadingBox(Modifier.padding(padding))
        }
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

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun MyTopAppBar(routeShortName: String?, headsign: String?) = TopAppBar(
    title = {
      Text(
        if (routeShortName != null && headsign != null)
          "$routeShortName smjer $headsign" else LOADING_TEXT
      )
    },
    navigationIcon = {
      IconButton(onClick = { finish() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Natrag")
      }
    }
  )

  @Composable
  private fun TripContent(
    trip: Trip,
    stops: Stops,
    modifier: Modifier = Modifier,
  ) {
    val stopTimeUpdate = tripId.let { id ->
      Live.instance.findForTrip(id)?.tripUpdate?.stopTimeUpdateList
    }

    Log.d(TAG, "TripContent: tripId = $tripId stopTimeUpdate = $stopTimeUpdate")

    val firstUpdate = stopTimeUpdate?.fastMinByOrNull { it.stopSequence }

    val time = localCurrentTimeMillis()

    val liveStopId = firstUpdate?.stopId?.toStopId()

    val delays = stopTimeUpdate.getDelayByStop()

    val nextStopIndex = run nextStopIndex@{
      trip.departures.forEachIndexed { index, departure ->
        if (time < timeOffset + (delays[index] + departure) * MILLIS_IN_SECONDS) {
          return@nextStopIndex index
        }
      }
      return@nextStopIndex trip.stops.size
    }

    Log.d(TAG, "TripContent: liveStopId = $liveStopId nextStopIndex = $nextStopIndex delays = $delays")

    LazyColumn(
      modifier.fillMaxWidth(), rememberLazyListState(
        (nextStopIndex - 4).coerceAtLeast(0)
      )
    ) {
      items(trip.stops.size) {
        val stop = stops.list[trip.stops[it].toStopId()]
        val departure = trip.departures[it]
        val offsetDeparture = departure + timeOffset / MILLIS_IN_SECONDS
        val delay = delays[it]

        Row(
          modifier = Modifier.clickable(enabled = stop != null) {
            if (stop != null) startActivity(Intent(this@TripActivity, StopScheduleActivity::class.java)
              .apply { putExtra(StopScheduleActivity.EXTRA_STOP, stop.id.value) })
          },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val firstLineTint =
            if (it <= nextStopIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

          val fillCircle = it < nextStopIndex || it == 0
          val circleTint =
            if (fillCircle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

          val lastLineTint =
            if (it < nextStopIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

          Canvas(Modifier.size(48.dp)) {
            val size = size.width // or height, they are equal
            if (it != 0) drawLine(
              color = firstLineTint,
              start = Offset(size / 2, 0f),
              end = Offset(size / 2, size * 1 / 3),
              strokeWidth = size / 16,
            )
            drawCircle(
              color = circleTint,
              radius = size / 6,
              style = if (fillCircle) Fill else Stroke(width = size / 16),
            )
            if (it != trip.stops.lastIndex) drawLine(
              color = lastLineTint,
              start = Offset(size / 2, size * 2 / 3),
              end = Offset(size / 2, size),
              strokeWidth = size / 16,
            )
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
}