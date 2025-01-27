package hr.squidpai.zetapi.cached

import com.opencsv.CSVWriter
import hr.squidpai.zetapi.Shape
import hr.squidpai.zetapi.writeNext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object CachedShape {

   fun save(shape: Shape, writer: CSVWriter) {
      writer.writeNext(
         shape.id,
         Json.encodeToString(shape as List<List<Shape.Point>>),
      )
   }

   operator fun invoke(data: Array<out String>) = Shape(
      id = data[0],
      points = Json.decodeFromString(data[1]),
   )

}