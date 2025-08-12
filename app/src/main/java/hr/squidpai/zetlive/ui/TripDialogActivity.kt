package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.TripId
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.localEpochDate

class TripDialogActivity : BaseAppActivity("TripDialogActivity") {

   private lateinit var routeId: RouteId
   private lateinit var tripId: TripId
   private var selectedDate = 0L

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      routeId = intent.getStringExtra(EXTRA_ROUTE_ID)
         ?: run {
            Log.w(TAG, "onCreate: no routeId given, finishing activity early")
            finish()
            return
         }

      tripId = intent.getStringExtra(EXTRA_TRIP_ID)
         ?: run {
            Log.w(TAG, "onCreate: no tripId given, finishing activity early")
            finish()
            return
         }

      selectedDate = intent.getLongExtra(EXTRA_SELECTED_DATE, 0L)
      if (selectedDate == 0L)
         selectedDate = localEpochDate()
   }

   @Composable
   override fun Content() {
      val schedule = ScheduleManager.instance.collectAsState().value
         ?: return

      LaunchedEffect(System.identityHashCode(schedule)) {
         showTripDialog(
            trip = schedule.routes[routeId]?.trips?.get(tripId) ?: return@LaunchedEffect,
            selectedDate,
         )
      }
   }

   override fun onTripDialogClosed() = finish()
}