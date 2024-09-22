package hr.squidpai.zetlive.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.alsoIf
import hr.squidpai.zetlive.gtfs.Love
import hr.squidpai.zetlive.gtfs.RouteScheduleEntry
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import hr.squidpai.zetlive.ui.TripDialogActivity

@Composable
fun LiveTravelSlider(routeScheduleEntry: RouteScheduleEntry, interactable: Boolean = true) {
   val context = LocalContext.current

   val highlightNextStop = Data.highlightNextStop

   val (_, sliderValue, trip, headsign, isHeadsignCommon, overriddenFirstStop, departureTime,
      delayAmount, timeOffset) = routeScheduleEntry
   val isAtFirstStop = routeScheduleEntry.nextStopIndex == 0
   val highlightedStopIndex =
      if (highlightNextStop) routeScheduleEntry.nextStopIndex
      else routeScheduleEntry.nextStopIndex - 1

   val specialLabel = Love.giveMeTheSpecialTripLabel(trip)

   Column(
      Modifier
         .padding(vertical = 6.dp)
         .alsoIf(interactable) {
            clickable { TripDialogActivity.selectTrip(context, trip, timeOffset) }
         },
   ) {
      val tint =
         if (!isHeadsignCommon || overriddenFirstStop.isValid() || specialLabel != null)
            MaterialTheme.colorScheme.tertiary
         else MaterialTheme.colorScheme.primary

      val stops = Schedule.instance.stops?.list

      val firstVisibleItemIndex = if (highlightedStopIndex > 0) 1 else 0
      val state = rememberSaveable(highlightedStopIndex, saver = LazyListState.Saver) {
         LazyListState(firstVisibleItemIndex)
      }

      LaunchedEffect(Unit) {
         state.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Stop &&
               state.firstVisibleItemIndex == 1 &&
               state.firstVisibleItemScrollOffset < 128
            ) state.scrollToItem(1, scrollOffset = 0)
         }
      }

      if (stops != null) LazyRow(
         modifier = Modifier.height(40.dp),
         state = state,
         horizontalArrangement = Arrangement.spacedBy(4.dp),
         verticalAlignment = Alignment.CenterVertically,
         userScrollEnabled = interactable,
      ) {
         if (highlightedStopIndex > 0) item {
            Text(
               trip.joinStopsToString(
                  stops,
                  endIndex = highlightedStopIndex,
                  postfix = " ${Typography.bullet} ".takeIf { !highlightNextStop },
               ),
               color = lerp(
                  MaterialTheme.colorScheme.onSurface,
                  MaterialTheme.colorScheme.surface,
                  .36f
               ),
               style = MaterialTheme.typography.bodyMedium,
            )
         }
         item {
            if (!isAtFirstStop && highlightNextStop) Icon(
               Icons.AutoMirrored.Filled.ArrowForward,
               modifier = Modifier.padding(horizontal = 8.dp),
               contentDescription = null,
               tint = tint
            )

            val textPaddingModifier =
               if (highlightNextStop) Modifier.padding(end = 8.dp)
               else Modifier.padding(start = 8.dp)
            Text(
               text = stops[trip.stops[highlightedStopIndex.coerceAtLeast(0)].toStopId()]?.name.orLoading(),
               modifier = textPaddingModifier,
               fontWeight = FontWeight.Medium,
            )

            if (!isAtFirstStop && !highlightNextStop) Icon(
               Icons.AutoMirrored.Filled.ArrowForward,
               modifier = Modifier.padding(horizontal = 8.dp),
               contentDescription = null,
               tint = tint
            )
         }
         if (highlightedStopIndex + 1 < trip.stops.size) item {
            Text(
               trip.joinStopsToString(
                  stops,
                  beginIndex = (highlightedStopIndex + 1).coerceAtLeast(1),
                  prefix = " ${Typography.bullet} ".takeIf { highlightNextStop || isAtFirstStop },
               ),
               color = lerp(
                  MaterialTheme.colorScheme.onSurface,
                  MaterialTheme.colorScheme.surface,
                  .36f
               ),
               style = MaterialTheme.typography.bodyMedium,
            )
         }
      }

      RouteSlider(
         value = sliderValue,
         departures = trip.departures,
         modifier = Modifier.fillMaxWidth(),
         passedTrackColor = tint,
      )

      if (isAtFirstStop ||
         !isHeadsignCommon ||
         overriddenFirstStop.isValid() ||
         specialLabel != null
      )
         Row(
            modifier = Modifier
               .fillMaxWidth()
               .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
         ) {
            if (isAtFirstStop) {
               val isLate = delayAmount >= 10 * 60
               Text(
                  (if (departureTime >= 0) "kreće u ${departureTime.timeToString()}"
                  else "kreće za ${(-departureTime - 1) / 60} min")
                     .let { if (isLate) "$it (kasni)" else it },
                  color = if (isLate) MaterialTheme.colorScheme.error else Color.Unspecified,
                  fontWeight = FontWeight.Medium.takeIf { isLate }
               )
            } else if (overriddenFirstStop.isValid())
            // do not display the first stop if stopSequence == 1 because then it is already highlighted
               Text("polazište ${stops?.get(overriddenFirstStop)?.name.orLoading()}")
            else
            // blank box take up space
               Box(Modifier.size(0.dp))

            specialLabel?.first?.let { Text(it) }

            if (specialLabel?.second != null || !isHeadsignCommon)
               Text(specialLabel?.second ?: "smjer $headsign", textAlign = TextAlign.End)
         }
   }
}
