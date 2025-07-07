package hr.squidpai.zetapi.realtime

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private fun main() {
   val data = RealtimeDispatcher.download()
   val timestamp = ZonedDateTime.ofInstant(
      Instant.ofEpochSecond(data.header.timestamp),
      ZoneId.of("Europe/Zagreb"),
   ).format(
      DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
   )
   println("Feed timestamp: $timestamp")
   println(data)

   val tripsWithTripUpdate = data.entityList.filter { it.hasTripUpdate() }
      .map { it.tripUpdate.trip.tripId }
      .toSet()

   val tripsWithVehicleUpdates = data.entityList.filter { it.hasVehicle() }
      .map { it.vehicle.trip.tripId }
      .toSet()

   val tripsWithVehicleUpdatesWithoutTripUpdates = tripsWithVehicleUpdates - tripsWithTripUpdate

   println("Trips with trip updates without vehicle updates: $tripsWithVehicleUpdatesWithoutTripUpdates")
}
