package hr.squidpai.zetapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Class containing all data of the same [ShapeId]
 * from the GTFS schedule file "stops.txt",
 * organised and grouped by stop indices of a trip.
 */
public class Shape internal constructor(
   /** Identifies a shape. */
   public val id: ShapeId,
   points: List<List<Point>>,
) : List<List<Shape.Point>> by points {

   /** A single point of a shape. */
   @Serializable(with = PointSerializer::class)
   public data class Point(
      /** Latitude of a shape point. */
      val latitude: Float,
      /** Longitude of a shape point. */
      val longitude: Float,
      /**
       * Actual distance traveled along the shape from the
       * first shape point to the point specified in this record.
       */
      val distanceTraveled: Float,
   )

   /** Shape builder object used by `GtfsShape`. */
   internal class Builder(
      val id: ShapeId,
      firstPoint: Point,
      firstPointSequence: Int,
   ) {

      val points = mutableListOf(mutableListOf(firstPoint))
      var lastPointSequence = firstPointSequence

      /**
       * Appends the specified [point] to the last list of points.
       *
       * @return `null` for [SequentialCsvFactory] as a signal to
       * continue using this builder object
       */
      fun addToCurrentList(point: Point): Nothing? {
         points.last() += point
         return null
      }

      /**
       * Appends a new list of points, and adds the specified [point] to it.
       *
       * @return `null` for [SequentialCsvFactory] as a signal to
       * continue using this builder object
       */
      fun addToNewList(point: Point): Nothing? {
         points += mutableListOf(point)
         return null
      }

      fun build() = Shape(id, points)
   }

   override fun equals(other: Any?): Boolean =
      this === other || other is Shape && this.id == other.id

   override fun hashCode(): Int = id.hashCode()

   override fun toString(): String =
      joinToString(
         prefix = "Points(id=$id, points=[",
         postfix = "])"
      )
}

/** Unique [Shape] identifier. */
public typealias ShapeId = String

/** Map of [Shape]s, typically extracted from the GTFS schedule file "shapes.txt". */
public typealias Shapes = Map<ShapeId, Shape>

private object PointSerializer : KSerializer<Shape.Point> {
   val delegateSerializer = FloatArraySerializer()

   @OptIn(ExperimentalSerializationApi::class)
   override val descriptor = SerialDescriptor(
      "hr.squidpai.zetapi.Shape.Point",
      delegateSerializer.descriptor,
   )

   override fun serialize(encoder: Encoder, value: Shape.Point) =
      encoder.encodeSerializableValue(
         delegateSerializer,
         if (value.distanceTraveled.isNaN())
            floatArrayOf(value.latitude, value.longitude)
         else
            floatArrayOf(value.latitude, value.longitude, value.distanceTraveled)
      )

   override fun deserialize(decoder: Decoder): Shape.Point {
      val data = decoder.decodeSerializableValue(delegateSerializer)

      return Shape.Point(
         latitude = data[0],
         longitude = data[1],
         distanceTraveled = data.getOrElse(2) { Float.NaN },
      )
   }
}
