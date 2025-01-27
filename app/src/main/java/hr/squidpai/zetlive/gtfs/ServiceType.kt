package hr.squidpai.zetlive.gtfs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import hr.squidpai.zetapi.ServiceType
import hr.squidpai.zetapi.ServiceType.SATURDAY
import hr.squidpai.zetapi.ServiceType.SUNDAY
import hr.squidpai.zetapi.ServiceType.WEEKDAY

val ServiceType.contentColor
  @Composable get() = when (this) {
    WEEKDAY -> MaterialTheme.colorScheme.primary
    SATURDAY -> MaterialTheme.colorScheme.secondary
    SUNDAY -> MaterialTheme.colorScheme.tertiary
  }
