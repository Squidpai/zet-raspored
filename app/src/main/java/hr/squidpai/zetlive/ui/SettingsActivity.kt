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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.gtfs.LoadedSchedule
import hr.squidpai.zetlive.gtfs.RouteScheduleEntry
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.Stop
import hr.squidpai.zetlive.gtfs.StopId
import hr.squidpai.zetlive.gtfs.Trip
import hr.squidpai.zetlive.gtfs.feedInfo
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.toSortedListMap
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.ui.composables.LiveTravelSlider
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

   companion object {
      val sampleRouteScheduleEntry = RouteScheduleEntry(
         nextStopIndex = 7,
         sliderValue = 6.25f,
         trip = Trip(
            routeId = 6,
            serviceId = "0_41",
            tripId = "0_41_601_6_10247",
            headsign = "Sopot",
            directionId = 0,
            blockId = 601,
            stops = intListOf(
               6422529, 6553601, 6619137, 6684673, 6750209, 11075585, 6881281, 6946817, 7012356,
               7143425, 7274497, 7340033, 7405572, 7471108, 7536644, 11206660, 113311748, 7667716,
               12124164, 12124162, 11993090, 11862018, 117571596
            ),
            departures = intListOf(
               22511, 22653, 22748, 22809, 22942, 23024, 23129, 23336, 23480, 23589, 23688, 23888,
               23989, 24094, 24163, 24257, 24317, 24416, 24477, 24537, 24617, 24717, 24807
            ),
            tripShape = 2,
         ),
         headsign = "",
         isHeadsignCommon = true,
         overriddenFirstStop = StopId.Invalid,
         departureTime = -1,
         delayAmount = 0,
         selectedDate = 0,
      )

      val sampleStops =
         listOf(
            /*Stop(6422529.toStopId(), 1, "Črnomerec", 45.815002f, 15.934932f, 98),
            Stop(6553601.toStopId(), 1, "Sveti Duh", 45.813297f, 15.943123f, 100),
            Stop(6619137.toStopId(), 1, "Mandaličina", 45.812428f, 15.948326f, 101),
            Stop(6684673.toStopId(), 1, "Slovenska", 45.812458f, 15.951965f, 102),
            Stop(6750209.toStopId(), 1, "Trg dr. F. Tuđmana", 45.81311f, 15.956823f, 103),
            Stop(11075585.toStopId(), 1, "Britanski trg", 45.81233f, 15.963088f, 169),*/
            Stop(6881281.toStopId(), 1, "Frankopanska", 45.813465f, 15.969438f, 105),
            Stop(6946817.toStopId(), 1, "Trg J. Jelačića", 45.812904f, 15.977312f, 106),
            Stop(7012356.toStopId(), 4, "Zrinjevac", 45.808884f, 15.977743f, 107),
            Stop(7143425.toStopId(), 1, "Glavni kolodvor", 45.805214f, 15.979278f, 109),
            Stop(7274497.toStopId(), 1, "Branimirova", 45.805443f, 15.983765f, 111),
            Stop(7340033.toStopId(), 1, "Branim. tržnica", 45.80614f, 15.99198f, 112),
            /*Stop(7405572.toStopId(), 4, "Autobusni kol.", 45.803726f, 15.993918f, 113),
            Stop(7471108.toStopId(), 4, "Držićeva", 45.80039f, 15.995899f, 114),
            Stop(7536644.toStopId(), 4, "Slavonska", 45.79687f, 15.998658f, 115),
            Stop(11206660.toStopId(), 4, "Folnegovićevo", 45.79224f, 16.002188f, 171),
            Stop(113311748.toStopId(), 4, "Borovje", 45.788567f, 16.004211f, 1729),
            Stop(7667716.toStopId(), 4, "Most mladosti", 45.78189f, 16.00128f, 117),
            Stop(12124164.toStopId(), 4, "Zapruđe", 45.77896f, 16.00186f, 185),
            Stop(12124162.toStopId(), 2, "Zapruđe", 45.778072f, 15.999797f, 185),
            Stop(11993090.toStopId(), 2, "Utrina", 45.777977f, 15.995805f, 183),
            Stop(11862018.toStopId(), 2, "Središće", 45.777817f, 15.989334f, 181),
            Stop(117571596.toStopId(), 12, "Sopot", 45.77772f, 15.984625f, 1794),*/
         ).toSortedListMap { it.id }
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
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Natrag",
            onClick = { finish() },
         )
      },
      windowInsets = WindowInsets.safeDrawing
         .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
   )

   @Composable
   private fun Content(modifier: Modifier) = Column(modifier) {
      HighlightedStationEntry()
      ScheduleInfoEntry()
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
            color = lerp(LocalContentColor.current, MaterialTheme.colorScheme.background, .20f),
            fontSize = 12.sp,
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

               LiveTravelSlider(sampleRouteScheduleEntry, sampleStops, interactable = false)
            }
         }
      )
   }

   @Composable
   private fun ScheduleInfoEntry() {
      val scope = rememberCoroutineScope()
      var showDialog by remember { mutableStateOf(false) }

      val schedule = Schedule.instance
      val feedInfo = (schedule as? LoadedSchedule)?.feedInfo

      Column(
         modifier = Modifier
            .clickable { showDialog = true }
            .defaultEntryModifiers()
      ) {
         Text("Informacije o rasporedu", style = MaterialTheme.typography.titleLarge)
         Text(
            if (feedInfo != null) "Verzija: ${feedInfo.version}"
            else "Nema preuzetog rasporeda",
            color = lerp(LocalContentColor.current, MaterialTheme.colorScheme.background, .20f),
            fontSize = 12.sp,
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
               if (schedule is LoadedSchedule) {
                  val latestVersion = Schedule.lastCheckedLatestVersion
                  val startDate = feedInfo?.startDate
                  val lastDate = schedule.calendarDates.lastDate
                  val newScheduleFeedInfo = try {
                     Schedule.getNewScheduleFile(filesDir).feedInfo
                  } catch (e: Exception) {
                     null
                  }
                  Text(
                     buildString {
                        append("Verzija: ").append(feedInfo?.version.orLoading())
                        if (latestVersion != null)
                           append("\nNajnovija verzija: ").append(latestVersion)
                        append("\nPočetak rasporeda: ").append(startDate?.toString().orLoading())
                        append("\nKraj rasporeda: ").append(lastDate?.toString().orLoading())
                        if (newScheduleFeedInfo != null) {
                           append("\n\nPostoji neaktivirani raspored:")
                           append("\n Verzija: ").append(newScheduleFeedInfo.version)
                           append("\n Početak: ").append(newScheduleFeedInfo.startDate)
                        }
                     }
                  )
               } else
                  Text("Nema preuzetog rasporeda")

               val (isCheckingUpdate, setIsCheckingUpdate) = remember { mutableStateOf(false) }
               val (isUpdating, setIsUpdating) = remember { mutableStateOf(false) }
               var errorMessage by remember { mutableStateOf<String?>(null) }

               FilledTonalButton(
                  onClick = {
                     scope.launch {
                        setIsCheckingUpdate(true)
                        errorMessage = null
                        val errorType = Schedule.update(filesDir).await()
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
               errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

               UpdateState(setIsUpdating)
            }
         }
      )
   }

   @Composable
   private fun UpdateState(setIsUpdating: (Boolean) -> Unit) {
      val loadingState = Schedule.priorityLoadingState ?: Schedule.loadingState
      if (loadingState == null) {
         setIsUpdating(false)
         return
      }
      setIsUpdating(true)
      Text(loadingState.text)
      if (loadingState is Schedule.Companion.TrackableLoadingState)
         LinearProgressIndicator(progress = { loadingState.progress })
      else
         LinearProgressIndicator()
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