package hr.squidpai.zetlive.gtfs

import hr.squidpai.zetlive.SortedListMap
import hr.squidpai.zetlive.asSortedListMap
import hr.squidpai.zetlive.emptySortedListMap
import java.util.zip.ZipFile

/**
 * Regex that matches ASCII digits 0 through 9.
 */
private val asciiDigitsOnlyRegex = Regex("[0-9]+")

@JvmInline
value class Routes(val list: SortedListMap<RouteId, Route>) {

  constructor() : this(emptySortedListMap())

  constructor(zip: ZipFile, name: String = "routes.txt") : this(
    zip.csvToListFromEntry(name, routeMapping, routeFactory).asSortedListMap { it.id })

  fun filter(trimmedInput: String) = filter(list, trimmedInput)

  companion object {
    val empty = Routes()

    fun filter(list: List<Route>, trimmedInput: String): List<Route> {
      if (trimmedInput.isEmpty()) return list

      if (trimmedInput.matches(asciiDigitsOnlyRegex))
        return list.filter { trimmedInput in it.shortName }

      val splitInput = trimmedInput.split(' ')

      return list.filter { route ->
        splitInput.all { route.longName.contains(it, ignoreCase = true) }
      }
    }
  }

}

typealias RouteId = Int

class Route(
  val id: RouteId,
  val shortName: String,
  val longName: String,
  val type: Int,
)

private val routeMapping: CsvHeaderMapping = { header ->
  val headerMap = IntArray(4) { -1 }
  header.forEachIndexed { i, h ->
    when (h) {
      "route_id" -> headerMap[0] = i
      "route_short_name" -> headerMap[1] = i
      "route_long_name" -> headerMap[2] = i
      "route_type" -> headerMap[3] = i
    }
  }
  headerMap
}

private val routeFactory: CsvFactory<Route> = { headerMap, data ->
  Route(
    id = data[headerMap[0]].toInt(),
    shortName = data[headerMap[1]],
    longName = data[headerMap[2]],
    type = data[headerMap[3]].toIntOrNull() ?: -1,
  )
}
