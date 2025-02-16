package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.squidpai.zetapi.CalendarDates
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.ServiceId
import hr.squidpai.zetapi.ServiceType
import hr.squidpai.zetapi.ServiceTypes
import hr.squidpai.zetapi.TimeOfDay
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.filterByServiceId
import hr.squidpai.zetapi.findFirstDeparture
import hr.squidpai.zetapi.sortedByDepartures
import hr.squidpai.zetapi.splitByDirection
import hr.squidpai.zetlive.LOADING_TEXT
import hr.squidpai.zetlive.MILLIS_IN_DAY
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.gtfs.contentColor
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.timeToString
import hr.squidpai.zetlive.ui.composables.CircularLoadingBox
import hr.squidpai.zetlive.ui.composables.DirectionRow
import hr.squidpai.zetlive.ui.composables.IconButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RouteScheduleActivity : ComponentActivity() {

   companion object {
      private const val TAG = "RouteScheduleActivity"

      /**
       * Array whose contents is `["Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned"]`
       */
      private val croatianShortDaysOfWeek = arrayOf(
         "Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned"
      )
   }

   private val measuredTime = localCurrentTimeMillis()
   private val todaysDate = measuredTime / MILLIS_IN_DAY
   private val todaysTime = (measuredTime % MILLIS_IN_DAY).toInt()
   private val cachedDateStrings = Array(11) {
      val localDate = LocalDate.ofEpochDay(todaysDate + it - 2)
      "${croatianShortDaysOfWeek[localDate.dayOfWeek.ordinal]} ${localDate.dayOfMonth}.${localDate.monthValue}."
   }

   private fun getLabel(differenceFromToday: Int) =
      cachedDateStrings[differenceFromToday + 2]

   private val selectedDirection = mutableIntStateOf(0)

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      val routeId = intent.getStringExtra(EXTRA_ROUTE_ID)

      if (routeId == null) {
         Log.w(TAG, "onCreate: No route id given, finishing activity early.")

         finish()
         return
      }

      if (intent.getIntExtra(EXTRA_DIRECTION, 0) == 1)
         selectedDirection.intValue = 1

      enableEdgeToEdge()
      setContent {
         AppTheme {
            val schedule = ScheduleManager.instance.collectAsState().value

            val route = schedule?.routes?.get(routeId)

            Scaffold(topBar = {
               MyTopAppBar(
                  when {
                     route != null -> "${route.shortName} ${route.longName}"
                     schedule == null -> "$routeId $LOADING_TEXT"
                     else -> routeId
                  }
               )
            }) { padding ->
               if (schedule != null && route != null)
                  MyContent(
                     route = route,
                     serviceTypes = schedule.serviceTypes,
                     calendarDates = schedule.calendarDates,
                     modifier = Modifier.padding(padding),
                  )
               else CircularLoadingBox(Modifier.padding(padding))
            }
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

   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun MyTopAppBar(titleText: String) = TopAppBar(
      title = {
         Text(
            titleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
         )
      },
      navigationIcon = {
         IconButton(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Natrag",
            onClick = { finish() }
         )
      },
      windowInsets = WindowInsets.safeDrawing
         .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
   )

   @Composable
   private fun MyContent(
      route: Route,
      serviceTypes: ServiceTypes?,
      calendarDates: CalendarDates,
      modifier: Modifier = Modifier,
   ) = Column(modifier) {
      val yesterdayServiceId = calendarDates[todaysDate - 1]
      val isYesterdayUnfinished = yesterdayServiceId != null &&
            route.trips.values.any {
               it.serviceId == yesterdayServiceId &&
                     it.departures.last() - 1 * SECONDS_IN_DAY > todaysTime / MILLIS_IN_SECONDS
            }
      val todayTabOffset = if (isYesterdayUnfinished) 2 else 1
      val serviceIds = calendarDates.relativeSubRange(
         todaysDate,
         if (isYesterdayUnfinished) -2..7 else -1..7
      )

      val pagerState = rememberPagerState(initialPage = 1) {
         if (isYesterdayUnfinished) 10 else 9
      }

      val scope = rememberCoroutineScope()

      ServiceTabRow(
         serviceIds,
         todayTabOffset,
         selectedTabIndex = pagerState.currentPage,
         onSetSelectedTabIndex = {
            scope.launch { pagerState.animateScrollToPage(it) }
         },
         serviceTypes,
      )

      HorizontalPager(pagerState, verticalAlignment = Alignment.Top) {
         val serviceId = serviceIds.getOrNull(it)
         val date = todaysDate + it - todayTabOffset

         val filteredTrips =
            if (serviceId != null)
               route.trips
                  .filterByServiceId(serviceId)
                  .splitByDirection()
                  .sortedByDepartures()
            else // this date does not have a schedule associated with it
               emptyList<Trip>() to emptyList()

         DateContent(
            route,
            tripsLists = filteredTrips,
            selectedServiceId = serviceId,
            selectedDate = date,
            serviceTypes = serviceTypes,
         )
      }
   }

   @Composable
   private fun DateContent(
      route: Route,
      tripsLists: Pair<List<Trip>, List<Trip>>,
      selectedServiceId: ServiceId?,
      selectedDate: Long,
      serviceTypes: ServiceTypes?,
   ) = Column {
      val (direction, setDirection) = selectedDirection

      val commonHeadsigns = route.commonHeadsigns[selectedServiceId]

      val currentTime = localCurrentTimeMillis().milliseconds
      val timeOffset = selectedDate.days

      val states = remember(key1 = System.identityHashCode(tripsLists)) {
         val timeOfDay = TimeOfDay(currentTime - timeOffset)
         LazyListState(tripsLists.first.findFirstDeparture(timeOfDay)) to
               LazyListState(tripsLists.second.findFirstDeparture(timeOfDay))
      }

      val isRoundRoute =
         if (tripsLists.first.isNotEmpty()) tripsLists.second.isEmpty()
         else commonHeadsigns?.second?.isEmpty() ?: false

      val list: List<Trip>
      val state: LazyListState
      if (direction == 0 || isRoundRoute) {
         list = tripsLists.first
         state = states.first
      } else {
         list = tripsLists.second
         state = states.second
      }

      if (commonHeadsigns != null)
         DirectionRow(
            route.id,
            commonHeadsigns,
            direction,
            setDirection,
            isRoundRoute
         )

      if (list.isEmpty())
         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val specialLabel =
               Love.giveMeTheSpecialLabelForNoTrips(
                  route,
                  selectedServiceId,
                  selectedDate,
                  serviceTypes,
               )
            Text(specialLabel, Modifier.padding(horizontal = 16.dp))
         }
      else LazyColumn(state = state) {
         val firstOfNextDay =
            list.indexOfFirst { TimeOfDay(it.departures.first()).isTomorrow() }

         items(list.size) { i ->
            val trip = list[i]

            if (i == firstOfNextDay) {
               HorizontalDivider(Modifier.padding(horizontal = 4.dp))
               Text(
                  getLabel((selectedDate + 1 - todaysDate).toInt()),
                  Modifier
                     .fillMaxWidth()
                     .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                     ),
                  textAlign = TextAlign.Center,
                  style = MaterialTheme.typography.titleMedium,
               )
            }
            HorizontalDivider(Modifier.padding(horizontal = 4.dp))
            TripRow(
               trip,
               liveComparison = when {
                  currentTime < trip.departures.first().seconds + timeOffset -> 1
                  currentTime < trip.departures.last().seconds + timeOffset -> 0
                  else -> -1
               },
               selectedDate
            )
         }
      }
   }

   @Composable
   private fun TripRow(
      trip: Trip,
      liveComparison: Int,
      selectedDate: Long,
      modifier: Modifier = Modifier,
   ) {
      val specialLabel = Love.giveMeTheSpecialTripLabel(trip)

      Layout(
         content = {
            val tint = when {
               liveComparison < 0 -> lerp(
                  MaterialTheme.colorScheme.onSurface,
                  MaterialTheme.colorScheme.surface,
                  fraction = .36f
               )

               liveComparison == 0 -> MaterialTheme.colorScheme.primary
               else -> MaterialTheme.colorScheme.onSurface
            }
            val weight = if (liveComparison == 0) FontWeight.SemiBold else null

            val startLabel = specialLabel?.first
               ?: if (!trip.isFirstStopCommon) trip.stops.first().name else null

            if (startLabel != null) Text(
               text = startLabel,
               color = tint,
               fontWeight = weight,
               textAlign = TextAlign.Center,
               maxLines = 3,
            )

            Row(
               Modifier
                  .padding(horizontal = 8.dp)
                  .layoutId(TripRowMeasurePolicy.TIME_LAYOUT_ID),
               verticalAlignment = Alignment.CenterVertically
            ) {
               Text(
                  text = trip.departures.first().timeToString(),
                  color = tint,
                  fontWeight = weight,
                  textAlign = TextAlign.End,
               )
               Icon(
                  Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.padding(horizontal = 8.dp),
                  tint = tint,
               )
               Text(
                  trip.departures.last().timeToString(),
                  color = tint,
                  fontWeight = weight
               )
            }

            val endLabel = specialLabel?.second
               ?: if (!trip.isHeadsignCommon) trip.headsign else null

            if (endLabel != null) Text(
               text = endLabel,
               color = tint,
               fontWeight = weight,
               textAlign = TextAlign.Center,
               maxLines = 3,
            )
         },
         modifier = modifier.clickable {
            TripDialogActivity.show(this, trip, selectedDate)
         },
         measurePolicy = TripRowMeasurePolicy,
      )
   }

   private data object TripRowMeasurePolicy : MeasurePolicy {

      const val TIME_LAYOUT_ID = "time"

      override fun MeasureScope.measure(
         measurables: List<Measurable>,
         constraints: Constraints
      ): MeasureResult {
         if (measurables.size == 1) {
            val timePlaceable = measurables[0].measure(constraints)

            val actualHeight = max(48.dp.roundToPx(), timePlaceable.height)
            val yOffset = (actualHeight - timePlaceable.height) / 2

            return layout(constraints.maxWidth, actualHeight) {
               timePlaceable.place(
                  (constraints.maxWidth - timePlaceable.width) / 2,
                  yOffset
               )
            }
         }

         val timeIndex =
            measurables.indexOfFirst { TIME_LAYOUT_ID == it.layoutId }

         val timePlaceable = measurables[timeIndex].measure(constraints)

         val leftTextMeasurable =
            if (timeIndex != 0) measurables.first() else null
         val rightTextMeasurable =
            if (timeIndex != measurables.lastIndex) measurables.last() else null

         if (constraints.maxWidth - timePlaceable.width >= 180.dp.roundToPx()) {
            val maxTextWidth = (constraints.maxWidth - timePlaceable.width) / 2

            val leftPlaceable =
               leftTextMeasurable?.measure(constraints.copy(maxWidth = maxTextWidth))
            val rightPlaceable =
               rightTextMeasurable?.measure(constraints.copy(maxWidth = maxTextWidth))

            var actualHeight = 48.dp.roundToPx()
            if (timePlaceable.height > actualHeight) actualHeight =
               timePlaceable.height
            if (leftPlaceable != null && leftPlaceable.height > actualHeight) actualHeight =
               leftPlaceable.height
            if (rightPlaceable != null && rightPlaceable.height > actualHeight) actualHeight =
               rightPlaceable.height

            return layout(constraints.maxWidth, actualHeight) {
               leftPlaceable?.place(
                  ((constraints.maxWidth - timePlaceable.width) / 2 - leftPlaceable.width) / 2,
                  (actualHeight - leftPlaceable.height) / 2,
               )
               timePlaceable.place(
                  (constraints.maxWidth - timePlaceable.width) / 2,
                  (actualHeight - timePlaceable.height) / 2,
               )
               rightPlaceable?.place(
                  (constraints.maxWidth + timePlaceable.width) / 2 + ((constraints.maxWidth - timePlaceable.width) / 2 - rightPlaceable.width) / 2,
                  (actualHeight - rightPlaceable.height) / 2,
               )
            }
         }

         val leftPlaceable = leftTextMeasurable?.measure(constraints)
         val rightPlaceable = rightTextMeasurable?.measure(constraints)

         val height =
            (leftPlaceable?.height
               ?: 0) + timePlaceable.height + (rightPlaceable?.height ?: 0)

         return layout(constraints.maxWidth, height) {
            leftPlaceable?.place(0, 0)
            timePlaceable.place(
               (constraints.maxWidth - timePlaceable.width) / 2,
               leftPlaceable?.height ?: 0
            )
            rightPlaceable?.place(
               constraints.maxWidth - rightPlaceable.width,
               (leftPlaceable?.height ?: 0) + timePlaceable.height,
            )
         }
      }
   }

   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun ServiceTabRow(
      serviceIds: List<ServiceId?>,
      todayTabOffset: Int,
      selectedTabIndex: Int,
      onSetSelectedTabIndex: (Int) -> Unit,
      serviceTypes: ServiceTypes?,
      modifier: Modifier = Modifier,
   ) = PrimaryScrollableTabRow(
      selectedTabIndex, modifier,
      indicator = {
         TabRowDefaults.PrimaryIndicator(
            Modifier.tabIndicatorOffset(
               selectedTabIndex,
               matchContentSize = true
            ),
            width = Dp.Unspecified,
            color = (serviceTypes?.get(serviceIds[selectedTabIndex])
               ?: ServiceType.WEEKDAY).contentColor
         )
      }
   ) {
      serviceIds.forEachIndexed { i, serviceId ->
         val type = serviceTypes?.get(serviceId) ?: ServiceType.WEEKDAY

         Tab(
            selected = selectedTabIndex == i,
            onClick = { onSetSelectedTabIndex(i) },
            text = { Text(getLabel(i - todayTabOffset)) },
            selectedContentColor = type.contentColor,
         )
      }
   }

}