package hr.squidpai.zetapi.gtfs

import hr.squidpai.zetapi.CsvFactory
import hr.squidpai.zetapi.CsvHeaderMapping
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.RouteAtStop
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.csvToListFromEntry
import hr.squidpai.zetapi.toStopId
import java.util.zip.ZipFile

internal fun loadStops(zip: ZipFile) =
   zip.csvToListFromEntry("stops.txt", mapping, factory)

private val mapping: CsvHeaderMapping = { header ->
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

private val factory: CsvFactory<Stop> = { headerMap, data ->
   Stop(
      id = data[headerMap[0]].toStopId(),
      code = data.getOrNull(headerMap[1])?.toIntOrNull() ?: 0,
      name = data[headerMap[2]],
      latitude = data.getOrNull(headerMap[3])?.toFloatOrNull() ?: Float.NaN,
      longitude = data.getOrNull(headerMap[4])?.toFloatOrNull() ?: Float.NaN,
      parentId = data.getOrNull(headerMap[5])?.toIntOrNull() ?: -1,
      routes = GtfsStopRoutes(),
   )
}

internal typealias GtfsStopRoutes = HashMap<Route, RouteAtStop>
