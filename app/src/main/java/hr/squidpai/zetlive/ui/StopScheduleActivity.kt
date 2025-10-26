package hr.squidpai.zetlive.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.StopId
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.asStopId
import hr.squidpai.zetlive.gtfs.ActualStopLiveSchedule
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.gtfs.StopNoLiveSchedule
import hr.squidpai.zetlive.gtfs.getUpdatingLiveSchedule
import hr.squidpai.zetlive.gtfs.iconInfo
import hr.squidpai.zetlive.gtfs.preferredName
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.ui.composables.IconButton

class StopScheduleActivity : BaseAppActivity("StopScheduleActivity") {

    private var stopId = StopId.Invalid

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted)
            startNotificationTracking()
        else
            Toast.makeText(
                this,
                "Ne može se postaviti obavijest${Typography.mdash}" +
                        "odbijena je dozvola za postavljanje obavijesti.",
                Toast.LENGTH_LONG
            ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stopId = intent.getIntExtra(EXTRA_STOP, StopId.Invalid.rawValue)
            .asStopId()

        if (stopId.isInvalid()) {
            Log.w(TAG, "onCreate: No stop id given, finishing activity early.")

            finish()
            return
        }
    }

    private fun trackInNotifications() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startNotificationTracking()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startNotificationTracking() {
        startForegroundService(
            Intent(this, NotificationTrackerService::class.java)
                .putExtra(EXTRA_STOP, stopId.rawValue)
        )
    }

    @Composable
    override fun Content() {
        val schedule = ScheduleManager.instance.collectAsState().value

        val groupedStop = schedule?.stops?.groupedStops?.get(stopId.stopNumber)

        Scaffold(
            topBar = { MyTopAppBar(groupedStop?.parentStop?.preferredName.orLoading()) },
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MyTopAppBar(stopName: String) = TopAppBar(
        title = { Text(stopName) },
        navigationIcon = {
            IconButton(
                Symbols.ArrowBack,
                contentDescription = "Natrag",
                onClick = { finish() },
            )
        },
        actions = {
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
                        onClick = {
                            trackInNotifications()
                            expanded = false
                        },
                    )
                }
            }
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
            CircularProgressIndicator(
                Modifier
                    .fillMaxSize()
                    .wrapContentSize()
            )
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
            state = rememberSaveable(saver = LazyListState.Saver) {
                val firstVisibleItem = (selectedStopIndex - 2).coerceAtLeast(0)
                LazyListState(
                    firstVisibleItemIndex = firstVisibleItem,
                    firstVisibleItemScrollOffset = if (firstVisibleItem > 0) 32 else 0,
                )
            },
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items(groupedStop.size) {
                val (stop, label) = labeledStops[it]
                FilterChip(
                    selected = selectedStopIndex == it,
                    onClick = {
                        setSelectedStopIndex(it)
                        stopId = stop.id
                    },
                    label = { Text(label ?: stop.iconInfo?.second ?: "Smjer") },
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

        val liveSchedule = selectedStop.getUpdatingLiveSchedule(
            keepDeparted = true,
            maxSize = 40,
            routesFiltered = routesFiltered.toList(),
        ) ?: run {
            CircularProgressIndicator(
                Modifier
                    .fillMaxSize()
                    .wrapContentSize()
            )
            return
        }

        Spacer(Modifier.height(4.dp))

        when (liveSchedule) {
            is StopNoLiveSchedule -> NoLiveSchedule(liveSchedule)
            is ActualStopLiveSchedule -> ActualLiveSchedule(selectedStop, liveSchedule)
        }
    }

    @Composable
    private fun NoLiveSchedule(liveSchedule: StopNoLiveSchedule) = Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            liveSchedule.noLiveMessage,
            Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )
    }

    @Composable
    private fun ActualLiveSchedule(stop: Stop, liveSchedule: ActualStopLiveSchedule) {
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
                LiveStopRow(
                    stop,
                    entry,
                    Modifier
                        .clickable { showTripDialog(entry.trip, entry.selectedDate) }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }

}