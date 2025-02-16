package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.StopId
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.asStopId
import hr.squidpai.zetlive.gtfs.ActualStopLiveSchedule
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.gtfs.StopNoLiveSchedule
import hr.squidpai.zetlive.gtfs.getLiveSchedule
import hr.squidpai.zetlive.gtfs.iconInfo
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.ui.composables.CircularLoadingBox
import hr.squidpai.zetlive.ui.composables.IconButton

class StopScheduleActivity : ComponentActivity() {

   companion object {
      private const val TAG = "StopScheduleActivity"

      const val EXTRA_STOP = "hr.squidpai.zetlive.extra.STOP"
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      val stopId = intent.getIntExtra(EXTRA_STOP, StopId.Invalid.rawValue)
         .asStopId()

      if (stopId == StopId.Invalid) {
         Log.w(TAG, "onCreate: No stop id given, finishing activity early.")

         finish()
         return
      }

      enableEdgeToEdge()
      setContent {
         AppTheme {
            val schedule = ScheduleManager.instance.collectAsState().value

            val groupedStop = schedule?.stops?.groupedStops?.get(stopId.stopNumber)

            Scaffold(
               topBar = { MyTopAppBar(groupedStop?.parentStop?.name.orLoading()) },
               contentWindowInsets = WindowInsets.safeDrawing,
            ) { padding ->
               MyContent(
                  groupedStop,
                  defaultCode = stopId.stopCode,
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
         IconButton(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Natrag",
            onClick = { finish() },
         )
      },
      colors = TopAppBarDefaults.topAppBarColors(
         containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
      ),
      windowInsets = WindowInsets.safeDrawing
         .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
   )

   @Composable
   private fun MyContent(
      groupedStop: Stops.Grouped?,
      defaultCode: Int,
      modifier: Modifier,
   ) = Column(modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
      if (groupedStop == null) {
         CircularLoadingBox()
         return
      }

      val labeledStops = groupedStop.labeledStop()

      val (selectedStopIndex, setSelectedStopIndex) = rememberSaveable {
         val index = labeledStops.indexOfFirst { it.stop.code == defaultCode }
         mutableIntStateOf(if (index != -1) index else 0)
      }

      Text("Postaje", Modifier.padding(start = 8.dp))

      LazyRow(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
         items(groupedStop.size) {
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

      val routesAtStop = remember(selectedStop) {
         selectedStop.routes.keys.sortedBy { it.sortOrder }
      }

      Text("Linije", Modifier.padding(start = 8.dp))

      val routesFiltered = remember { mutableStateListOf<RouteId>() }
      val filterEmpty = routesFiltered.isEmpty() ||
            routesAtStop.none { it.id in routesFiltered }

      LazyRow(Modifier.fillMaxWidth()) {
         items(routesAtStop.size) { i ->
            val route = routesAtStop[i]
            val selected = route.id in routesFiltered
            FilterChip(
               selected = selected || filterEmpty,
               onClick = {
                  if (selected)
                     routesFiltered -= route.id
                  else {
                     routesFiltered += route.id

                     // if we've selected all routes, deselect them all
                     val containsAll = routesAtStop.all {
                        it.id in routesFiltered
                     }
                     if (containsAll)
                        routesAtStop.forEach { routesFiltered -= it.id }
                  }
               },
               label = {
                  Text(
                     text = route.shortName,
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
         keepDeparted = true,
         maxSize = 40,
         routesFiltered = routesFiltered.toList(),
      ) ?: run {
         CircularLoadingBox()
         return
      }

      Spacer(Modifier.height(4.dp))

      when (liveSchedule) {
         is StopNoLiveSchedule -> NoLiveSchedule(liveSchedule, filterEmpty)
         is ActualStopLiveSchedule -> ActualLiveSchedule(
            liveSchedule,
            selectedStopIndex
         )
      }
   }

   @Composable
   private fun NoLiveSchedule(
      liveSchedule: StopNoLiveSchedule,
      filterEmpty: Boolean,
   ) = Box(
      Modifier
         .fillMaxSize()
         .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center,
   ) {
      Text(
         liveSchedule.noLiveMessage
            ?: if (filterEmpty) "Nema polazaka na postaji uskoro."
            else "Izabrane linije nemaju nikakvih polazaka uskoro.",
         Modifier.padding(horizontal = 16.dp),
         textAlign = TextAlign.Center,
      )
   }

   @Composable
   private fun ActualLiveSchedule(
      liveSchedule: ActualStopLiveSchedule,
      selectedStopIndex: Int,
   ) {
      val departedColor = lerp(
         MaterialTheme.colorScheme.onSurface,
         MaterialTheme.colorScheme.surface,
         fraction = 0.36f,
      )

      // TODO add option to view this stop's whole day schedule (similar to RouteScheduleActivity)

      LazyColumn(
         modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxHeight(),
         state = rememberSaveable(
            liveSchedule.key,
            saver = LazyListState.Saver,
         ) {
            LazyListState(
               firstVisibleItemIndex = liveSchedule
                  .indexOfFirst { it.relativeTime >= 0 }
                  .coerceAtLeast(0)
            )
         },
      ) {
         items(liveSchedule.size) {
            val entry = liveSchedule[it]
            val departed = entry.relativeTime < 0

            Row(
               Modifier
                  .clickable {
                     TripDialogActivity.show(
                        this@StopScheduleActivity,
                        entry.trip,
                        entry.selectedDate,
                     )
                  }
                  .padding(vertical = 8.dp),
               verticalAlignment = Alignment.CenterVertically,
            ) {
               val routeStyle = MaterialTheme.typography.titleMedium
               Text(
                  text = entry.route.id,
                  modifier = Modifier.width(with(LocalDensity.current) {
                     (routeStyle.fontSize * 3.5f).toDp()
                  }),
                  color = if (departed) departedColor
                  else MaterialTheme.colorScheme.primary,
                  textAlign = TextAlign.Center,
                  style = routeStyle,
               )
               Text(
                  entry.headsign,
                  Modifier.weight(1f),
                  color = if (departed) departedColor else Color.Unspecified,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
               )
               Text(
                  if (entry.useRelative) {
                     if (!departed) "${entry.relativeTime / 60} min"
                     else "prije ${-entry.relativeTime / 60} min"
                  } else entry.absoluteTime.toStringHHMM(),
                  modifier = Modifier.padding(end = 4.dp),
                  color = if (departed) departedColor
                  else MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold.takeUnless { departed },
               )
            }
         }
      }
   }

}