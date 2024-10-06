@file:Suppress("NOTHING_TO_INLINE")

package hr.squidpai.zetlive

import android.icu.util.TimeZone

const val MILLIS_IN_SECONDS = 1000
const val MILLIS_IN_HOURS = 1000 * 60 * 60
const val MILLIS_IN_DAY = 1000 * 60 * 60 * 24
const val SECONDS_IN_DAY = 60 * 60 * 24
const val SECONDS_IN_HOUR = 60 * 60

fun utcEpochDate() = System.currentTimeMillis() / MILLIS_IN_DAY

fun utcEpochTime() = (System.currentTimeMillis() % MILLIS_IN_DAY).toInt()

fun localCurrentTimeMillis() = System.currentTimeMillis().let { it + TimeZone.getDefault().getOffset(it) }

fun localEpochDate() = localCurrentTimeMillis() / MILLIS_IN_DAY

fun localEpochTime() = (localCurrentTimeMillis() % MILLIS_IN_DAY).toInt()

/**
 * Returns the weekday from the [epochDate] where 0 is monday,
 * 1 is tuesday, 2 is wednesday, and so on.
 */
fun getWeekDayOf(epochDate: Long) = (epochDate + 3) % 7

@Suppress("NOTHING_TO_INLINE")
inline fun Int.timeToString(): String {
  val hours = this / 3600 % 24
  val minutes = this / 60 % 60
  return if (minutes < 10) "$hours:0$minutes" else "$hours:$minutes"
}
