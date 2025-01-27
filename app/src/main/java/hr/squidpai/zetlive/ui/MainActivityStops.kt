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
import androidx.compose.runtime.remember
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
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.StopNumber
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.filter
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.extractInt
import hr.squidpai.zetlive.gtfs.ActualStopLiveSchedule
import hr.squidpai.zetlive.gtfs.StopNoLiveSchedule
import hr.squidpai.zetlive.gtfs.getLiveSchedule
import hr.squidpai.zetlive.gtfs.iconInfo
import hr.squidpai.zetlive.gtfs.label
import hr.squidpai.zetlive.ui.composables.IconButton

@Composable
fun MainActivityStops(groupedStops: Map<StopNumber, Stops.Grouped>) =
	Column(Modifier.fillMaxSize()) {
		val inputState = rememberSaveable { mutableStateOf("") }

		val pinnedStops = Data.pinnedStops.toSet()
		val list = remember(key1 = groupedStops, key2 = pinnedStops) {
			mutableStateListOf<Stops.Grouped>().apply {
				addAll(groupedStops.values.filter(inputState.value.trim()))
			}
		}

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
					val stop = groupedStops[pinnedStopId] ?: continue
					item(key = -pinnedStopId) {
						StopContent(
							stop, pinned = true,
							modifier = Modifier
								.fillParentMaxWidth()
								.animateItem(),
						)
					}
				}

			items(list.size, key = { list[it].parentStop.id.rawValue }) {
				val stop = list[it]
				StopContent(
					stop, pinned = stop.parentStop.id.stopNumber in pinnedStops,
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
	groupedStops: Map<StopNumber, Stops.Grouped>,
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
				else groupedStops.values.filter(newInputTrimmed)

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
	modifier: Modifier,
) {
	val (expanded, setExpanded) =
		rememberSaveable(key = "st${groupedStop.parentStop.id.rawValue}") {
			mutableStateOf(
				false
			)
		}

	Surface(
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
						groupedStop.parentStop.name,
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
						val color = lerp(
							MaterialTheme.colorScheme.onSurface,
							MaterialTheme.colorScheme.surface,
							fraction = .36f
						)

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
							val index =
								labeledStops.indexOfFirst { it.stop.code == preferredCode }
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
					items(groupedStop.size) {
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
									StopScheduleActivity.EXTRA_STOP,
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
			liveSchedule.noLiveMessage ?: "Na postaji nema više polazaka danas.",
			Modifier
				.padding(vertical = 8.dp)
				.align(Alignment.CenterHorizontally)
		)

		is ActualStopLiveSchedule -> {
			val context = LocalContext.current

			for (entry in liveSchedule) Row(
				Modifier
					.clickable {
						TripDialogActivity.show(
							context,
							entry.trip,
							entry.selectedDate
						)
					}
					.padding(vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				val routeStyle = MaterialTheme.typography.titleMedium
				Text(
					text = entry.route.id,
					modifier = Modifier.width(with(LocalDensity.current) { (routeStyle.fontSize * 3.5f).toDp() }),
					color = MaterialTheme.colorScheme.primary,
					textAlign = TextAlign.Center,
					style = routeStyle,
				)
				Text(
					entry.headsign,
					Modifier.weight(1f),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
				Text(
					if (entry.useRelative) "${(entry.relativeTime.coerceAtLeast(0)) / 60} min"
					else entry.absoluteTime.toStringHHMM(),
					modifier = Modifier.padding(end = 4.dp),
					color = MaterialTheme.colorScheme.primary,
					fontWeight = FontWeight.Bold,
				)
			}
		}
	}
}