package hr.squidpai.zetlive.gtfs

import hr.squidpai.zetlive.SortedListMap
import hr.squidpai.zetlive.emptySortedListMap
import hr.squidpai.zetlive.toSortedListMap
import java.util.zip.ZipFile

@JvmInline
value class ZippedTrips(val list: SortedListMap<TripId, ZippedTrip>) {

  constructor() : this(emptySortedListMap())

  constructor(zip: ZipFile, name: String = "trips.txt") : this(
    zip.csvToListFromEntry(name, zippedTripMapping, zippedTripFactory).toSortedListMap { it.tripId })

}

data class ZippedTrip(
  val routeId: RouteId,
  val serviceId: ServiceId,
  val tripId: TripId,
  val headsign: String,
  val directionId: Int,
  val blockId: Int,
) {
  companion object {
    fun getRouteIdFromTripId(tripId: TripId): RouteId {
      val lastUnderscore = tripId.lastIndexOf('_')
      val semiLastUnderscore = tripId.lastIndexOf('_', startIndex = lastUnderscore - 1)
      return tripId.substring(semiLastUnderscore + 1, lastUnderscore).toInt()
    }
  }
}

private val zippedTripMapping: CsvHeaderMapping = { header ->
  val headerMap = IntArray(6) { -1 }

  header.forEachIndexed { i, h ->
    when (h) {
      "route_id" -> headerMap[0] = i
      "service_id" -> headerMap[1] = i
      "trip_id" -> headerMap[2] = i
      "trip_headsign" -> headerMap[3] = i
      "direction_id" -> headerMap[4] = i
      "block_id" -> headerMap[5] = i
    }
  }

  headerMap
}

private val zippedTripFactory: CsvFactory<ZippedTrip> = { headerMap, data ->
  ZippedTrip(
    routeId = data[headerMap[0]].toInt(),
    serviceId = data[headerMap[1]],
    tripId = data[headerMap[2]],
    headsign = data[headerMap[3]],
    directionId = data[headerMap[4]].toInt(),
    blockId = data[headerMap[5]].toInt(),
  )
}
