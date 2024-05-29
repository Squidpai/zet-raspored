package hr.squidpai.zetlive.ui

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.gtfs.*
import hr.squidpai.zetlive.orLoading
import hr.squidpai.zetlive.timeToString
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.text.Typography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainActivityRoutes() = Column {
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

  val updateInput = { newInput: String ->
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
    label = { Text("Pretraži linije") },
    leadingIcon = { Icon(Symbols.Search, null) },
    trailingIcon = {
      if (input.isNotEmpty()) IconButton(onClick = { updateInput("") }) {
        Icon(Symbols.Close, "Izbriši unos")
      }
    },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions { keyboardController?.hide() },
  )
}

// TODO check why it lags so much on first scroll
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
        Text(
          route.shortName,
          modifier = Modifier.width(48.dp),
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(route.longName, Modifier.weight(1f))

        if (expanded || pinned)
          IconButton(onClick = {
            Data.updateData {
              if (route.id !in pinnedRoutes) pinnedRoutes += route.id
              else pinnedRoutes -= route.id
            }
          }) {
            Icon(if (pinned) Symbols.PushPinFilled else Symbols.PushPin, "Pinaj")
          }
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
                .putExtra(RouteScheduleActivity.EXTRA_ROUTE, route.id)
                .putExtra(RouteScheduleActivity.EXTRA_DIRECTION, directionState.intValue)
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

  DirectionRow(commonHeadsign = routeLiveSchedule?.commonHeadsign, direction, setDirection)

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

  LaunchedEffect(Unit) {
    val schedule = Schedule.instance
    schedule.stops?.let { schedule.getTripsOfRoute(route.id).value?.printSpecialTripShapes(it) }
  }

  if (liveTravels.isEmpty()) {
    Text(
      "Linija danas više nema polazaka.",
      Modifier
        .padding(vertical = 8.dp)
        .align(Alignment.CenterHorizontally)
    )
    return
  }

  for (entry in liveTravels) {
    LiveTravelSlider(entry)
  }
}

@Composable
private fun LiveTravelSlider(routeScheduleEntry: RouteScheduleEntry) {
  val selectTrip = LocalSelectTrip.current

  val (name, stopSequence, sliderValue, trip, overriddenHeadsign, overriddenFirstStop, departureTime) =
    routeScheduleEntry

  val specialLabel = Love.giveMeTheSpecialTripLabel(trip)

  Column(
    Modifier
      .padding(vertical = 6.dp)
      .clickable { selectTrip(trip, 0L) },
  ) {
    val tint =
      if (overriddenHeadsign != null || overriddenFirstStop.isValid() || specialLabel != null)
        MaterialTheme.colorScheme.tertiary
      else MaterialTheme.colorScheme.primary

    val stops = Schedule.instance.stops?.list

    val state = rememberSaveable(stopSequence, saver = LazyListState.Saver) {
      LazyListState(firstVisibleItemIndex = max(stopSequence - 1, 0))
    }

    LaunchedEffect(Unit) {
      state.interactionSource.interactions.collect { interaction ->
        if (interaction is DragInteraction.Stop) {
          if (state.firstVisibleItemIndex == stopSequence - 1 && state.firstVisibleItemScrollOffset < 32) {
            state.scrollToItem(stopSequence - 1, scrollOffset = 0)
          }
        }
      }
    }

    if (stops == null) Text(name, color = tint)
    else LazyRow(
      modifier = Modifier.height(40.dp),
      state = state,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items(trip.stops.size) {
        if (it + 1 == stopSequence) {
          if (stopSequence != 1) Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.padding(start = 8.dp),
            contentDescription = null,
            tint = tint
          )
          Text(
            text = name,
            modifier = Modifier.padding(horizontal = 8.dp),
            fontWeight = FontWeight.Medium,
          )
        } else {
          val stopName = stops[trip.stops[it].toStopId()]?.name.orLoading()
          Text(
            text = if (it == 0) stopName else "${Typography.bullet} $stopName",
            color = lerp(MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface, .36f),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    RouteSlider(
      value = sliderValue,
      departures = trip.departures,
      modifier = Modifier.fillMaxWidth(),
      passedTrackColor = tint,
    )

    if (stopSequence == 1 ||
      overriddenHeadsign != null ||
      overriddenFirstStop.isValid() ||
      specialLabel != null)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (stopSequence == 1)
          Text(if (departureTime >= 0) departureTime.timeToString() else "za ${(-departureTime - 1) / 60} min")
        else if (overriddenFirstStop.isValid())
          // do not display the first stop if stopSequence == 1 because then it is already highlighted
          Text("polazište ${stops?.get(overriddenFirstStop)?.name.orLoading()}")
        else
          // blank box take up space
          Box(Modifier.size(0.dp))

        specialLabel?.first?.let { Text(it) }

        if (specialLabel?.second != null || overriddenHeadsign != null)
          Text(specialLabel?.second ?: "smjer $overriddenHeadsign")
      }
  }
}