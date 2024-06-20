package hr.squidpai.zetlive.gtfs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class ServiceIdType {
  WEEKDAY, SATURDAY, SUNDAY;

  val contentColor
    @Composable get() = when (this) {
      WEEKDAY -> MaterialTheme.colorScheme.primary
      SATURDAY -> MaterialTheme.colorScheme.secondary
      SUNDAY -> MaterialTheme.colorScheme.tertiary
    }

  companion object {
    fun ofDate(date: Long) =
      // January 1st, 1970 (day 0) was a thursday.
      // Offset it by three days and mod by 7 since weekdays are
      // independent of any other time frame.
      when ((date + 3) % 7) {
        5L -> SATURDAY
        6L -> SUNDAY
        else -> WEEKDAY
      }
  }
}

typealias ServiceIdTypes = Map<ServiceId, ServiceIdType>