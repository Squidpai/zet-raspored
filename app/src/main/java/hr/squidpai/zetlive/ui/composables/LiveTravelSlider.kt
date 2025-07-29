package hr.squidpai.zetlive.ui.composables

import androidx.collection.IntList
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetapi.Love
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.gtfs.RouteScheduleEntry
import hr.squidpai.zetlive.gtfs.preferredHeadsign
import hr.squidpai.zetlive.gtfs.preferredName
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import hr.squidpai.zetlive.ui.showTripDialog

@Composable
fun LiveTravelSlider(routeScheduleEntry: RouteScheduleEntry) {
   val context = LocalContext.current

   val (nextStopIndex, sliderValue, trip, departureTime, delayAmount,
      selectedDate, isCancelled) = routeScheduleEntry

   val isAtFirstStop = nextStopIndex == 0
   val specialLabel = Love.giveMeTheSpecialTripLabel(trip)
   val isLate = delayAmount >= 5 * 60

   LiveTravelSlider(
      nextStopIndex = nextStopIndex,
      sliderValue = sliderValue,
      stopNames = trip.stops.map { it.preferredName },
      departures = trip.departures,
      modifier = Modifier
         .padding(vertical = 6.dp)
         .clickable { showTripDialog(context, trip, selectedDate) },
      bottomStartLabel = when {
         isCancelled -> "otkazano"
         isAtFirstStop ->
            (if (departureTime >= 0) "kreće u ${departureTime.timeToString()}"
            else "kreće za ${-departureTime - 1} min")
               .let { if (isLate) "$it (kasni)" else it }

         !trip.isFirstStopCommon -> "polazište ${trip.stops.first().preferredName.orLoading()}"
         else -> null
      },
      isBottomStartError = isAtFirstStop && isLate,
      bottomCenterLabel = specialLabel?.first,
      bottomEndLabel = specialLabel?.second
         ?: if (!trip.isHeadsignCommon) "smjer ${trip.preferredHeadsign}" else null,
      tint = when {
         isCancelled -> MaterialTheme.colorScheme.surfaceVariant

         !trip.isHeadsignCommon || !trip.isFirstStopCommon || specialLabel != null ->
            MaterialTheme.colorScheme.tertiary

         else -> MaterialTheme.colorScheme.primary
      },
      disabled = isCancelled,
   )
}

@Composable
fun LiveTravelSlider(
   nextStopIndex: Int,
   sliderValue: Float,
   stopNames: List<String>,
   departures: IntList,
   modifier: Modifier = Modifier,
   titleText: String? = null,
   bottomStartLabel: String? = null,
   isBottomStartError: Boolean = false,
   bottomCenterLabel: String? = null,
   bottomEndLabel: String? = null,
   highlightNextStop: Boolean = Data.highlightNextStop,
   interactable: Boolean = true,
   tint: Color = MaterialTheme.colorScheme.primary,
   disabled: Boolean = false,
) = Column(modifier) {
   val highlightedStopIndex =
      if (highlightNextStop) nextStopIndex
      else nextStopIndex - 1
   val isAtFirstStop = nextStopIndex == 0

   val firstVisibleItemIndex = if (highlightedStopIndex > 0 && interactable) 1 else 0
   val state = rememberSaveable(highlightedStopIndex, saver = LazyListState.Saver) {
      LazyListState(firstVisibleItemIndex)
   }

   if (interactable)
      LaunchedEffect(Unit) {
         state.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Stop &&
               state.firstVisibleItemIndex == 1 &&
               state.firstVisibleItemScrollOffset < 128
            ) state.scrollToItem(1, scrollOffset = 0)
         }
      }

   if (titleText != null)
      Text(
         titleText,
         overflow = TextOverflow.Ellipsis,
         maxLines = 1,
         style = MaterialTheme.typography.labelLarge,
      )

   LazyRow(
      modifier = Modifier.height(40.dp),
      state = state,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
      userScrollEnabled = interactable,
   ) {
      // if the slider is not interactable, the user will never see this item
      if (highlightedStopIndex > 0 && interactable) item {
         Text(
            stopNames.subList(0, highlightedStopIndex).joinToString(
               separator = " ${Typography.bullet} ",
               postfix = if (!highlightNextStop) " ${Typography.bullet} " else "",
            ),
            color = MaterialTheme.colorScheme.disabled,
            style = MaterialTheme.typography.bodyMedium,
         )
      }
      if (highlightedStopIndex < stopNames.size) item {
         if (!isAtFirstStop && highlightNextStop) Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.padding(horizontal = 8.dp),
            contentDescription = null,
            tint = if (disabled) MaterialTheme.colorScheme.disabled else tint,
         )

         val textPaddingModifier =
            if (highlightNextStop) Modifier.padding(end = 8.dp)
            else Modifier.padding(start = 8.dp)
         Text(
            text = stopNames[highlightedStopIndex.coerceAtLeast(0)],
            modifier = textPaddingModifier,
            color = if (disabled) MaterialTheme.colorScheme.disabled else Color.Unspecified,
            fontWeight = FontWeight.Medium,
         )

         if (!isAtFirstStop && !highlightNextStop) Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.padding(horizontal = 8.dp),
            contentDescription = null,
            tint = if (disabled) MaterialTheme.colorScheme.disabled else tint,
         )
      }
      if (highlightedStopIndex + 1 < stopNames.size) item {
         Text(
            stopNames.subList(
               (highlightedStopIndex + 1).coerceAtLeast(1),
               stopNames.size
            ).joinToString(
               separator = " ${Typography.bullet} ",
               prefix = if (highlightNextStop || isAtFirstStop) " ${Typography.bullet} " else "",
            ),
            color = MaterialTheme.colorScheme.disabled,
            style = MaterialTheme.typography.bodyMedium,
         )
      }
   }

   RouteSlider(
      value = sliderValue,
      departures = departures,
      modifier = Modifier.fillMaxWidth(),
      passedTrackColor = tint,
      nextStopColor = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant
      else MaterialTheme.colorScheme.onSurface,
   )

   if (bottomStartLabel != null || bottomCenterLabel != null || bottomEndLabel != null)
      Row(
         modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
         horizontalArrangement = Arrangement.SpaceBetween,
      ) {
         if (bottomStartLabel != null)
            Text(
               text = bottomStartLabel,
               color = when {
                  disabled -> MaterialTheme.colorScheme.disabled
                  isBottomStartError -> MaterialTheme.colorScheme.error
                  else -> Color.Unspecified
               },
               fontWeight = FontWeight.Medium.takeIf { isBottomStartError },
            )
         else // blank box to take up space
            Box(Modifier.size(0.dp))

         if (bottomCenterLabel != null)
            Text(
               bottomCenterLabel,
               color = if (disabled) MaterialTheme.colorScheme.disabled else Color.Unspecified,
            )

         if (bottomEndLabel != null)
            Text(
               bottomEndLabel,
               color = if (disabled) MaterialTheme.colorScheme.disabled else Color.Unspecified,
               textAlign = TextAlign.End,
            )
      }
}
