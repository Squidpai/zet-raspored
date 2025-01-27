package hr.squidpai.zetapi

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public typealias ServiceId = String

/** Class containing data from the GTFS schedule file "calendar_dates.txt". */
@Serializable
public class CalendarDates internal constructor(
   /**
    * The [ServiceId] used in each date of this schedule.
    *
    * Index 0 represents the [firstDate].
    */
   private val data: Array<ServiceId>,
   /** The first date when this schedule is available. */
   @Serializable(with = LocalDateSerializer::class)
   private val firstDate: LocalDate,
   /** The first date epoch when this schedule is available. */
   private val firstDateEpoch: Long,
   /** The last date when this schedule is available. */
   @Serializable(with = LocalDateSerializer::class)
   public val lastDate: LocalDate,
) {

   public val serviceIds: Iterator<ServiceId> get() = data.iterator()

   /**
    * Returns the [ServiceId] operating at the given date [epoch],
    * or `null` if there is no service that day.
    */
   public operator fun get(epoch: Long): ServiceId? = data.getOrNull((epoch - firstDateEpoch).toInt())

   /**
    * Returns the [ServiceId] operating at the given [date],
    * or `null` if there is no service that day.
    */
   public operator fun get(date: LocalDate): ServiceId? =
      data.getOrNull(ChronoUnit.DAYS.between(date, firstDate).toInt())

   /**
    * Returns a list of [ServiceId] operating from epoch [dateEpoch] + [range].[first][IntRange.first]
    * to (including) [dateEpoch] + [range].[last][IntRange.last].
    */
   public fun relativeSubRange(dateEpoch: Long, range: IntRange): List<ServiceId?> {
      val dateOffset = dateEpoch - firstDateEpoch

      return RelativeSubList((dateOffset + range.first).toInt(), (dateOffset + range.last + 1).toInt())
   }

   private inner class RelativeSubList(private val fromIndex: Int, toIndex: Int) : AbstractList<ServiceId?>(), RandomAccess {
      override val size = toIndex - fromIndex

      override fun get(index: Int) = data.getOrNull(fromIndex + index)
   }
}

private object LocalDateSerializer : KSerializer<LocalDate> {
   override val descriptor = PrimitiveSerialDescriptor(
      "java.time.LocalDate", PrimitiveKind.STRING
   )

   override fun serialize(encoder: Encoder, value: LocalDate) =
      encoder.encodeString(value.localDateToDate())

   override fun deserialize(decoder: Decoder) =
      decoder.decodeString().dateToLocalDate()

}
