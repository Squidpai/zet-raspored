package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.LOADING_TEXT
import hr.squidpai.zetlive.MILLIS_IN_DAY
import hr.squidpai.zetlive.MILLIS_IN_SECONDS
import hr.squidpai.zetlive.SECONDS_IN_DAY
import hr.squidpai.zetlive.any
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Love
import hr.squidpai.zetlive.gtfs.Route
import hr.squidpai.zetlive.gtfs.RouteId
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.gtfs.ServiceId
import hr.squidpai.zetlive.gtfs.ServiceIdType
import hr.squidpai.zetlive.gtfs.ServiceIdTypes
import hr.squidpai.zetlive.gtfs.StopId
import hr.squidpai.zetlive.gtfs.Stops
import hr.squidpai.zetlive.gtfs.Trip
import hr.squidpai.zetlive.gtfs.Trips
import hr.squidpai.zetlive.gtfs.TripsList
import hr.squidpai.zetlive.gtfs.filterByServiceId
import hr.squidpai.zetlive.gtfs.findFirstDepartureTomorrow
import hr.squidpai.zetlive.gtfs.findFirstDepartures
import hr.squidpai.zetlive.gtfs.sortedByDepartures
import hr.squidpai.zetlive.gtfs.splitByDirection
import hr.squidpai.zetlive.gtfs.toStopId
import hr.squidpai.zetlive.localCurrentTimeMillis
import hr.squidpai.zetlive.timeToString
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

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

      val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)

      if (routeId == -1) {
         Log.w(TAG, "onCreate: No route id given, finishing activity early.")

         finish()
         return
      }

      if (intent.getIntExtra(EXTRA_DIRECTION, 0) == 1)
         selectedDirection.intValue = 1

      enableEdgeToEdge()
      setContent {
         AppTheme {
            val schedule = Schedule.instance

            val route = schedule.routes?.list?.get(key = routeId)
            val trips = schedule.getTripsOfRoute(routeId).value

            val commonFirstStop = trips?.commonFirstStop

            val serviceIdTypes = schedule.serviceIdTypes

            Scaffold(topBar = { MyTopAppBar(route) }) { padding ->
               MyContent(routeId, trips, commonFirstStop, serviceIdTypes, Modifier.padding(padding))
            }
         }
      }
   }

   override fun onPause() {
      super.onPause()
      Live.pauseLiveData()
   }

   override fun onResume() {
      super.onResume()
      Live.resumeLiveData()
   }

   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun MyTopAppBar(route: Route?) = TopAppBar(
      title = {
         Text(
            if (route != null) "${route.shortName} ${route.longName}" else LOADING_TEXT,
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

   @OptIn(ExperimentalFoundationApi::class)
   @Composable
   private fun MyContent(
      routeId: RouteId,
      trips: Trips?,
      commonFirstStops: Pair<StopId, StopId>?,
      serviceIdTypes: ServiceIdTypes?,
      modifier: Modifier = Modifier,
   ) = Column(modifier) {
      val calendarDates = Schedule.instance.calendarDates

      val yesterdayServiceId = calendarDates?.get(todaysDate - 1)
      val isYesterdayUnfinished = trips != null && yesterdayServiceId != null &&
            trips.list.any {
               it.serviceId == yesterdayServiceId &&
                     it.departures.last() - 1 * SECONDS_IN_DAY > todaysTime / MILLIS_IN_SECONDS
            }
      val todayTabOffset = if (isYesterdayUnfinished) 2 else 1
      val serviceIds = calendarDates?.relativeSubRange(
         todaysDate,
         if (isYesterdayUnfinished) -2..7 else -1..7
      ).orEmpty()

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
         serviceIdTypes,
      )

      val stops = Schedule.instance.stops

      HorizontalPager(pagerState, verticalAlignment = Alignment.Top) {
         val serviceId = serviceIds.getOrNull(it)
         val date = todaysDate + it - todayTabOffset

         val filteredStopTimes =
            if (serviceId != null && trips != null)
               trips.list
                  .filterByServiceId(serviceId)
                  .splitByDirection()
                  .sortedByDepartures()
            else if (trips != null) // this date does not have a schedule associated with it
               emptyList<Trip>() to emptyList()
            else null // trips not loaded, give null to display loading box

         DateContent(
            routeId, commonFirstStops, trips?.commonHeadsign,
            stopTimes = filteredStopTimes,
            stops = stops,
            selectedServiceId = serviceId,
            selectedDate = date,
            serviceIdTypes = serviceIdTypes,
            tripsList = trips?.list,
         )
      }
   }

   @Composable
   private fun DateContent(
      routeId: RouteId,
      commonFirstStops: Pair<StopId, StopId>?,
      commonHeadsigns: Pair<String, String>?,
      stopTimes: Pair<List<Trip>, List<Trip>>?,
      stops: Stops?,
      selectedServiceId: ServiceId?,
      selectedDate: Long,
      serviceIdTypes: ServiceIdTypes?,
      tripsList: TripsList?,
   ) = Column {
      val (direction, setDirection) = selectedDirection

      val currentTime = localCurrentTimeMillis() / MILLIS_IN_SECONDS
      val timeOffset = selectedDate * SECONDS_IN_DAY

      val states = remember(key1 = System.identityHashCode(stopTimes)) {
         stopTimes?.findFirstDepartures((currentTime - timeOffset).toInt())
            ?.let { (first, second) ->
               LazyListState(first) to LazyListState(second)
            }
      }

      val commonHeadsign: String?
      val commonFirstStop: StopId
      val list: List<Trip>?
      val state: LazyListState?

      val isRoundRoute = stopTimes != null &&
            (if (stopTimes.first.isNotEmpty()) stopTimes.second.isEmpty()
            else commonHeadsigns?.second?.isEmpty() ?: false)

      if (direction == 0 || isRoundRoute) {
         commonHeadsign = commonHeadsigns?.first
         commonFirstStop = commonFirstStops?.first ?: StopId.Invalid
         list = stopTimes?.first
         state = states?.first
      } else {
         commonHeadsign = commonHeadsigns?.second
         commonFirstStop = commonFirstStops?.second ?: StopId.Invalid
         list = stopTimes?.second
         state = states?.second
      }

      DirectionRow(commonHeadsigns, direction, setDirection, isRoundRoute)

      if (list == null)
         CircularLoadingBox()
      else if (list.isEmpty())
         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val specialLabel =
               Love.giveMeTheSpecialLabelForNoTrips(
                  routeId,
                  tripsList,
                  selectedServiceId,
                  selectedDate,
                  serviceIdTypes
               )
            Text(specialLabel, Modifier.padding(horizontal = 16.dp))
         }
      else LazyColumn(state = state!!) { // list != null  =>  stopTimes != null  =>  state != null
         val firstOfNextDay = list.findFirstDepartureTomorrow()

         items(list.size) { i ->
            val stopTime = list[i]
            val stopId = stopTime.stops.first().toStopId()

            val stop = if (stopId != commonFirstStop) stops?.list?.get(stopId) else null

            if (i == firstOfNextDay) {
               HorizontalDivider(Modifier.padding(horizontal = 4.dp))
               Text(
                  getLabel((selectedDate + 1 - todaysDate).toInt()),
                  Modifier
                     .fillMaxWidth()
                     .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                  textAlign = TextAlign.Center,
                  style = MaterialTheme.typography.titleMedium,
               )
            }
            HorizontalDivider(Modifier.padding(horizontal = 4.dp))
            TripRow(
               stopTime,
               stop?.name,
               stopTime.headsign.takeIf { it != commonHeadsign },
               liveComparison = when {
                  currentTime < stopTime.departures.first() + timeOffset -> 1
                  currentTime < stopTime.departures.last() + timeOffset -> 0
                  else -> -1
               },
               selectedDate
            )
         }
      }
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
               timePlaceable.place((constraints.maxWidth - timePlaceable.width) / 2, yOffset)
            }
         }

         val timeIndex = measurables.indexOfFirst { TIME_LAYOUT_ID == it.layoutId }

         val timePlaceable = measurables[timeIndex].measure(constraints)

         val leftTextMeasurable = if (timeIndex != 0) measurables.first() else null
         val rightTextMeasurable =
            if (timeIndex != measurables.lastIndex) measurables.last() else null

         if (constraints.maxWidth - timePlaceable.width >= 180.dp.roundToPx()) {
            val maxTextWidth = (constraints.maxWidth - timePlaceable.width) / 2

            val leftPlaceable =
               leftTextMeasurable?.measure(constraints.copy(maxWidth = maxTextWidth))
            val rightPlaceable =
               rightTextMeasurable?.measure(constraints.copy(maxWidth = maxTextWidth))

            var actualHeight = 48.dp.roundToPx()
            if (timePlaceable.height > actualHeight) actualHeight = timePlaceable.height
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
            (leftPlaceable?.height ?: 0) + timePlaceable.height + (rightPlaceable?.height ?: 0)

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

   @Composable
   private fun TripRow(
      trip: Trip,
      overriddenFirstStop: String?,
      overriddenHeadsign: String?,
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

            val startLabel = specialLabel?.first ?: overriddenFirstStop

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
               Text(trip.departures.last().timeToString(), color = tint, fontWeight = weight)
            }

            val endLabel = specialLabel?.second ?: overriddenHeadsign

            if (endLabel != null) Text(
               text = endLabel,
               color = tint,
               fontWeight = weight,
               textAlign = TextAlign.Center,
               maxLines = 3,
            )
         },
         modifier = modifier.clickable {
            TripDialogActivity.selectTrip(this, trip, selectedDate * MILLIS_IN_DAY)
         },
         measurePolicy = TripRowMeasurePolicy,
      )
   }

   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun ServiceTabRow(
      serviceIds: List<ServiceId?>,
      todayTabOffset: Int,
      selectedTabIndex: Int,
      onSetSelectedTabIndex: (Int) -> Unit,
      serviceIdTypes: ServiceIdTypes?,
      modifier: Modifier = Modifier,
   ) {
      // refresh service id on change of calendarDates
      /*LaunchedEffect(calendarDates) {
         onSetServiceId(
            SelectedServiceState(
               serviceId = serviceIds.getOrNull(selectedTabIndex),
               date = todaysDate + selectedTabIndex - todayTabOffset,
               index = selectedTabIndex,
            )
         )
      }*/

      PrimaryScrollableTabRow(
         selectedTabIndex, modifier,
         indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
               val width by animateDpAsState(
                  targetValue = tabPositions[selectedTabIndex].contentWidth,
                  label = "WidthAnimation",
               )
               TabRowDefaults.PrimaryIndicator(
                  Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                  width = width,
                  color = (serviceIdTypes?.get(serviceIds[selectedTabIndex])
                     ?: ServiceIdType.WEEKDAY).contentColor
               )
            }
         }
      ) {
         serviceIds.forEachIndexed { i, serviceId ->
            val type = serviceIdTypes?.get(serviceId) ?: ServiceIdType.WEEKDAY

            Tab(
               selected = selectedTabIndex == i,
               onClick = {
                  onSetSelectedTabIndex(i)
                  /*onSetServiceId(
                     SelectedServiceState(
                        serviceId = serviceId,
                        date = todaysDate + i - todayTabOffset,
                        index = i,
                     )
                  )*/
               },
               text = { Text(getLabel(i - todayTabOffset)) },
               selectedContentColor = type.contentColor,
            )
         }
      }
   }

}