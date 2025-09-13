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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.StopNumber
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.filter
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.extractInt
import hr.squidpai.zetlive.gtfs.ActualStopLiveSchedule
import hr.squidpai.zetlive.gtfs.StopNoLiveSchedule
import hr.squidpai.zetlive.gtfs.StopScheduleEntry
import hr.squidpai.zetlive.gtfs.getLiveSchedule
import hr.squidpai.zetlive.gtfs.iconInfo
import hr.squidpai.zetlive.gtfs.label
import hr.squidpai.zetlive.gtfs.preferredHeadsign
import hr.squidpai.zetlive.gtfs.preferredName
import hr.squidpai.zetlive.mutableStateSetSaver
import hr.squidpai.zetlive.ui.composables.IconButton
import hr.squidpai.zetlive.ui.composables.disabled
import hr.squidpai.zetlive.withRemovedKeys

@Composable
fun MainActivityStops(groupedStops: Map<StopNumber, Stops.Grouped>) =
    Column(Modifier.fillMaxSize()) {
        val inputState = rememberSaveable { mutableStateOf("") }

        val pinnedStops = Data.pinnedStops.toSet()
        val resortedStops = remember(groupedStops, pinnedStops) {
            val resortedStops = mutableListOf<Stops.Grouped>()

            for (pinnedStopId in pinnedStops)
                groupedStops[pinnedStopId]?.let { resortedStops += it }

            resortedStops += groupedStops.withRemovedKeys(pinnedStops).values

            resortedStops
        }

        val list = remember(resortedStops) {
            mutableStateListOf<Stops.Grouped>().apply {
                addAll(resortedStops.filter(inputState.value.trim()))
            }
        }

        val lazyListState = rememberLazyListState()

        StopFilterSearchBar(
            inputState, resortedStops, list, lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )

        val expandedStops = rememberSaveable(saver = mutableStateSetSaver()) {
            mutableStateSetOf<StopNumber>()
        }

        LazyColumn(state = lazyListState) {
            items(list.size, key = { list[it].parentStop.id.rawValue }) {
                val stop = list[it]
                val stopNumber = stop.parentStop.id.stopNumber
                StopContent(
                    stop, pinned = stopNumber in pinnedStops,
                    expanded = stopNumber in expandedStops,
                    setExpanded = { isNowExpanded ->
                        if (isNowExpanded)
                            expandedStops += stopNumber
                        else
                            expandedStops -= stopNumber
                    },
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .animateItem(),
                )
            }
        }

    }

@Composable
private fun StopFilterSearchBar(
    inputState: MutableState<String>,
    originalStopList: List<Stops.Grouped>,
    list: SnapshotStateList<Stops.Grouped>,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val (input, setInput) = inputState

    val updateInput = updateInput@{ newInput: String ->
        if (newInput.length > 100)
            return@updateInput

        setInput(newInput)

        val newInputTrimmed = newInput.trim()
        val oldInputTrimmed = input.trim()

        if (newInputTrimmed != oldInputTrimmed) {
            lazyListState.requestScrollToItem(0)

            val newList =
                if (input in newInput) list.filter(newInputTrimmed)
                else originalStopList.filter(newInputTrimmed)

            list.clear()
            list.addAll(newList)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = input,
        onValueChange = updateInput,
        modifier = modifier,
        label = {
            Text(
                "Pretraži postaje",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Symbols.Search, null) },
        trailingIcon = {
            if (input.isNotEmpty())
                IconButton(
                    Symbols.Close,
                    "Izbriši unos",
                    onClick = { updateInput("") })
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions { keyboardController?.hide() },
    )
}

data class LabelPair(val stop: Stop, val label: String?) :
    Comparable<LabelPair> {
    override fun compareTo(other: LabelPair): Int {
        return when {
            this.label === other.label -> 0
            this.label == null -> -1
            other.label == null -> 1
            else -> {
                val firstCharComparison = this.label[0].compareTo(other.label[0])

                if (firstCharComparison != 0) firstCharComparison
                else {
                    val thisNum = this.label.extractInt()
                    val otherNum = other.label.extractInt()
                    if (thisNum != -1 || otherNum != -1) thisNum.compareTo(otherNum)
                    else this.label.compareTo(other.label)
                }
            }
        }
    }
}

fun Stops.Grouped.labeledStop() = map { LabelPair(it, it.label) }.sorted()

@Composable
private fun StopContent(
    groupedStop: Stops.Grouped,
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
    Column {
        Row(
            modifier = Modifier
                .clickable { setExpanded(!expanded) }
                .minimumInteractiveComponentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Text(
                    groupedStop.parentStop.preferredName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val iconInfo = when (groupedStop.routeType) {
                    Route.Type.Tram -> Symbols.Tram20 to "Tramvaji"
                    Route.Type.Bus -> Symbols.Bus20 to "Autobusi"
                    else -> null
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = MaterialTheme.colorScheme.disabled

                    iconInfo?.let {
                        Icon(
                            iconInfo.first,
                            iconInfo.second,
                            Modifier.size(16.dp),
                            tint = color
                        )
                    }
                    Text(
                        groupedStop.parentStop.allRoutesListed,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (expanded || pinned)
                IconButton(
                    if (pinned) Symbols.PushPinFilled else Symbols.PushPin,
                    if (pinned) "Otkvači s vrha popisa" else "Zakvači na vrh popisa",
                    onClick = {
                        Data.updateData {
                            val id = groupedStop.parentStop.id.stopNumber
                            if (id !in pinnedStops) pinnedStops += id
                            else pinnedStops -= id
                        }
                    }
                )
        }

        if (expanded) {
            val labeledStops = groupedStop.labeledStop()

            val (selectedStopIndex, setSelectedStopIndex) = rememberSaveable {
                val preferredCode =
                    Data.defaultStopCodes.getOrDefault(
                        groupedStop.parentStop.id.stopNumber,
                        0
                    )
                mutableIntStateOf(
                    if (preferredCode == 0) 0
                    else {
                        val index = labeledStops.indexOfFirst { it.stop.code == preferredCode }
                        if (index == -1) 0
                        else index
                    }
                )
            }
            val selectedStop = labeledStops[selectedStopIndex].stop

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items(labeledStops.size) {
                    val (stop, label) = labeledStops[it]
                    FilterChip(
                        selected = selectedStopIndex == it,
                        onClick = {
                            if (selectedStopIndex != it) {
                                setSelectedStopIndex(it)
                                Data.updateData {
                                    defaultStopCodes[groupedStop.parentStop.id.stopNumber] =
                                        stop.code
                                }
                            }
                        },
                        label = { Text(label ?: "Smjer") },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        trailingIcon = if (label == null) ({
                            stop.iconInfo?.let { iconInfo ->
                                Icon(iconInfo.first, iconInfo.second)
                            }
                        }) else null
                    )
                }
                /*if (labeledStops.size > 4) item {
                    Box {
                        var opened by remember { mutableStateOf(false) }

                        FilterChip(
                            selected = false,
                            onClick = { opened = !opened },
                            label = { Text(Typography.ellipsis.toString()) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
                        )

                        DropdownMenu(
                            expanded = opened,
                            onDismissRequest = { opened = false },
                        ) {
                            for (i in 3..<labeledStops.size) {
                                val (stop, label) = labeledStops[i]
                                DropdownMenuItem(
                                    text = { Text(label ?: "Smjer") },
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            stop.id.toString(),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        opened = false
                                        if (selectedStopIndex != i) {
                                            setSelectedStopIndex(i)
                                            //Data.updateData {
                                            Data.defaultStopCodes[groupedStop.parentStop.id.stopNumber] =
                                                stop.code
                                            //}
                                        }
                                    },
                                    trailingIcon = if (label == null) ({
                                        stop.iconInfo?.let { iconInfo ->
                                            Icon(iconInfo.first, iconInfo.second)
                                        }
                                    }) else null,
                                )
                            }
                        }
                    }
                }*/
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                val context = LocalContext.current

                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            StopScheduleActivity::class.java
                        ).apply {
                            putExtra(
                                EXTRA_STOP,
                                selectedStop.id.rawValue
                            )
                        })
                }) {
                    Text("Raspored")
                }
                /*TextButton(onClick = {
                  // TODO show map
                }) {
                  Text("Prikaži na karti")
                }*/
            }

            StopLiveTravels(selectedStop)
        }
    }
}

@Composable
private fun ColumnScope.StopLiveTravels(stop: Stop) {
    val liveSchedule = stop.getLiveSchedule(keepDeparted = false, maxSize = 8)

    if (liveSchedule == null) {
        CircularProgressIndicator(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        )
        return
    }

    when (liveSchedule) {
        is StopNoLiveSchedule -> Text(
            liveSchedule.noLiveMessage,
            Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        is ActualStopLiveSchedule -> {
            val context = LocalContext.current

            for (entry in liveSchedule)
                LiveStopRow(
                    stop,
                    entry,
                    Modifier
                        .clickable { showTripDialog(context, entry.trip, entry.selectedDate) }
                        .padding(vertical = 4.dp),
                )
        }
    }
}

@Composable
fun LiveStopRow(
    stop: Stop,
    entry: StopScheduleEntry,
    modifier: Modifier = Modifier,
) = Row(
    modifier,
    verticalAlignment = Alignment.CenterVertically,
) {
    val departed = entry.relativeTime < 0

    val tintColor: Color
    val regularColor: Color
    if (departed || entry.isCancelled) {
        tintColor = MaterialTheme.colorScheme.disabled
        regularColor = tintColor
    } else {
        tintColor = MaterialTheme.colorScheme.primary
        regularColor = Color.Unspecified
    }

    val isLastStop = entry.trip.stops.last() == stop

    val routeStyle = MaterialTheme.typography.titleMedium
    Text(
        text = entry.route.id,
        modifier = Modifier.width(with(LocalDensity.current) { (routeStyle.fontSize * 3.5f).toDp() }),
        color = tintColor,
        textAlign = TextAlign.Center,
        style = routeStyle,
    )
    Text(
        if (!isLastStop) entry.trip.preferredHeadsign
        else "IZLAZ",
        Modifier.weight(1f),
        color = regularColor,
        fontWeight = FontWeight.Bold.takeIf { isLastStop },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        when {
            entry.isCancelled -> "otkazano"
            entry.useRelative ->
                if (!departed) "${entry.relativeTime / 60} min"
                else "prije ${-entry.relativeTime / 60} min"
            else -> entry.absoluteTime.toStringHHMM()
        },
        modifier = Modifier.padding(end = 4.dp),
        color = tintColor,
        fontWeight = FontWeight.Bold.takeUnless { departed },
    )
}
