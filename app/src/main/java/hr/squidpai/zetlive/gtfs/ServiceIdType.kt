package hr.squidpai.zetlive.gtfs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class ServiceIdType {
  WEEKDAY, SATURDAY, SUNDAY;

  val contentColor @Composable get() = when (this) {
    WEEKDAY -> MaterialTheme.colorScheme.primary
    SATURDAY -> MaterialTheme.colorScheme.secondary
    SUNDAY -> MaterialTheme.colorScheme.tertiary
  }
}

typealias ServiceIdTypes = Map<ServiceId, ServiceIdType>