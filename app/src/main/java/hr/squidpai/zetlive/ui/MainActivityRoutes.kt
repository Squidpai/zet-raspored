package hr.squidpai.zetlive.ui

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.alsoIf
import hr.squidpai.zetlive.gtfs.Love
import hr.squidpai.zetlive.gtfs.Route
import hr.squidpai.zetlive.gtfs.RouteScheduleEntry
import hr.squidpai.zetlive.gtfs.Routes
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.getLiveSchedule
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainActivityRoutes() = Column(Modifier.fillMaxSize()) {
   val routes = Schedule.instance.routes

   val inputState = rememberSaveable { mutableStateOf("") }

   val pinnedRoutes = Data.pinnedRoutes.toSet()
   val list = remember(routes) {
      routes?.let {
         mutableStateListOf<Route>().apply { addAll(it.filter(inputState.value.trim())) }
      }
   }

   if (routes != null && list != null) {
      val lazyListState = rememberLazyListState()

      RouteFilterSearchBar(
         inputState, routes, list, lazyListState,
         modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
      )

      LazyColumn(state = lazyListState) {
         if (inputState.value.isBlank())
            for (pinnedRouteId in pinnedRoutes) {
               val route = routes.list.get(key = pinnedRouteId) ?: continue
               item(key = -pinnedRouteId) {
                  RouteContent(
                     route, pinned = true,
                     modifier = Modifier
                        .fillParentMaxWidth()
                        .animateItemPlacement(),
                  )
               }
            }

         items(list.size, key = { list[it].id }) {
            val route = list[it]
            RouteContent(
               route, pinned = route.id in pinnedRoutes,
               modifier = Modifier
                  .fillParentMaxWidth()
                  .animateItemPlacement(),
            )
         }
      }
   } else CircularLoadingBox()

}

@Composable
private fun RouteFilterSearchBar(
   inputState: MutableState<String>,
   routes: Routes,
   list: SnapshotStateList<Route>,
   lazyListState: LazyListState,
   modifier: Modifier = Modifier,
) {
   val (input, setInput) = inputState

   val coroutineScope = rememberCoroutineScope()

   val updateInput = updateInput@{ newInput: String ->
      if (newInput.length > 100) return@updateInput
      setInput(newInput)

      val newInputTrimmed = newInput.trim()
      val oldInputTrimmed = input.trim()

      if (newInputTrimmed != oldInputTrimmed) {
         coroutineScope.launch { lazyListState.scrollToItem(0, 0) }

         val newList =
            if (input in newInput) Routes.filter(list, newInputTrimmed)
            else routes.filter(newInputTrimmed)

         list.clear()
         list.addAll(newList)
      }
   }

   val keyboardController = LocalSoftwareKeyboardController.current

   OutlinedTextField(
      value = input,
      onValueChange = updateInput,
      modifier = modifier,
      label = { Text("Pretraži linije", maxLines = 1, overflow = TextOverflow.Ellipsis) },
      leadingIcon = { Icon(Symbols.Search, null) },
      trailingIcon = {
         if (input.isNotEmpty())
            IconButton(
               Symbols.Close,
               "Izbriši unos",
               onClick = { updateInput("") }
            )
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions { keyboardController?.hide() },
   )
}

@Composable
private fun RouteContent(route: Route, pinned: Boolean, modifier: Modifier) {
   val (expanded, setExpanded) =
      rememberSaveable(key = "re${route.id}") { mutableStateOf(false) }

   val context = LocalContext.current

   Surface(
      modifier = modifier
         .padding(4.dp)
         .animateContentSize(),
      tonalElevation = if (expanded) 2.dp else 0.dp,
   ) {
      Column {
         Row(
            modifier = Modifier
               .defaultMinSize(minHeight = 48.dp)
               .clickable(
                  onClick = { setExpanded(!expanded) },
                  /*onClick = {
                    if (expanded) setExpanded(false)
                    else context.startActivity(
                      Intent(context, RouteScheduleActivity::class.java)
                        .putExtra(RouteScheduleActivity.EXTRA_ROUTE, route.id)
                    )
                  },*/
               ),
            verticalAlignment = Alignment.CenterVertically,
         ) {
            val shortNameStyle = MaterialTheme.typography.titleMedium
            Text(
               route.shortName,
               modifier = Modifier.width(with(LocalDensity.current) { (shortNameStyle.fontSize * 3.5f).toDp() }),
               color = MaterialTheme.colorScheme.primary,
               textAlign = TextAlign.Center,
               style = shortNameStyle,
            )
            Text(route.longName, Modifier.weight(1f))

            if (expanded || pinned)
               IconButton(
                  if (pinned) Symbols.PushPinFilled else Symbols.PushPin,
                  if (pinned) "Otkvači s vrha popisa" else "Zakvači na vrh popisa",
                  onClick = {
                     Data.updateData {
                        if (route.id !in pinnedRoutes) pinnedRoutes += route.id
                        else pinnedRoutes -= route.id
                     }
                  }
               )
         }

         if (expanded) {
            val directionState = rememberSaveable { mutableIntStateOf(0) }

            Row(
               modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
               horizontalArrangement = Arrangement.SpaceAround,
            ) {
               OutlinedButton(onClick = {
                  context.startActivity(
                     Intent(context, RouteScheduleActivity::class.java)
                        .putExtra(EXTRA_ROUTE_ID, route.id)
                        .putExtra(EXTRA_DIRECTION, directionState.intValue)
                  )
               }) {
                  Text("Raspored")
               }
               /*TextButton(onClick = {
                 // TODO show map
               }) {
                 Text("Prikaži na karti")
               }*/
            }

            RouteLiveTravels(route, directionState)
         }
      }
   }
}

@Composable
private fun ColumnScope.RouteLiveTravels(route: Route, directionState: MutableIntState) {
   val routeLiveSchedule = route.getLiveSchedule()

   val (direction, setDirection) = directionState

   DirectionRow(
      commonHeadsign = routeLiveSchedule?.commonHeadsign,
      direction, setDirection,
      isRoundRoute = routeLiveSchedule != null &&
            (if (routeLiveSchedule.noLiveMessage == null && routeLiveSchedule.first!!.isNotEmpty())
               routeLiveSchedule.second!!.isEmpty()
            else routeLiveSchedule.commonHeadsign.second.isEmpty()),
   )

   if (routeLiveSchedule == null) {
      CircularProgressIndicator(
         Modifier
            .align(Alignment.CenterHorizontally)
            .padding(vertical = 8.dp)
      )
      return
   }

   val liveTravels =
      if (direction == 0) routeLiveSchedule.first
      else routeLiveSchedule.second

   if (liveTravels.isNullOrEmpty()) {
      Text(
         routeLiveSchedule.noLiveMessage ?: "Linija danas nema više polazaka.",
         Modifier
            .padding(vertical = 8.dp)
            .align(Alignment.CenterHorizontally)
      )
      return
   }

   for (entry in liveTravels)
      LiveTravelSlider(entry)
}

@Composable
fun LiveTravelSlider(routeScheduleEntry: RouteScheduleEntry, interactable: Boolean = true) {
   val context = LocalContext.current

   val highlightNextStop = Data.highlightNextStop

   val (_, sliderValue, trip, overriddenHeadsign, overriddenFirstStop, departureTime, timeOffset) =
      routeScheduleEntry
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
         if (overriddenHeadsign != null || overriddenFirstStop.isValid() || specialLabel != null)
            MaterialTheme.colorScheme.tertiary
         else MaterialTheme.colorScheme.primary

      val stops = Schedule.instance.stops?.list

      val firstVisibleItemIndex = if (highlightedStopIndex > 0) 1 else 0
      val state = rememberSaveable(highlightedStopIndex, saver = LazyListState.Saver) {
         LazyListState(firstVisibleItemIndex)
      }

      LaunchedEffect(Unit) {
         state.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Stop) {
               if (state.firstVisibleItemIndex == 1 && state.firstVisibleItemScrollOffset < 128) {
                  state.scrollToItem(1, scrollOffset = 0)
               }
            }
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
         overriddenHeadsign != null ||
         overriddenFirstStop.isValid() ||
         specialLabel != null
      )
         Row(
            modifier = Modifier
               .fillMaxWidth()
               .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
         ) {
            if (isAtFirstStop)
               Text(
                  if (departureTime >= 0) "kreće u ${departureTime.timeToString()}"
                  else "kreće za ${(-departureTime - 1) / 60} min"
               )
            else if (overriddenFirstStop.isValid())
            // do not display the first stop if stopSequence == 1 because then it is already highlighted
               Text("polazište ${stops?.get(overriddenFirstStop)?.name.orLoading()}")
            else
            // blank box take up space
               Box(Modifier.size(0.dp))

            specialLabel?.first?.let { Text(it) }

            if (specialLabel?.second != null || overriddenHeadsign != null)
               Text(specialLabel?.second ?: "smjer $overriddenHeadsign", textAlign = TextAlign.End)
         }
   }
}