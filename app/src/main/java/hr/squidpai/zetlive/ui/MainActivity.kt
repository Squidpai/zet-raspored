package hr.squidpai.zetlive.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.clientVersionStalenessDays
import com.google.android.play.core.ktx.installStatus
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.android.play.core.ktx.updatePriority
import hr.squidpai.zetlive.gtfs.EmptySchedule
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.LoadedSchedule
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.ui.MainActivity.VersionStaleness.DRY
import hr.squidpai.zetlive.ui.MainActivity.VersionStaleness.ROTTEN
import hr.squidpai.zetlive.ui.MainActivity.VersionStaleness.SPOILED
import hr.squidpai.zetlive.ui.MainActivity.VersionStaleness.STALE
import hr.squidpai.zetlive.ui.composables.HintIconButton
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.ui.composables.errorIconButtonColors
import hr.squidpai.zetlive.ui.composables.inverseRichTooltipColors
import hr.squidpai.zetlive.ui.composables.textButtonColorsOnInverseRichTooltip
import kotlinx.coroutines.launch

@Suppress("unused")
private const val TAG = "MainActivity"

/**
 * The main Activity of the app. Displays all routes and stops and all their schedules.
 *
 * TODO Will also display the map view soon.
 */
class MainActivity : ComponentActivity() {

   private lateinit var appUpdateManager: AppUpdateManager

   @OptIn(ExperimentalMaterial3Api::class)
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      appUpdateManager = AppUpdateManagerFactory.create(this)

      appUpdateManager.appUpdateInfo
         .addOnSuccessListener { appUpdateInfo ->
            updateInfo = UpdateInfo.from(appUpdateInfo)
         }

      enableEdgeToEdge()
      setContent {
         AppTheme {
            Scaffold(
               topBar = { MyTopAppBar() },
               snackbarHost = { MySnackbars() },
               contentWindowInsets = WindowInsets.safeDrawing,
            ) { padding ->
               val outerModifier = Modifier.padding(padding)
               when (val schedule = Schedule.instance) {
                  is EmptySchedule -> MainActivityLoading(
                     errorType = schedule.errorType,
                     modifier = outerModifier,
                  )

                  is LoadedSchedule -> Column(modifier = outerModifier) {
                     val scope = rememberCoroutineScope()

                     val pagerState = rememberPagerState { 2 }

                     val selectedPage = pagerState.currentPage

                     val keyboardController = LocalSoftwareKeyboardController.current
                     val focusManager = LocalFocusManager.current

                     fun setSelectedPage(page: Int) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        scope.launch { pagerState.animateScrollToPage(page) }
                     }

                     PrimaryTabRow(selectedTabIndex = selectedPage) {
                        Tab(
                           selected = selectedPage == 0,
                           onClick = { setSelectedPage(0) },
                           text = {
                              Text("Linije", maxLines = 1, overflow = TextOverflow.Ellipsis)
                           },
                        )
                        Tab(
                           selected = selectedPage == 1,
                           onClick = { setSelectedPage(1) },
                           text = {
                              Text("Postaje", maxLines = 1, overflow = TextOverflow.Ellipsis)
                           },
                        )
                     }

                     HorizontalPager(
                        state = pagerState,
                        pageNestedScrollConnection = object : NestedScrollConnection {
                           // Make the child consume all the available scroll amount, so it
                           // doesn't overscroll into the other pager state
                           override fun onPostScroll(
                              consumed: Offset,
                              available: Offset,
                              source: NestedScrollSource
                           ) = available
                        }
                     ) {
                        when (it) {
                           0 -> MainActivityRoutes(schedule.routes, schedule.stops)
                           1 -> MainActivityStops(schedule.stops.groupedStops)
                        }
                     }

                     PriorityLoadingDialog()
                  }
               }


            }
         }
      }
   }

   override fun onPause() {
      super.onPause()
      Live.pauseLiveData()
      appUpdateManager.unregisterListener(installStateUpdatedListener)
   }

   override fun onResume() {
      super.onResume()
      Live.resumeLiveData()
      appUpdateManager.registerListener(installStateUpdatedListener)
      // Immediately
      appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
         updateProgress =
            when (appUpdateInfo.installStatus) {
               InstallStatus.DOWNLOADING ->
                  (appUpdateInfo.bytesDownloaded().toFloat() / appUpdateInfo.totalBytesToDownload())
                     .coerceIn(0f, 1f) // just in case
               InstallStatus.DOWNLOADED -> 2f
               else -> -1f
            }

         appUpdateInfo.bytesDownloaded()

         if (appUpdateInfo.updateAvailability() ==
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
         ) {
            // If an in-app update is already running, resume the update.
            appUpdateManager.startUpdateFlow(
               appUpdateInfo,
               this,
               AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
         }
      }
   }

   /** Displays a Material 3 [TopAppBar]. */
   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun MyTopAppBar() = TopAppBar(
      title = { Text("Raspored", maxLines = 1, overflow = TextOverflow.Ellipsis) },
      actions = {
         UpdateIconButton()

         if (Live.instance.isCached) HintIconButton(
            icon = Symbols.NoInternet,
            contentDescription = null,
            tooltipTitle = "Nema interneta.",
            tooltipText = "Dogodila se greška prilikom preuzimanja rasporeda uživo.\n" +
                  "Raspored prikazan možda neće biti točan.",
            iconTint = MaterialTheme.colorScheme.error,
            action = {
               var attempting by remember { mutableStateOf(false) }

               TextButton(
                  onClick = {
                     attempting = true
                     Live.updateNow { attempting = false }
                  },
                  enabled = !attempting,
                  colors = ButtonDefaults.textButtonColorsOnInverseRichTooltip(),
               ) {
                  Text(if (attempting) "Preuzimanje${Typography.ellipsis}" else "Pokušaj ponovno")
               }
            },
            showClickIndication = true,
         )
         IconButton(
            Symbols.Settings,
            contentDescription = "Postavke",
            onClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
         )
      },
      windowInsets = WindowInsets.safeDrawing
         .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
   )

   @Composable
   private fun MySnackbars() {
      val loadingState = Schedule.loadingState
      if (loadingState != null) {
         Snackbar {
            Row(verticalAlignment = Alignment.CenterVertically) {
               if (loadingState is Schedule.Companion.TrackableLoadingState)
                  CircularProgressIndicator(
                     progress = { loadingState.progress },
                     color = MaterialTheme.colorScheme.inversePrimary,
                     trackColor = MaterialTheme.colorScheme.inverseSurface,
                  )
               else CircularProgressIndicator()
               Text(loadingState.text, Modifier.padding(start = 8.dp))
            }
         }
         return
      }

      when (updateProgress) {
         in 0f..1f -> Snackbar {
            Row(verticalAlignment = Alignment.CenterVertically) {
               CircularProgressIndicator(
                  progress = { updateProgress },
                  color = MaterialTheme.colorScheme.inversePrimary,
                  trackColor = MaterialTheme.colorScheme.inverseSurface,
               )
               Text("Preuzimanje ažuriranja", Modifier.padding(start = 8.dp))
            }
         }

         2f -> Snackbar(
            action = {
               TextButton(onClick = { appUpdateManager.completeUpdate() }) {
                  Text("Instaliraj", color = MaterialTheme.colorScheme.inversePrimary)
               }
            }
         ) {
            Text("Ažuriranje preuzeto.")
         }
      }
   }

   @Composable
   private fun PriorityLoadingDialog() {
      val loadingState = Schedule.priorityLoadingState
      if (loadingState != null) AlertDialog(
         onDismissRequest = { },
         text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
               if (loadingState is Schedule.Companion.TrackableLoadingState)
                  CircularProgressIndicator(progress = { loadingState.progress })
               else CircularProgressIndicator()
               Text(loadingState.text, Modifier.padding(start = 16.dp))
            }
         },
         confirmButton = { }
      )
   }

   private var updateInfo by mutableStateOf<UpdateInfo?>(null)

   private var updateProgress by mutableFloatStateOf(-1f)

   private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
      updateProgress =
         when (state.installStatus) {
            InstallStatus.DOWNLOADING ->
               (state.bytesDownloaded().toFloat() / state.totalBytesToDownload())
                  .coerceIn(0f, 1f) // just in case
            InstallStatus.DOWNLOADED -> 2f
            else -> -1f
         }
   }

   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun UpdateIconButton() {
      val currentUpdateInfo = updateInfo
      if (currentUpdateInfo?.versionStaleness?.shouldShowIcon == true) {
         val state = rememberTooltipState(isPersistent = true)

         TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
               RichTooltip(
                  title = { Text("Postoji ažuriranje") },
                  action = {
                     if (currentUpdateInfo.appUpdateInfo.isFlexibleUpdateAllowed ||
                        currentUpdateInfo.appUpdateInfo.isImmediateUpdateAllowed
                     ) TextButton(
                        onClick = {
                           val appUpdateType =
                              if (currentUpdateInfo.appUpdateInfo.isFlexibleUpdateAllowed &&
                                 (!currentUpdateInfo.isUrgent ||
                                       !currentUpdateInfo.appUpdateInfo.isImmediateUpdateAllowed)
                              ) AppUpdateType.FLEXIBLE
                              else AppUpdateType.IMMEDIATE

                           appUpdateManager.startUpdateFlow(
                              currentUpdateInfo.appUpdateInfo,
                              this@MainActivity,
                              AppUpdateOptions.defaultOptions(appUpdateType),
                           ).addOnSuccessListener { resultCode ->
                              if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED)
                                 Toast.makeText(
                                    this@MainActivity,
                                    "Dogodila se pogreška pokušavajući ažurirati aplikaciju.",
                                    Toast.LENGTH_LONG,
                                 ).show()
                              state.dismiss()
                           }
                        },
                        colors = ButtonDefaults.textButtonColorsOnInverseRichTooltip(),
                     ) {
                        Text("Ažuriraj")
                     } else when (updateProgress) {
                        2f -> TextButton(onClick = { appUpdateManager.completeUpdate() }) {
                           Text("Instaliraj", color = MaterialTheme.colorScheme.inversePrimary)
                        }

                        in 0f..1f -> Column {
                           Text("Preuzimanje ažuriranja${Typography.ellipsis}")
                           LinearProgressIndicator(
                              progress = { updateProgress },
                              color = MaterialTheme.colorScheme.inversePrimary,
                              trackColor = MaterialTheme.colorScheme.inverseSurface,
                           )
                        }

                        else -> {}
                     }
                  },
                  colors = TooltipDefaults.inverseRichTooltipColors(),
               ) {
                  Text(currentUpdateInfo.message)
               }
            },
            state,
            focusable = false,
            enableUserInput = false,
         ) {
            val scope = rememberCoroutineScope()
            IconButton(
               icon = Symbols.Upgrade,
               contentDescription = "Postoji ažuriranje",
               onClick = {
                  appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                     val newUpdateInfo = UpdateInfo.from(appUpdateInfo)
                     updateInfo = newUpdateInfo
                     if (newUpdateInfo?.versionStaleness?.shouldShowIcon != true)
                        return@addOnSuccessListener

                     scope.launch { state.show() }
                  }
               },
               colors = currentUpdateInfo.versionStaleness.colors,
            )
         }
      }
   }

   private enum class VersionStaleness {
      /** No need to display anything to the user yet. */
      DRY,

      /** Show a simple icon. */
      STALE,

      /** Show a tinted icon. */
      SPOILED,

      /** Show an urgent icon. */
      ROTTEN;

      val shouldShowIcon get() = this != DRY

      val colors
         @Composable get(): IconButtonColors = when (this) {
            ROTTEN -> IconButtonDefaults.errorIconButtonColors()
            SPOILED -> IconButtonDefaults.filledIconButtonColors()
            else -> IconButtonDefaults.iconButtonColors()
         }
   }

   private data class UpdateInfo(
      val versionStaleness: VersionStaleness,
      val message: String,
      val appUpdateInfo: AppUpdateInfo,
      val isUrgent: Boolean = false,
   ) {
      companion object {
         fun from(appUpdateInfo: AppUpdateInfo) =
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
               appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
               val versionStalenessDays = appUpdateInfo.clientVersionStalenessDays ?: -1

               /*
               Legend for version staleness:
                      update priority
               days  	0 1 2 3 4 5     symbols
               ..<7	          . o !    DRY
               7..<30	    . o o !    STALE   .
               30..<100	  . o o ! !    SPOILED o
               100..	. o o ! ! !    ROTTEN  !
                */

               val versionStaleness = when (appUpdateInfo.updatePriority) {
                  0 ->
                     if (versionStalenessDays < 100) DRY
                     else STALE

                  1 ->
                     if (versionStalenessDays < 30) DRY
                     else if (versionStalenessDays < 100) STALE
                     else SPOILED

                  2 ->
                     if (versionStalenessDays < 7) DRY
                     else if (versionStalenessDays < 30) STALE
                     else SPOILED

                  3 ->
                     if (versionStalenessDays < 7) STALE
                     else if (versionStalenessDays < 100) SPOILED
                     else ROTTEN

                  4 ->
                     if (versionStalenessDays < 30) SPOILED
                     else ROTTEN

                  5 -> ROTTEN
                  else -> STALE
               }

               val message = when (appUpdateInfo.updatePriority) {
                  3, 4 -> "Nova značajka popravlja mali problem s trenutnom verzijom, " +
                        "preporuka je da ažurirate aplikaciju u jednom trenutku."

                  5 -> "Preporuka je da odmah ažurirate aplikaciju jer novo ažuriranje " +
                        "popravlja kritičnu pogrešku koja postoji u trenutnoj verziji."

                  else -> "Ažurirajte aplikaciju kako biste uvijek imali najnovije značajke."
               }

               UpdateInfo(versionStaleness, message, appUpdateInfo)
            } else null

      }
   }
}
