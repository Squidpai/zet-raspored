package hr.squidpai.zetapi.cached

import com.opencsv.CSVWriter
import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.RouteAtStop
import hr.squidpai.zetapi.Routes
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.getFlag
import hr.squidpai.zetapi.mapEntries
import hr.squidpai.zetapi.putFlag
import hr.squidpai.zetapi.toStopId
import hr.squidpai.zetapi.writeNext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

internal object CachedStop {
   fun save(stop: Stop, writer: CSVWriter) = writer.writeNext(
      stop.id.toString(), stop.code.toString(), stop.name,
      stop.latitude.toString(), stop.longitude.toString(),
      stop.parentId.toString(),
      Json.encodeToString(RoutesSerializer(), stop.routes)
   )

   operator fun invoke(data: Array<out String>, routes: Routes) = Stop(
      id = data[0].toStopId(),
      code = data[1].toInt(),
      name = data[2],
      latitude = data[3].toFloat(),
      longitude = data[4].toFloat(),
      parentId = data[5].toInt(),
      routes = Json.decodeFromString(RoutesSerializer(routes), data[6])
   )
}

private class RoutesSerializer(
   val routes: Routes? = null,
) : KSerializer<Map<Route, RouteAtStop>> {
   private val delegateSerializer = MapSerializer(
      keySerializer = String.serializer(),
      valueSerializer = Int.serializer(),
   )

   @OptIn(ExperimentalSerializationApi::class)
   override val descriptor = SerialDescriptor(
      "Stop.routes",
      delegateSerializer.descriptor,
   )

   override fun serialize(encoder: Encoder, value: Map<Route, RouteAtStop>) =
      encoder.encodeSerializableValue(
         delegateSerializer,
         value.mapEntries { it.key.id to it.value.serialize() }
      )

   override fun deserialize(decoder: Decoder) =
      decoder.decodeSerializableValue(delegateSerializer).mapEntries {
         routes!![it.key]!! to it.value.deserialize()
      }

   private fun RouteAtStop.serialize() =
      isFirst.putFlag(0) or isLast.putFlag(1) or
            stopsAtDirectionZero.putFlag(2) or
            stopsAtDirectionOne.putFlag(3)

   private fun Int.deserialize() = RouteAtStop().apply {
      isFirst = getFlag(0)
      isLast = getFlag(1)
      stopsAtDirectionZero = getFlag(2)
      stopsAtDirectionOne = getFlag(3)
   }

}
