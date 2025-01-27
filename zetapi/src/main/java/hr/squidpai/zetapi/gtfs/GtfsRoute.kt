package hr.squidpai.zetapi.gtfs

import hr.squidpai.zetapi.CsvHeaderMapping
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.ServiceId
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetapi.TripId
import hr.squidpai.zetapi.csvToListFromEntry
import java.util.zip.ZipFile

internal class GtfsRoute(
   headerMap: IntArray,
   data: Array<out String>,
) : Route(
   id = data[headerMap[0]],
   shortName = data.getOrNull(headerMap[1]).orEmpty(),
   longName = data.getOrNull(headerMap[2]).orEmpty(),
   type = Type(data.getOrNull(headerMap[3])),
   sortOrder = data.getOrNull(headerMap[4])?.toIntOrNull()
      ?: data[headerMap[0]].let { id ->
         id.filter { it.isDigit() }.toIntOrNull() ?: id.hashCode()
      },
   commonHeadsigns = GtfsRouteCommonHeadsigns(),
) {
   override var trips = HashMap<TripId, Trip>()
      internal set

   override val stopSequences =
      mutableListOf<List<Stop>>() to mutableListOf<List<Stop>>()

   companion object {
      fun loadRoutes(zip: ZipFile) =
         zip.csvToListFromEntry("routes.txt", mapping, ::GtfsRoute)

      private val mapping: CsvHeaderMapping = { header ->
         val headerMap = IntArray(5) { -1 }
         header.forEachIndexed { i, h ->
            when (h) {
               "route_id" -> headerMap[0] = i
               "route_short_name" -> headerMap[1] = i
               "route_long_name" -> headerMap[2] = i
               "route_type" -> headerMap[3] = i
               "route_sort_order" -> headerMap[4] = i
            }
         }
         headerMap
      }
   }
}

internal typealias GtfsRouteCommonHeadsigns =
      HashMap<ServiceId, Pair<String, String>>
