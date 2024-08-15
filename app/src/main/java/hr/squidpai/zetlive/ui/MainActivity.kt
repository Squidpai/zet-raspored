package hr.squidpai.zetlive.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Schedule
import kotlinx.coroutines.launch

@Suppress("unused")
private const val TAG = "MainActivity"

/**
 * The main Activity of the app. Displays all routes and stops and all their schedules.
 *
 * TODO Will also display the map view soon.
 */
class MainActivity : ComponentActivity() {

   @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()
      setContent {
         AppTheme {
            Scaffold(
               topBar = { MyTopAppBar() },
               snackbarHost = {
                  val loadingState = Schedule.loadingState
                  if (loadingState != null) Snackbar {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        if (loadingState is Schedule.Companion.TrackableLoadingState)
                           CircularProgressIndicator(
                              progress = { loadingState.progress },
                              color = MaterialTheme.colorScheme.inversePrimary,
                           )
                        else CircularProgressIndicator()
                        Text(loadingState.text, Modifier.padding(start = 8.dp))
                     }
                  }
               },
               contentWindowInsets = WindowInsets.safeDrawing,
            ) { padding ->
               Column(modifier = Modifier.padding(padding)) {
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
                        0 -> MainActivityRoutes()
                        1 -> MainActivityStops()
                     }
                  }

                  PriorityLoadingDialog()
               }
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

   /**
    * Displays a Material 3 [TopAppBar].
    */
   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun MyTopAppBar() {
      //var showUpdateDialog by remember { mutableStateOf(false) }

      TopAppBar(
         title = { Text("Raspored", maxLines = 1, overflow = TextOverflow.Ellipsis) },
         actions = {
            IconButton(
               Symbols.Settings,
               contentDescription = "Postavke",
               onClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
            )
         },
         windowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
      )

      /*if (showUpdateDialog) {
        var link by remember { mutableStateOf("https://www.zet.hr/gtfs-scheduled/scheduled-000-000348.zip") }

        val context = LocalContext.current

        AlertDialog(
          onDismissRequest = { showUpdateDialog = false },
          title = { Text("Enter version number") },
          text = {
            OutlinedTextField(
              value = link,
              onValueChange = { link = it },
            )
          },
          confirmButton = {
            TextButton(
              onClick = {
                showUpdateDialog = false
                Schedule.update(context.filesDir, link)
              }
            ) {
              Text("Update")
            }
          },
          dismissButton = {
            TextButton(
              onClick = {
                showUpdateDialog = false
                Schedule.update(context.filesDir)
              }
            ) {
              Text("Latest")
            }
          }
        )
      }*/
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
