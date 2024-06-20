package hr.squidpai.zetlive.ui

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import hr.squidpai.zetlive.*
import hr.squidpai.zetlive.gtfs.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainActivityStops() = Column(Modifier.fillMaxSize()) {
  val groupedStops = Schedule.instance.stops?.groupedStops

  val inputState = rememberSaveable { mutableStateOf("") }

  val pinnedStops = Data.pinnedStops.toSet()
  val list = remember(key1 = groupedStops, key2 = pinnedStops) {
    groupedStops?.let {
      mutableStateListOf<GroupedStop>().apply { addAll(it.filter(inputState.value.trim())) }
    }
  }

  if (groupedStops != null && list != null) {
    val lazyListState = rememberLazyListState()

    StopFilterSearchBar(
      inputState, groupedStops, list, lazyListState,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
    )

    LazyColumn(state = lazyListState) {
      if (inputState.value.isBlank())
        for (pinnedStopId in pinnedStops) {
          val stop = groupedStops[pinnedStopId.toParentStopId()] ?: continue
          item(key = -pinnedStopId) {
            StopContent(
              stop, pinned = true,
              modifier = Modifier
                .fillParentMaxWidth()
                .animateItemPlacement(),
            )
          }
        }

      items(list.size, key = { list[it].parentStop.id.value }) {
        val stop = list[it]
        StopContent(
          stop, pinned = stop.parentStop.id.stationNumber in pinnedStops,
          modifier = Modifier
            .fillParentMaxWidth()
            .animateItemPlacement(),
        )
      }
    }
  } else CircularLoadingBox()

}

@Composable
private fun StopFilterSearchBar(
  inputState: MutableState<String>,
  groupedStops: SortedListMap<StopId, GroupedStop>,
  list: SnapshotStateList<GroupedStop>,
  lazyListState: LazyListState,
  modifier: Modifier = Modifier,
) {
  val (input, setInput) = inputState

  val coroutineScope = rememberCoroutineScope()

  val updateInput = updateInput@{ newInput: String ->
    if (newInput.length > 100)
      return@updateInput

    setInput(newInput)

    val newInputTrimmed = newInput.trim()
    val oldInputTrimmed = input.trim()

    if (newInputTrimmed != oldInputTrimmed) {
      coroutineScope.launch { lazyListState.scrollToItem(0, 0) }

      val newList =
        if (input in newInput) list.filter(newInputTrimmed)
        else groupedStops.filter(newInputTrimmed)

      list.clear()
      list.addAll(newList)
    }
  }

  val keyboardController = LocalSoftwareKeyboardController.current

  OutlinedTextField(
    value = input,
    onValueChange = updateInput,
    modifier = modifier,
    label = { Text("Pretraži postaje", maxLines = 1, overflow = TextOverflow.Ellipsis) },
    leadingIcon = { Icon(Symbols.Search, null) },
    trailingIcon = {
      if (input.isNotEmpty())
        IconButton(Symbols.Close, "Izbriši unos") { updateInput("") }
    },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions { keyboardController?.hide() },
  )
}

data class LabelPair(val stop: Stop, val label: String?) : Comparable<LabelPair> {
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

fun GroupedStop.labeledStop(routesAtStopMap: RoutesAtStopMap?) = childStops
  .map { LabelPair(it, it.getLabel(routesAtStopMap)) }
  .sorted()

@Composable
private fun StopContent(groupedStop: GroupedStop, pinned: Boolean, modifier: Modifier) {
  val (expanded, setExpanded) =
    rememberSaveable(key = "st${groupedStop.parentStop.id.value}") { mutableStateOf(false) }

  Surface(
    modifier = modifier.padding(4.dp).animateContentSize(),
    tonalElevation = if (expanded) 2.dp else 0.dp,
  ) {
    Column {
      val schedule = Schedule.instance
      val routesAtStopMap = schedule.routesAtStopMap

      Row(
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .clickable { setExpanded(!expanded) },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          Modifier
            .weight(1f)
            .padding(4.dp)
        ) {
          Text(
            groupedStop.parentStop.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
          )

          val iconInfo = when (groupedStop.stopType) {
            StopType.Tram -> Symbols.Tram20 to "Tramvaji"
            StopType.Bus -> Symbols.Bus20 to "Autobusi"
            else -> null
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            val color = lerp(
              MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface, fraction = .36f
            )

            iconInfo?.let { Icon(iconInfo.first, iconInfo.second, Modifier.size(16.dp), tint = color) }
            Text(
              routesAtStopMap?.let { groupedStop.joinAllRoutesToString(it) }.orLoading(),
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
            if (pinned) "Otkvači s vrha popisa" else "Zakvači na vrh popisa"
          ) {
            Data.updateData {
              val id = groupedStop.parentStop.id.stationNumber
              if (id !in pinnedStops) pinnedStops += id
              else pinnedStops -= id
            }
          }
      }

      if (expanded) {
        val labeledStops = groupedStop.labeledStop(routesAtStopMap)

        val (selectedStopIndex, setSelectedStopIndex) = rememberSaveable {
          val preferredCode = Data.defaultStopCodes.getOrDefault(groupedStop.parentStop.id.stationNumber, 0)
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
          items(groupedStop.childStops.size) {
            val (stop, label) = labeledStops[it]
            FilterChip(
              selected = selectedStopIndex == it,
              onClick = {
                if (selectedStopIndex != it) {
                  setSelectedStopIndex(it)
                  Data.updateData { defaultStopCodes[groupedStop.parentStop.id.stationNumber] = stop.code }
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
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          val context = LocalContext.current

          OutlinedButton(onClick = {
            context.startActivity(Intent(context, StopScheduleActivity::class.java).apply {
              putExtra(StopScheduleActivity.EXTRA_STOP, selectedStop.id.value)
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

        routesAtStopMap?.let { StopLiveTravels(selectedStop, it) }
      }
    }
  }
}

@Composable
private fun ColumnScope.StopLiveTravels(stop: Stop, routesAtStopMap: RoutesAtStopMap) {
  val (live, errorMessage) = routesAtStopMap[stop.id.value]?.let {
    stop.getLiveSchedule(it, keepDeparted = false, maxSize = 8)
  }
    ?: return

  if (live.isNullOrEmpty()) {
    Text(
      errorMessage ?: "Na postaji nema više polazaka danas.",
      Modifier
        .padding(vertical = 8.dp)
        .align(Alignment.CenterHorizontally)
    )
    return
  }

  val selectTrip = LocalSelectTrip.current

  for ((routeNumber, headsign, trip, absoluteTime, relativeTime, useRelative) in live) {
    Row(
      Modifier
        .clickable { selectTrip(trip, 0L) }
        .padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val routeStyle = MaterialTheme.typography.titleMedium
      Text(
        text = routeNumber.toString(),
        modifier = Modifier.width(with(LocalDensity.current) { (routeStyle.fontSize * 3.5f).toDp() }),
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        style = routeStyle,
      )
      Text(headsign, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        if (useRelative) "${relativeTime / 60} min" else absoluteTime.timeToString(),
        modifier = Modifier.padding(end = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}