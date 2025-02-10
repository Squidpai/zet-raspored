package hr.squidpai.zetapi

import androidx.collection.IntList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@JvmInline
public value class TimeOfDay(public val valueInSeconds: Int) {

   public constructor(duration: Duration) :
         this(duration.toInt(DurationUnit.SECONDS))

   public constructor(hours: Int, minutes: Int) :
         this(hours * 3600 + minutes * 60)

   public constructor(hours: Int, minutes: Int, seconds: Int) :
         this(hours * 3600 + minutes * 60 + seconds)

   public constructor(time: String) :
         this(
            (time[0] - '0') * 36000 + (time[1] - '0') * 3600 +
                  (time[3] - '0') * 600 + (time[4] - '0') * 60 +
                  (time[6] - '0') * 10 + (time[7] - '0')
         )

   public val valueInMinutes: Int get() = valueInSeconds / 60

   public val valueInHours: Int get() = valueInSeconds / 3600

   public val seconds: Int get() = valueInSeconds % 60

   public val minutes: Int get() = valueInSeconds / 60 % 60

   public val hours: Int get() = valueInSeconds / 3600

   @Suppress("NOTHING_TO_INLINE")
   public inline operator fun component1(): Int = hours

   @Suppress("NOTHING_TO_INLINE")
   public inline operator fun component2(): Int = minutes

   @Suppress("NOTHING_TO_INLINE")
   public inline operator fun component3(): Int = seconds

   public operator fun plus(duration: Duration): TimeOfDay =
      TimeOfDay((valueInSeconds + duration.inWholeSeconds).toInt())

   public operator fun minus(duration: Duration): TimeOfDay =
      TimeOfDay((valueInSeconds - duration.inWholeSeconds).toInt())

   public operator fun minus(timeOfDay: TimeOfDay): Duration =
      (valueInSeconds - timeOfDay.valueInSeconds).seconds

   public fun minusSeconds(timeOfDay: TimeOfDay): Int =
      valueInSeconds - timeOfDay.valueInSeconds

   public fun minusMinutes(timeOfDay: TimeOfDay): Int =
      valueInMinutes - timeOfDay.valueInMinutes

   public fun minusHours(timeOfDay: TimeOfDay): Int =
      valueInHours - timeOfDay.valueInHours

   public operator fun compareTo(other: TimeOfDay): Int =
      this.valueInSeconds.compareTo(other.valueInSeconds)

   public fun isTomorrow(): Boolean = hours >= 24

   override fun toString(): String =
      "${hours.toString().padStart(2, '0')}:${
         minutes.toString().padStart(2, '0')
      }:${
         seconds.toString().padStart(2, '0')
      }"

   public fun toStringHHMM(): String {
      val hours = hours % 24
      val minutes = minutes
      return if (minutes < 10) "$hours:0$minutes" else "$hours:$minutes"
   }

}

/** Type alias for a list of [TimeOfDay]s. */
public typealias TimeOfDayList = IntList
