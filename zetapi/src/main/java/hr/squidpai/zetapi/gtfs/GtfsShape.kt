package hr.squidpai.zetapi.gtfs

import hr.squidpai.zetapi.CsvHeaderMapping
import hr.squidpai.zetapi.SequentialCsvFactory
import hr.squidpai.zetapi.Shape
import hr.squidpai.zetapi.ShapeId
import hr.squidpai.zetapi.Shapes
import hr.squidpai.zetapi.csvToListSequentialFromEntry
import java.util.zip.ZipFile

internal fun loadShapes(zip: ZipFile, name: String = "shapes.txt"): Shapes =
   zip.csvToListSequentialFromEntry(
      name,
      estimatedListSize = 100_000,
      mapping,
      factory,
   ).associate { it.id to it.build() }

private val mapping: CsvHeaderMapping = { header ->
   val headerMap = IntArray(5) { -1 }
   header.forEachIndexed { i, h ->
      when (h) {
         "shape_id" -> headerMap[0] = i
         "shape_pt_lat" -> headerMap[1] = i
         "shape_pt_lon" -> headerMap[2] = i
         "shape_pt_sequence" -> headerMap[3] = i
         "shape_dist_traveled" -> headerMap[4] = i
      }
   }
   headerMap
}

private val factory: SequentialCsvFactory<Shape.Builder> =
   { headerMap, data, previous ->
      val id: ShapeId = data[headerMap[0]]
      val latitude = data[headerMap[1]].toFloatOrNull() ?: Float.NaN
      val longitude = data[headerMap[2]].toFloatOrNull() ?: Float.NaN
      val sequence = data[headerMap[3]].toIntOrNull() ?: -1
      val distanceTraveled = data[headerMap[4]].toFloatOrNull() ?: Float.NaN

      val point = Shape.Point(latitude, longitude, distanceTraveled)

      if (previous == null || previous.id != id)
         Shape.Builder(id, point, sequence)
      else if (sequence > previous.lastPointSequence + 90)
         previous.addToNewList(point)
      else
         previous.addToCurrentList(point)
   }