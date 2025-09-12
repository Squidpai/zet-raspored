package hr.squidpai.zetlive.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.intListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import hr.squidpai.zetapi.cached.CachedScheduleIO
import hr.squidpai.zetapi.gtfs.GtfsScheduleLoader
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.ui.composables.LiveTravelSlider
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty0

class SettingsActivity : ComponentActivity() {

    companion object {
        private val sampleStopNames = listOf(
            "", "", "", "", "", "", "Frankopanska", "Trg J. Jelačića", "Zrinjevac",
            "Glavni kolodvor", "Branimirova", "Branim. tržnica",
        )

        private val sampleDepartures = intListOf(
            0, 142, 237, 298, 431, 513, 618, 825, 969, 1078, 1177, 1377, 1478, 1583, 1652,
            1746, 1806, 1905, 1966, 2026, 2106, 2206, 2296,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(topBar = { MyTopAppBar() }) { padding ->
                    Content(Modifier.padding(padding))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MyTopAppBar() = TopAppBar(
        title = { Text("Postavke") },
        navigationIcon = {
            IconButton(
                Symbols.ArrowBack,
                contentDescription = "Natrag",
                onClick = { finish() },
            )
        },
        windowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )

    @Composable
    private fun Content(modifier: Modifier) = Column(modifier) {
        SimpleSwitchEntry(
            text = "Koristi pune nazive linija",
            hint = "Npr. „Zagreb (Glavni kolodvor) – Velika Gorica“ umjesto „Zag.(Gl.k.)-V.Gorica“",
            property = Data::useFullRouteNames,
        )
        SimpleSwitchEntry(
            text = "Koristi pune nazive postaja",
            hint = "Npr. „Trg bana Josipa Jelačića“ umjesto „Trg J. Jelačića“",
            property = Data::useFullStopNames,
        )
        SimpleSwitchEntry(
            text = "Koristi pune nazive smjerova",
            hint = "Npr. „Garaža Tuškanac ⇄ Trg bana Josipa Jelačića“ umjesto „G. Tuškanac ⇄ T.b.J.Jelačića“",
            property = Data::useFullHeadsigns,
        )
        HighlightedStationEntry()
        ScheduleInfoEntry()
    }

    private val semiTransparentColor
        @Composable get() = lerp(
            LocalContentColor.current,
            MaterialTheme.colorScheme.background,
            .20f
        )

    @Composable
    private fun SimpleSwitchEntry(
        text: String, hint: String, property: KMutableProperty0<Boolean>,
    ) = Row(
        modifier = Modifier.defaultEntryModifiers(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.titleLarge)
            Text(hint, color = semiTransparentColor, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = property.get(),
            onCheckedChange = { Data.updateData { property.set(it) } },
        )
    }

    @Composable
    private fun HighlightedStationEntry() {
        var showDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .clickable { showDialog = true }
                .defaultEntryModifiers()
        ) {
            Text("Istaknuta postaja", style = MaterialTheme.typography.titleLarge)
            Text(
                if (Data.highlightNextStop) "Podebljaj sljedeću postaju"
                else "Podebljaj trenutnu postaju",
                color = semiTransparentColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (showDialog) AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Istaknuta postaja") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !Data.highlightNextStop,
                            onClick = { Data.updateData { highlightNextStop = false } },
                        )
                        Text("Podebljaj trenutnu postaju")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = Data.highlightNextStop,
                            onClick = { Data.updateData { highlightNextStop = true } },
                        )
                        Text("Podebljaj sljedeću postaju")
                    }
                    Text("Primjer", style = MaterialTheme.typography.labelLarge)

                    LiveTravelSlider(
                        nextStopIndex = 7,
                        sliderValue = 6.25f,
                        stopNames = sampleStopNames,
                        departures = sampleDepartures,
                        interactable = false,
                    )
                }
            }
        )
    }

    @Composable
    private fun ScheduleInfoEntry() {
        val scope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }

        val schedule = ScheduleManager.instance.collectAsState().value
        val feedInfo = schedule?.feedInfo

        Column(
            modifier = Modifier
                .clickable { showDialog = true }
                .defaultEntryModifiers()
        ) {
            Text(
                "Informacije o rasporedu",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                if (feedInfo != null) "Verzija: ${feedInfo.version}"
                else "Nema preuzetog rasporeda",
                color = semiTransparentColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (showDialog) AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Zatvori")
                }
            },
            title = { Text("Informacije o rasporedu") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (schedule != null) {
                        val latestVersion = ScheduleManager.lastCheckedLatestVersion
                        val startDate = feedInfo?.startDate
                        val lastDate = schedule.calendarDates.lastDate
                        val newScheduleFeedInfo = CachedScheduleIO.getFeedInfoOrNull(
                            ScheduleManager.getNewScheduleFile(filesDir)
                        ) ?: GtfsScheduleLoader.getFeedInfoOrNull(
                            ScheduleManager.getNewDownloadedFile(filesDir)
                        )
                        Text(
                            buildString {
                                append("Verzija: ").append(feedInfo?.version.orLoading())
                                if (latestVersion != null)
                                    append("\nNajnovija verzija: ").append(latestVersion)
                                append("\nPočetak rasporeda: ").append(
                                    startDate?.toString().orLoading()
                                )
                                append("\nKraj rasporeda: ").append(
                                    lastDate.toString().orLoading()
                                )
                                if (newScheduleFeedInfo != null) {
                                    append("\n\nPostoji neaktivirani raspored:")
                                    append("\n Verzija: ").append(newScheduleFeedInfo.version)
                                    append("\n Početak: ").append(newScheduleFeedInfo.startDate)
                                }
                            }
                        )
                    } else Text("Nema preuzetog rasporeda")

                    val (isCheckingUpdate, setIsCheckingUpdate) = remember {
                        mutableStateOf(false)
                    }
                    val (isUpdating, setIsUpdating) = remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                setIsCheckingUpdate(true)
                                errorMessage = null
                                ScheduleManager.update(filesDir).join()
                                val errorType = ScheduleManager.lastDownloadError.value
                                errorMessage = errorType?.errorMessage
                                setIsCheckingUpdate(false)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = !isCheckingUpdate && !isUpdating,
                    ) {
                        Text(
                            when {
                                isUpdating -> "Ažuriranje${Typography.ellipsis}"
                                isCheckingUpdate -> "Provjeravanje${Typography.ellipsis}"
                                else -> "Provjeri ažuriranje"
                            }
                        )
                    }
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    UpdateState(setIsUpdating)
                }
            }
        )
    }

    @Composable
    private fun UpdateState(setIsUpdating: (Boolean) -> Unit) {
        val loadingState = ScheduleManager.downloadState.collectAsState().value
        if (loadingState == null) {
            setIsUpdating(false)
            return
        }
        setIsUpdating(true)
        // TODO check if Schedule exists and change this text then
        Text(
            if (loadingState.operation == ScheduleManager.DownloadOperation.DOWNLOADING)
                "Preuzimanje novog rasporeda${Typography.ellipsis}"
            else "Ažuriranje rasporeda${Typography.ellipsis}"
        )
        if (loadingState.progress.isNaN())
            LinearProgressIndicator()
        else
            LinearProgressIndicator(progress = { loadingState.progress })
    }

    /*@Composable
    private fun LabelText(text: String) = Text(
       text = text,
       modifier = Modifier
          .windowInsetsPadding(
             WindowInsets.safeContent.only(WindowInsetsSides.Horizontal)
          )
          .padding(top = 8.dp),
       style = MaterialTheme.typography.labelLarge,
    )*/

    @Composable
    @NonRestartableComposable
    private fun Modifier.defaultEntryModifiers() = this
        .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Horizontal))
        .padding(vertical = 8.dp)
        .fillMaxWidth()
        .minimumInteractiveComponentSize()

}