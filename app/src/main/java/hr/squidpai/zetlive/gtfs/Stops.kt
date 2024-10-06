package hr.squidpai.zetlive.gtfs

import androidx.collection.MutableIntList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hr.squidpai.zetlive.SortedListMap
import hr.squidpai.zetlive.asSortedListMap
import hr.squidpai.zetlive.emptySortedListMap
import hr.squidpai.zetlive.toSortedListMap
import hr.squidpai.zetlive.ui.Symbols
import java.util.zip.ZipFile
import kotlin.math.absoluteValue

class Stops(val list: SortedListMap<StopId, Stop>) {

  constructor() : this(emptySortedListMap())

  constructor(zip: ZipFile, name: String = "stops.txt") : this(
    zip.csvToListFromEntry(name, stopMapping, stopFactory).toSortedListMap { it.id })

  val groupedStops = ArrayList<GroupedStop>().apply {
    var currentGroup: GroupedStop? = null
    for (stop in list) {
      if (stop.parentId == -1) continue

      // The group is sorted by stop ids, which are compared by the stop number and then by the
      // stop code. All child stops have the same stop number, meaning they are all placed
      // together, and we can collect them with a single for loop.
      if (currentGroup == null || currentGroup.parentStop.id.stationNumber != stop.parentId) {
        if (currentGroup != null) {
          currentGroup.stopType = if (currentGroup.childStops.any { it.code < 10 }) StopType.Tram else StopType.Bus
        }
        currentGroup = GroupedStop(
          parentStop = list[stop.parentId.toParentStopId()]!!,
          childStops = mutableListOf(stop),
        ).also { add(it) }
      } else {
        currentGroup.childStops as MutableList += stop
      }
    }
    if (currentGroup != null) {
      currentGroup.stopType = if (currentGroup.childStops.any { it.code < 10 }) StopType.Tram else StopType.Bus
    }
  }.asSortedListMap { it.parentStop.id }
}

fun List<GroupedStop>.filter(trimmedInput: String): List<GroupedStop> {
  val splitInput = trimmedInput.split(' ')
  return filter { stop ->
    splitInput.all { stop.parentStop.name.contains(it, ignoreCase = true) }
  }
}


class GroupedStop(
  val parentStop: Stop,
  val childStops: List<Stop>,
) {
  var stopType by mutableStateOf(StopType.Undefined)

  private var joinedRoutesCache: Pair<String, RoutesAtStopMap>? = null

  fun joinAllRoutesToString(routesAtStopMap: RoutesAtStopMap): String {
    if (joinedRoutesCache != null && joinedRoutesCache!!.second === routesAtStopMap) {
      return joinedRoutesCache!!.first
    }

    val list = MutableIntList()
    for (childStop in childStops) {
      routesAtStopMap[childStop.id.value]?.let { routesAtStop ->
        routesAtStop.routes.forEach {
          val routeId = it.absoluteValue
          if (routeId !in list) list += routeId
        }
      }
    }
    list.sort()
    return list.joinToString().also { joinedRoutesCache = it to routesAtStopMap }
  }
}

@JvmInline
value class StopType private constructor(private val value: Int) {

  override fun toString() = when (this) {
    Undefined -> "Undefined"
    Tram -> "Tram"
    Bus -> "Bus"
    else -> "Invalid"
  }

  companion object {
    val Undefined = StopType(-1)
    val Tram = StopType(0)
    val Bus = StopType(1)
  }

}

@JvmInline
value class StopId(val value: Int) : Comparable<StopId> {

  constructor(stationNumber: Int, stationCode: Int) : this(stationNumber shl 16 or stationCode)

  val stationNumber get() = value ushr 16

  val stationCode get() = value and 0xFFFF

  override fun compareTo(other: StopId) = this.value.compareTo(other.value)

  override fun toString() = "${stationNumber}_$stationCode"

  fun isInvalid() = value == Invalid.value

  fun isValid() = value != Invalid.value

  inline fun <T> ifValid(action: (StopId) -> T) =
    if (this.isValid()) action(this) else null

  operator fun component1() = stationNumber

  operator fun component2() = stationCode

  companion object {
    val Invalid = StopId(-1)
  }
}

/**
 * Returns `this` value if it satisfies the given [predicate] or [StopId.Invalid], if it doesn't.
 */
inline fun StopId.takeIf(predicate: (StopId) -> Boolean) =
  if (predicate(this)) this else StopId.Invalid

fun String.toStopId() = indexOf('_').let {
  if (it == -1) toInt().toParentStopId()
  else StopId(substring(0, it).toInt(), substring(it + 1).toInt())
}

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toParentStopId() = StopId(this, 0)

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toStopId() = StopId(this)

data class Stop(
  val id: StopId,
  val code: Int,
  val name: String,
  val latitude: Float,
  val longitude: Float,
  val parentId: Int,
) {

  override fun toString() =
    "Stop(${id.value}.toStopId(), $code, \"$name\", ${latitude}f, ${longitude}f, $parentId)"

  fun getLabel(routesAtStopMap: RoutesAtStopMap?) =
    Love.giveMeTheLabelForStop(id) ?: getLabelFrom(routesAtStopMap?.get(id.value))

  fun getLabel(routesAtStop: RoutesAtStop?) =
    Love.giveMeTheLabelForStop(id) ?: getLabelFrom(routesAtStop)

  private fun getLabelFrom(routesAtStop: RoutesAtStop?) = when {
    routesAtStop == null -> null
    routesAtStop.first -> "Ulaz ${routesAtStop.routes.joinToString { it.absoluteValue.toString() }}"

    routesAtStop.last ->
      if (routesAtStop.routes.size == 1) "Izlaz ${routesAtStop.routes.first().absoluteValue}" else "Izlaz"

    else -> null
  }

  val iconInfo get() = when (Love.giveMeTheIconCodeForStop(id) % 10) {
    1 -> Symbols.ArrowRightAlt to "desno"
    2 -> Symbols.ArrowLeftAlt to "lijevo"
    3 -> Symbols.ArrowUpwardAlt to "gore"
    4 -> Symbols.ArrowDownwardAlt to "dolje"
    else -> null
  }

}

private val stopMapping: CsvHeaderMapping = { header ->
  val headerMap = IntArray(6) { -1 }
  header.forEachIndexed { i, h ->
    when (h) {
      "stop_id" -> headerMap[0] = i
      "stop_code" -> headerMap[1] = i
      "stop_name" -> headerMap[2] = i
      "stop_lat" -> headerMap[3] = i
      "stop_lon" -> headerMap[4] = i
      "parent_station" -> headerMap[5] = i
    }
  }
  headerMap
}

private val stopFactory: CsvFactory<Stop> = { headerMap, data ->
  Stop(
    id = data[headerMap[0]].toStopId(),
    code = data[headerMap[1]].toIntOrNull() ?: 0,
    name = data[headerMap[2]],
    latitude = data[headerMap[3]].toFloat(),
    longitude = data[headerMap[4]].toFloat(),
    parentId = data[headerMap[5]].toIntOrNull() ?: -1,
  )
}
