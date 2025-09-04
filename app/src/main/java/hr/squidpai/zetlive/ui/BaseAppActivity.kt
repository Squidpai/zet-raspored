package hr.squidpai.zetlive.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetlive.gtfs.ScheduleManager
import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseAppActivity(
    @Suppress("PropertyName") protected val TAG: String
) : ComponentActivity() {

    private val mTripDialogData = MutableStateFlow<TripDialogData?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            mTripDialogData.value?.let { startNotificationTracking(it) }
            mTripDialogData.value = null
            onTripDialogClosed()
        } else
            Toast.makeText(
                this,
                "Ne može se postaviti obavijest${Typography.mdash}" +
                        "odbijena je dozvola za postavljanje obavijesti.",
                Toast.LENGTH_LONG
            ).show()
    }

    private fun trackInNotifications(tripDialogData: TripDialogData) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startNotificationTracking(tripDialogData)
            mTripDialogData.value = null
            onTripDialogClosed()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startNotificationTracking(tripDialogData: TripDialogData) {
        startForegroundService(
            Intent(this, NotificationTrackerService::class.java)
                .putExtra(EXTRA_ROUTE_ID, tripDialogData.trip.route.id)
                .putExtra(EXTRA_TRIP_ID, tripDialogData.trip.tripId)
                .putExtra(EXTRA_SELECTED_DATE, tripDialogData.selectedDate)
        )
    }

    fun showTripDialog(tripDialogData: TripDialogData) {
        mTripDialogData.value = tripDialogData
    }

    fun showTripDialog(trip: Trip, selectedDate: Long) =
        showTripDialog(TripDialogData(trip, selectedDate))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Content()

                val tripData = mTripDialogData.collectAsState().value

                if (tripData != null)
                    TripDialog(
                        onDismissRequest = {
                            mTripDialogData.value = null
                            onTripDialogClosed()
                        },
                        onStopClicked = {
                            startActivity(
                                Intent(this, StopScheduleActivity::class.java)
                                    .putExtra(EXTRA_STOP, it.id.rawValue)
                            )
                        },
                        onTrackInNotificationsRequest = { trackInNotifications(tripData) },
                        data = tripData,
                    )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ScheduleManager.realtimeDispatcher.addListener(TAG)
    }

    override fun onStop() {
        super.onStop()
        ScheduleManager.realtimeDispatcher.removeListener(TAG)
    }

    @Composable
    protected abstract fun Content()

    protected open fun onTripDialogClosed() = Unit

}