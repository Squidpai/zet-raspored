package hr.squidpai.zetlive.ui

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.Routes
import hr.squidpai.zetapi.filter
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.gtfs.ActualRouteLiveSchedule
import hr.squidpai.zetlive.gtfs.RouteNoLiveSchedule
import hr.squidpai.zetlive.gtfs.getLiveSchedule
import hr.squidpai.zetlive.gtfs.preferredName
import hr.squidpai.zetlive.mutableStateSetSaver
import hr.squidpai.zetlive.ui.composables.DirectionRow
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.ui.composables.LiveTravelSlider
import hr.squidpai.zetlive.withRemovedKeys

@Composable
fun MainActivityRoutes(routes: Routes) = Column(Modifier.fillMaxSize()) {
    val inputState = rememberSaveable { mutableStateOf("") }

    val pinnedRoutes = Data.pinnedRoutes.toSet()
    val resortedRoutes = remember(routes, pinnedRoutes) {
        val resortedRoutes = mutableListOf<Route>()

        for (pinnedRouteId in pinnedRoutes)
            routes[pinnedRouteId]?.let { resortedRoutes += it }

        resortedRoutes += routes.withRemovedKeys(pinnedRoutes).values

        resortedRoutes
    }

    val list = remember(resortedRoutes) {
        mutableStateListOf<Route>().apply {
            addAll(resortedRoutes.filter(inputState.value.trim()))
        }
    }

    val lazyListState = rememberLazyListState()

    RouteFilterSearchBar(
        inputState, resortedRoutes, list, lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )

    val expandedRoutes = rememberSaveable(saver = mutableStateSetSaver()) {
        mutableStateSetOf<RouteId>()
    }

    LazyColumn(state = lazyListState) {
        items(list.size, key = { list[it].id }) {
            val route = list[it]
            RouteContent(
                route, pinned = route.id in pinnedRoutes,
                expanded = route.id in expandedRoutes,
                setExpanded = { isNowExpanded: Boolean ->
                    if (isNowExpanded)
                        expandedRoutes += route.id
                    else
                        expandedRoutes -= route.id
                },
                modifier = Modifier
                    .fillParentMaxWidth()
                    .animateItem(),
            )
        }
    }

}

@Composable
private fun RouteFilterSearchBar(
    inputState: MutableState<String>,
    originalRouteList: List<Route>,
    list: SnapshotStateList<Route>,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val (input, setInput) = inputState

    val updateInput = updateInput@{ newInput: String ->
        if (newInput.length > 100) return@updateInput
        setInput(newInput)

        val newInputTrimmed = newInput.trim()
        val oldInputTrimmed = input.trim()

        if (newInputTrimmed != oldInputTrimmed) {
            lazyListState.requestScrollToItem(0)

            val newList =
                if (input in newInput) list.filter(newInputTrimmed)
                else originalRouteList.filter(newInputTrimmed)

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
private fun RouteContent(
    route: Route,
    pinned: Boolean,
    expanded: Boolean,
    setExpanded: (Boolean) -> Unit,
    modifier: Modifier,
) = Surface(
    modifier = modifier
        .padding(4.dp)
        .animateContentSize(),
    tonalElevation = if (expanded) 2.dp else 0.dp,
) {
    val context = LocalContext.current

    Column {
        Row(
            modifier = Modifier
                .clickable { setExpanded(!expanded) }
                .minimumInteractiveComponentSize(),
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
            Text(route.preferredName, Modifier.weight(1f))

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
            val directionState =
                rememberSaveable { mutableIntStateOf(Data.getDirectionForRoute(route.id)) }

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

@Composable
private fun ColumnScope.RouteLiveTravels(route: Route, directionState: MutableIntState) {
    val liveSchedule = route.getLiveSchedule()

    if (liveSchedule == null) {
        CircularProgressIndicator(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        )
        return
    }

    when (liveSchedule) {
        is RouteNoLiveSchedule -> Text(
            liveSchedule.noLiveMessage,
            Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        is ActualRouteLiveSchedule -> {
            val (direction, setDirection) = directionState

            val isRoundRoute =
                if (liveSchedule.first.isNotEmpty()) liveSchedule.second.isEmpty()
                else liveSchedule.commonHeadsign.second.isEmpty()

            DirectionRow(
                routeId = route.id,
                commonHeadsign = liveSchedule.commonHeadsign,
                direction, setDirection,
                isRoundRoute,
            )

            val liveTravels =
                if (direction == 0 || isRoundRoute) liveSchedule.first
                else liveSchedule.second

            if (liveTravels.isEmpty())
                Text(
                    "Linija nema više trenutno polazaka.",
                    Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            else for (entry in liveTravels)
                LiveTravelSlider(entry)
        }
    }
}
