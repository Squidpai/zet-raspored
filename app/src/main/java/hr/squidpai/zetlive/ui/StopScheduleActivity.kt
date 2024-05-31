package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.LOADING_TEXT
import hr.squidpai.zetlive.gtfs.*
import hr.squidpai.zetlive.timeToString

@OptIn(ExperimentalMaterial3Api::class)
class StopScheduleActivity : ComponentActivity() {

  companion object {
    private const val TAG = "StopScheduleActivity"

    const val EXTRA_STOP = "hr.squidpai.zetlive.extra.STOP"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val stopId = intent.getIntExtra(EXTRA_STOP, StopId.Invalid.value).toStopId()

    if (stopId == StopId.Invalid) {
      Log.w(TAG, "onCreate: No stop id given, finishing activity early.")

      finish()
      return
    }

    setContent {
      AppTheme {
        val schedule = Schedule.instance

        val stop = schedule.stops?.list?.get(stopId)
        val routesAtStop = schedule.routesAtStopMap?.get(stopId.value)

        Scaffold(
          topBar = { MyTopAppBar(stop, routesAtStop) },
        ) { padding ->
          MyContent(
            stop, routesAtStop,
            Modifier
              .fillMaxSize()
              .padding(padding),
          )
        }
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun MyTopAppBar(stop: Stop?, routesAtStop: RoutesAtStop?) = LargeTopAppBar(
    title = {
      if (stop == null) Text(LOADING_TEXT)
      else Column {
        Text(stop.name)
        val label = stop.getLabel(routesAtStop)
        if (label == null) Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Smjer ")

          stop.iconInfo?.let {
            val size = with(LocalDensity.current) {
              LocalTextStyle.current.fontSize.toDp()
            }
            Icon(it.first, it.second, Modifier.size(size))
          }
        } else Text(label)
      }
    },
    navigationIcon = {
      IconButton(Icons.AutoMirrored.Filled.ArrowBack, "Natrag") { finish() }
    },
  )

  @Composable
  private fun MyContent(stop: Stop?, routesAtStop: RoutesAtStop?, modifier: Modifier) {
    val live = routesAtStop?.let {
      stop?.getLiveSchedule(it, keepDeparted = true, maxSize = 40)
    }
      ?: run {
        CircularLoadingBox(modifier)
        return
      }

    val departedColor = lerp(
      MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface, fraction = 0.36f
    )

    val selectTrip = LocalSelectTrip.current

    // TODO add option to switch between child stops here
    // TODO add option to filter specific routes
    // TODO add option to view this stop's whole day schedule (similar to RouteScheduleActivity)

    LazyColumn(
      modifier = modifier,
      state = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = live.indexOfFirst { !it.departed })
      },
    ) {
      items(live.size) {
        val (routeNumber, headsign, stopTime, absoluteTime, relativeTime,
          useRelative, departed) = live[it]

        Row(
          Modifier
            .clickable { selectTrip(stopTime, 0L) }
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val routeStyle = MaterialTheme.typography.titleMedium
          Text(
            text = routeNumber.toString(),
            modifier = Modifier.width(with(LocalDensity.current) { (routeStyle.fontSize * 3.5f).toDp() }),
            color = if (!departed) MaterialTheme.colorScheme.primary else departedColor,
            textAlign = TextAlign.Center,
            style = routeStyle,
          )
          Text(
            headsign,
            Modifier.weight(1f),
            color = if (!departed) Color.Unspecified else departedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            if (departed) "otišao"
            else if (useRelative) "${relativeTime / 60} min"
            else absoluteTime.timeToString(),
            modifier = Modifier.padding(end = 4.dp),
            color = if (!departed) MaterialTheme.colorScheme.primary else departedColor,
            fontWeight = FontWeight.Bold.takeUnless { departed },
          )
        }
      }
    }
  }

}