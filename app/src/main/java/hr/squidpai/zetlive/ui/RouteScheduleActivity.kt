package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.*
import hr.squidpai.zetlive.gtfs.*
import java.time.LocalDate

class RouteScheduleActivity : ComponentActivity() {

  companion object {
    private const val TAG = "RouteScheduleActivity"

    const val EXTRA_ROUTE = "hr.squidpai.zetlive.extra.ROUTE"
    const val EXTRA_DIRECTION = "hr.squidpai.zetlive.extra.DIRECTION"

    private val defaultDateRange = -1..7

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

    val routeId = intent.getIntExtra(EXTRA_ROUTE, -1)

    if (routeId == -1) {
      Log.w(TAG, "onCreate: No route id given, finishing activity early.")

      finish()
      return
    }

    if (intent.getIntExtra(EXTRA_DIRECTION, 0) == 1)
      selectedDirection.intValue = 1

    setContent {
      AppTheme {
        val schedule = Schedule.instance

        val route = schedule.routes?.list?.get(key = routeId)
        val trips = schedule.getTripsOfRoute(routeId).value

        val commonFirstStop = trips?.commonFirstStop

        Scaffold(topBar = { MyTopAppBar(route) }) { padding ->
          MyContent(routeId, trips, commonFirstStop, Modifier.padding(padding))
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
    title = { Text(if (route != null) "${route.shortName} ${route.longName}" else LOADING_TEXT) },
    navigationIcon = {
      IconButton(onClick = { finish() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Natrag")
      }
    }
  )

  @Composable
  private fun MyContent(
    routeId: RouteId,
    trips: Trips?,
    commonFirstStops: Pair<StopId, StopId>?,
    modifier: Modifier = Modifier,
  ) = Column(modifier) {
    val (selectedServiceId, setServiceId) =
      remember { mutableStateOf(null as ServiceId? to 1L) }

    ServiceTabRow(trips?.list, onSetServiceId = setServiceId)

    val stops = Schedule.instance.stops

    val filteredStopTimes =
      if (selectedServiceId.first != null && trips != null)
        trips.list
          .filterByServiceId(selectedServiceId.first!!)
          .filterByDirection()
          .sortedByDepartures()
      else null

    DateContent(
      routeId, commonFirstStops, trips?.commonHeadsign,
      stopTimes = filteredStopTimes,
      stops,
      selectedDate = selectedServiceId.second
    )
  }

  @Composable
  private fun DateContent(
    routeId: RouteId,
    commonFirstStops: Pair<StopId, StopId>?,
    commonHeadsigns: Pair<String, String>?,
    stopTimes: Pair<List<Trip>, List<Trip>>?,
    stops: Stops?,
    selectedDate: Long,
  ) {
    val (direction, setDirection) = selectedDirection

    val currentTime = localCurrentTimeMillis() / MILLIS_IN_SECONDS
    val timeOffset = selectedDate * SECONDS_IN_DAY

    val states = remember(key1 = System.identityHashCode(stopTimes)) {
      stopTimes?.findFirstDepartures((currentTime - timeOffset).toInt())?.let { (first, second) ->
        LazyListState(first) to LazyListState(second)
      }
    }

    val commonHeadsign: String?
    val commonFirstStop: StopId
    val list: List<Trip>?
    val state: LazyListState?

    if (direction == 0) {
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

    DirectionRow(commonHeadsigns, direction, setDirection)

    if (list == null)
      CircularLoadingBox()
    else if (list.isEmpty())
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val specialLabel = Love.giveMeTheSpecialLabelForNoTrips(routeId)
        Text(specialLabel ?: "Linija nema polazaka na izabrani datum.", Modifier.padding(horizontal = 16.dp))
      }
    else LazyColumn(state = state!!) {// list != null  =>  stopTimes != null  =>  state != null
      val firstOfNextDay = list.findFirstDepartureTomorrow()

      items(list.size) { i ->
        val stopTime = list[i]
        val stopId = stopTime.stops.first().toStopId()

        val stop = if (stopId != commonFirstStop) stops?.list?.get(stopId) else null

        if (i == firstOfNextDay) {
          HorizontalDivider(Modifier.padding(horizontal = 4.dp))
          Text(
            getLabel((selectedDate + 1 - todaysDate).toInt()),
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
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

  @Composable
  private fun TripRow(
    trip: Trip,
    overriddenFirstStop: String?,
    overriddenHeadsign: String?,
    liveComparison: Int,
    selectedDate: Long,
    modifier: Modifier = Modifier,
  ) {
    val selectTrip = LocalSelectTrip.current

    val specialLabel = Love.giveMeTheSpecialTripLabel(trip)

    Row(
      modifier = modifier
        .defaultMinSize(minHeight = 48.dp)
        .clickable { selectTrip(trip, selectedDate * MILLIS_IN_DAY) },
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val tint = when {
        liveComparison < 0 -> lerp(
          MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface, fraction = .36f
        )

        liveComparison == 0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
      }
      val weight = if (liveComparison == 0) FontWeight.SemiBold else null

      val startLabel = specialLabel?.first ?: overriddenFirstStop

      if (startLabel != null) Text(
        text = startLabel,
        modifier = Modifier.weight(1f),
        color = tint,
        fontWeight = weight,
        textAlign = TextAlign.Center,
        maxLines = 2,
      ) else Spacer(Modifier.weight(1f))

      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = trip.departures.first().timeToString(),
          modifier = Modifier.weight(1f),
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
        Text(trip.departures.last().timeToString(), Modifier.weight(1f), color = tint, fontWeight = weight)
      }

      val endLabel = specialLabel?.second ?: overriddenHeadsign

      if (endLabel != null) Text(
        text = endLabel,
        modifier = Modifier.weight(1f),
        color = tint,
        fontWeight = weight,
        textAlign = TextAlign.Center,
        maxLines = 2,
      ) else Spacer(Modifier.weight(1f))
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun ServiceTabRow(
    stopTimes: TripsList?,
    onSetServiceId: (Pair<ServiceId?, Long>) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    val calendarDates = Schedule.instance.calendarDates
    val serviceIds = calendarDates?.relativeSubList(todaysDate, defaultDateRange).orEmpty()

    val yesterdayServiceId = calendarDates?.get(todaysDate - 1)
    val todayTabOffset = if (stopTimes != null && yesterdayServiceId != null &&
      stopTimes.any {
        it.serviceId == yesterdayServiceId &&
            it.departures.last() - 1 * SECONDS_IN_DAY > todaysTime / MILLIS_IN_SECONDS
      }
    ) 2 else 1

    val (selectedTabIndex, setSelectedTabIndex) = remember { mutableIntStateOf(1) }

    // refresh service id on change of calendarDates
    LaunchedEffect(calendarDates) {
      onSetServiceId(serviceIds.getOrNull(selectedTabIndex) to todaysDate + selectedTabIndex - todayTabOffset)
    }

    PrimaryScrollableTabRow(selectedTabIndex, modifier) {
      serviceIds.forEachIndexed { i, serviceId ->
        Tab(
          selected = selectedTabIndex == i,
          onClick = {
            setSelectedTabIndex(i)
            onSetServiceId(serviceId to todaysDate + i - todayTabOffset)
          },
          text = {
            Text(getLabel(i - todayTabOffset))
          }
        )
      }
    }
  }

}