package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import hr.squidpai.zetlive.gtfs.*
import hr.squidpai.zetlive.none
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import kotlin.math.absoluteValue

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
      AppTheme(statusBarElevation = 3.dp) {
        val schedule = Schedule.instance

        val groupedStop = schedule.stops?.groupedStops?.get(stopId.stationNumber.toParentStopId())

        val routesAtStopMap = schedule.routesAtStopMap

        Scaffold(
          topBar = { MyTopAppBar(groupedStop?.parentStop?.name.orLoading()) },
        ) { padding ->
          MyContent(
            groupedStop, routesAtStopMap,
            defaultCode = stopId.stationCode,
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
  private fun MyTopAppBar(stopName: String) = TopAppBar(
    title = { Text(stopName) },
    navigationIcon = {
      IconButton(Icons.AutoMirrored.Filled.ArrowBack, "Natrag") { finish() }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalStatusBarColor.current)
  )

  @Composable
  private fun MyContent(
    groupedStop: GroupedStop?,
    routesAtStopMap: RoutesAtStopMap?,
    defaultCode: Int,
    modifier: Modifier,
  ) = Column(modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
    if (groupedStop == null || routesAtStopMap == null) {
      CircularLoadingBox()
      return
    }

    val labeledStops = groupedStop.labeledStop(routesAtStopMap)

    val (selectedStopIndex, setSelectedStopIndex) = rememberSaveable {
      val index = labeledStops.indexOfFirst { it.stop.code == defaultCode }
      mutableIntStateOf(if (index != -1) index else 0)
    }

    Text("Postaje", Modifier.padding(start = 8.dp))

    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      items(groupedStop.childStops.size) {
        val (stop, label) = labeledStops[it]
        FilterChip(
          selected = selectedStopIndex == it,
          onClick = { setSelectedStopIndex(it) },
          label = { Text(label ?: "Smjer") },
          modifier = Modifier.padding(horizontal = 4.dp),
          trailingIcon = if (label == null) ({
            stop.iconInfo?.let { iconInfo ->
              Icon(iconInfo.first, iconInfo.second)
            }
          }) else null,
        )
      }
    }

    val selectedStop = labeledStops[selectedStopIndex].stop

    val routesAtStop = routesAtStopMap[selectedStop.id.value]
    if (routesAtStop == null) {
      CircularLoadingBox()
      return
    }

    Text("Linije", Modifier.padding(start = 8.dp))

    val routesFiltered = remember { mutableStateListOf<Int>() }
    val filterEmpty = routesFiltered.isEmpty() || routesAtStop.routes.none { it.absoluteValue in routesFiltered }

    LazyRow(Modifier.fillMaxWidth()) {
      items(routesAtStop.routes.size) { i ->
        val route = routesAtStop.routes[i].absoluteValue
        val selected = route in routesFiltered
        FilterChip(
          selected = selected || filterEmpty,
          onClick = {
            if (selected)
              routesFiltered -= route
            else {
              routesFiltered += route

              // if we've selected all routes, deselect them all
              var containsAll = true
              routesAtStop.routes.forEach { r ->
                if (r.absoluteValue !in routesFiltered) {
                  containsAll = false
                  return@forEach
                }
              }
              if (containsAll)
                routesAtStop.routes.forEach { routesFiltered -= it.absoluteValue }
            }
          },
          label = {
            Text(
              text = route.toString(),
              modifier = Modifier.defaultMinSize(18.dp),
              textAlign = TextAlign.Center,
            )
          },
          modifier = Modifier.padding(horizontal = 6.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = lerp(
              MaterialTheme.colorScheme.primaryContainer,
              MaterialTheme.colorScheme.surface,
              0.2f,
            )
          ),
        )
      }
    }

    val liveSchedule = selectedStop.getLiveSchedule(
      routesAtStop,
      keepDeparted = true,
      maxSize = 40,
      routesFiltered = null //routesFiltered.toList(),
    )
      ?: run {
        CircularLoadingBox()
        return
      }

    Spacer(Modifier.height(4.dp))

    val live =
      if (filterEmpty) liveSchedule.first
      else liveSchedule.first?.filter { it.routeNumber in routesFiltered }

    if (live.isNullOrEmpty()) {
      Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          if (liveSchedule.second != null) liveSchedule.second!!
          else if (filterEmpty) "Nema polazaka na postaji uskoro."
          else "Izabrane linije nemaju nikakvih polazaka uskoro.",
          Modifier.padding(horizontal = 16.dp),
          textAlign = TextAlign.Center,
        )
      }
      return
    }

    val departedColor = lerp(
      MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface, fraction = 0.36f
    )

    val selectTrip = LocalSelectTrip.current

    // TODO add option to view this stop's whole day schedule (similar to RouteScheduleActivity)

    LazyColumn(
      modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxHeight(),
      state = rememberSaveable(selectedStopIndex, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = live.indexOfFirst { !it.departed }.coerceAtLeast(0))
      },
    ) {
      items(live.size) {
        val (
          routeNumber, headsign, stopTime, absoluteTime, relativeTime,
          useRelative, departed,
        ) = live[it]

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