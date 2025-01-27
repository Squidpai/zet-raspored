package hr.squidpai.zetlive.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.TripId
import hr.squidpai.zetlive.LOADING_TEXT
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.gtfs.getUpdatingLiveDisplayData
import hr.squidpai.zetlive.localEpochDate
import hr.squidpai.zetlive.ui.composables.HintIconButton
import hr.squidpai.zetlive.ui.composables.IconButton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TripDialogActivity : ComponentActivity() {

	companion object {
		private const val TAG = "TripDialogActivity"

		fun show(context: Context, trip: Trip, selectedDate: Long) {
			context.startActivity(
				Intent(context, TripDialogActivity::class.java)
					.putExtra(EXTRA_ROUTE_ID, trip.route.id)
					.putExtra(EXTRA_TRIP_ID, trip.tripId)
					.putExtra(EXTRA_SELECTED_DATE, selectedDate)
			)
		}
	}

	private lateinit var routeId: RouteId
	private lateinit var tripId: TripId
	private var selectedDate = 0L

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		if (isGranted) {
			startNotificationTracking()
			finish()
		} else
			Toast.makeText(
				this,
				"Ne može se postaviti obavijest${Typography.mdash}" +
						"odbijena je dozvola za postavljanje obavijesti.",
				Toast.LENGTH_LONG
			).show()
	}

	private fun trackInNotifications() {
		if (ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED
		) {
			startNotificationTracking()
			finish()
			return
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
	}

	private fun startNotificationTracking() {
		startForegroundService(
			Intent(this, NotificationTrackerService::class.java)
				.putExtra(EXTRA_ROUTE_ID, routeId)
				.putExtra(EXTRA_TRIP_ID, tripId)
				.putExtra(EXTRA_SELECTED_DATE, selectedDate)
		)
	}

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

		setContent {
			AppTheme {
				var isAbsoluteTime by remember { mutableStateOf(true) }

				val schedule = ScheduleManager.instance
				val route = schedule?.routes?.get(routeId)
				val trip = route?.trips?.get(tripId)

				AlertDialog(
					onDismissRequest = ::finish,
					confirmButton = {
						TextButton(onClick = ::finish) {
							Text("Zatvori")
						}
					},
					title = {
						Row(verticalAlignment = Alignment.CenterVertically) {
							Text(
								text = if (route != null && trip != null)
									"${route.shortName} smjer ${trip.headsign}"
								else LOADING_TEXT,
								modifier = Modifier.weight(1f),
							)

							val icon = if (isAbsoluteTime) Symbols.ClockLoader10 else Symbols.Schedule
							IconButton(
								icon, "Promjeni prikaz vremena",
								onClick = { isAbsoluteTime = !isAbsoluteTime },
							)
							Box {
								var expanded by remember { mutableStateOf(false) }

								IconButton(
									Symbols.MoreVert, "Dodatne opcije",
									onClick = { expanded = true },
								)
								DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
									DropdownMenuItem(
										text = { Text("Prati u obavijestima") },
										onClick = { trackInNotifications() }
									)
								}
							}
						}
					},
					text = {
						if (schedule == null || trip == null) {
							CircularProgressIndicator()
							return@AlertDialog
						}

						DialogContent(trip, isAbsoluteTime)
					}
				)
			}
		}
	}

	override fun onPause() {
		super.onPause()
		ScheduleManager.realtimeDispatcher.removeListener(TAG)
	}

	override fun onResume() {
		super.onResume()
		ScheduleManager.realtimeDispatcher.addListener(TAG)
	}

	@Composable
	private fun DialogContent(trip: Trip, isAbsoluteTime: Boolean) {
		val (realtimeDepartures, timeOfDay, nextStopIndex, nextStopValue) =
			getUpdatingLiveDisplayData(trip, selectedDate)
		val isCancelled = realtimeDepartures == null

		LazyColumn(
			modifier = Modifier.fillMaxWidth(),
			state = rememberLazyListState(
				(nextStopIndex - 4).coerceAtLeast(0)
			),
		) {
			items(trip.stops.size) { i ->
				val stop = trip.stops[i]
				val departure = TimeOfDay(trip.departures[i])
				val realtimeDeparture = realtimeDepartures?.get(i)
					?.let { TimeOfDay(it) } ?: departure

				Layout(
					content = {
						LineCanvas(
							index = i,
							nextStopIndex,
							nextStopValue,
							isCancelled,
							lastIndex = trip.stops.lastIndex,
						)

						Column {
							val passed = i < nextStopIndex

							val stopColor: Color
							val timeColor: Color
							if (passed || isCancelled) {
								stopColor = lerp(
									MaterialTheme.colorScheme.onSurface,
									MaterialTheme.colorScheme.surface,
									fraction = .36f
								)
								timeColor = stopColor
							} else {
								stopColor = Color.Unspecified
								timeColor = MaterialTheme.colorScheme.primary
							}
							Text(stop.name, color = stopColor)
							Text(
								buildAnnotatedString {
									val t = realtimeDeparture.minusSeconds(timeOfDay)
									if (isCancelled) {
										withStyle(
											SpanStyle(
												textDecoration = TextDecoration.LineThrough,
												fontWeight = FontWeight.Normal
											)
										) {
											append(departure.toStringHHMM())
										}
										append(" otkazano")
									} else if (!isAbsoluteTime) {
										append(if (t < 0) "prije " else "za ")
										append((abs(t) / 60 % 60).toString())
										append(" min")
									} else {
										if (realtimeDeparture.minusMinutes(departure) != 0) {
											append(realtimeDeparture.toStringHHMM())
											append(' ')
											withStyle(
												SpanStyle(
													textDecoration = TextDecoration.LineThrough,
													fontWeight = FontWeight.Normal
												)
											) {
												append(departure.toStringHHMM())
											}
										} else append(departure.toStringHHMM())
									}
								},
								color = timeColor,
								fontWeight = FontWeight.Bold,
								style = MaterialTheme.typography.bodySmall,
							)
						}

						if (trip.departures !== realtimeDepartures && i == (nextStopIndex - 1).coerceAtLeast(0))
							HintIconButton(
								Symbols.MyLocation,
								contentDescription = null,
								tooltipTitle = "GPS praćenje",
								tooltipText = "Ovom vozilu moguće je pratiti lokaciju. " +
										"Raspored prikazan ovdje prilagođen je prema " +
										"lokaciji vozila."
							)
					},
					modifier = Modifier
						.fillMaxWidth()
						.clip(MaterialTheme.shapes.large)
						.clickable {
							startActivity(
								Intent(
									this@TripDialogActivity,
									StopScheduleActivity::class.java,
								).putExtra(StopScheduleActivity.EXTRA_STOP, stop.id.rawValue)
							)
						},
					measurePolicy = TripRowMeasurePolicy,
				)
			}
		}
	}

	@Composable
	private fun LineCanvas(
		index: Int,
		nextStopIndex: Int,
		nextStopValue: Float,
		isCancelled: Boolean,
		lastIndex: Int,
	) {
		val notFilled: Color
		val filled: Color

		if (isCancelled) {
			notFilled = lerp(
				MaterialTheme.colorScheme.surface,
				MaterialTheme.colorScheme.onSurface,
				.36f,
			)
			filled = notFilled
		} else {
			notFilled = MaterialTheme.colorScheme.onSurface
			filled = MaterialTheme.colorScheme.primary
		}

		val fillCircle = index < nextStopIndex || index == 0 && !isCancelled
		val circleTint = if (fillCircle) filled else notFilled

		Canvas(Modifier) {
			val (width, height) = size
			if (index != 0) {
				val prefillRatio =
					((nextStopValue - (index - 0.5f)) * 2f).coerceIn(0f, 1f)
				val prefillLength = prefillRatio * (height / 2 - width / 6)
				if (prefillRatio != 1f) drawLine(
					color = notFilled,
					start = Offset(width / 2, prefillLength),
					end = Offset(width / 2, (height / 2 - width / 6)),
					strokeWidth = width / 16,
					cap = StrokeCap.Round,
				)
				if (prefillRatio != 0f) drawLine(
					color = filled,
					start = Offset(width / 2, 0f),
					end = Offset(width / 2, prefillLength),
					strokeWidth = width / 16,
					cap = StrokeCap.Round,
				)
			}
			drawCircle(
				color = circleTint,
				radius = width / 6,
				style = if (fillCircle) Fill else Stroke(width = width / 16),
			)
			if (index != lastIndex) {
				val prefillRatio =
					((nextStopValue - index) * 2f).coerceIn(0f, 1f)
				val prefillLength = prefillRatio * (height / 2 - width / 6)
				val beginY = (height / 2 + width / 6)
				if (prefillRatio != 1f) drawLine(
					color = notFilled,
					start = Offset(width / 2, beginY + prefillLength),
					end = Offset(width / 2, height),
					strokeWidth = width / 16,
					cap = StrokeCap.Round,
				)
				if (prefillRatio != 0f) drawLine(
					color = filled,
					start = Offset(width / 2, beginY),
					end = Offset(width / 2, beginY + prefillLength),
					strokeWidth = width / 16,
					cap = StrokeCap.Round,
				)
			}
		}
	}

	private data object TripRowMeasurePolicy : MeasurePolicy {
		override fun MeasureScope.measure(
			measurables: List<Measurable>,
			constraints: Constraints
		): MeasureResult {
			val px48 = 48.dp.roundToPx()

			val iconPlaceable = measurables.getOrNull(2)
				?.takeIf { constraints.maxWidth >= px48 * 3 }
				?.measure(Constraints.fixed(px48, min(px48, constraints.maxHeight)))

			val stopWidthOffset = if (iconPlaceable != null) px48 * 2 else px48

			val stopPlaceable = measurables[1].measure(
				constraints.copy(
					minWidth = (constraints.minWidth - stopWidthOffset).coerceAtLeast(0),
					maxWidth = (constraints.maxWidth - stopWidthOffset).coerceAtLeast(0),
				)
			)

			val height = max(stopPlaceable.height, px48)

			val canvasPlaceable = measurables[0].measure(Constraints.fixed(px48, height))

			return layout(constraints.maxWidth, height) {
				canvasPlaceable.place(0, 0)
				stopPlaceable.place(px48, (height - stopPlaceable.height) / 2)
				iconPlaceable?.place(constraints.maxWidth - px48, (height - px48) / 2)
			}
		}
	}

}