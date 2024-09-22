package hr.squidpai.zetlive.ui.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable

@ExperimentalMaterial3Api
@Composable
fun TooltipDefaults.inverseRichTooltipColors() = richTooltipColors(
   containerColor = MaterialTheme.colorScheme.inverseSurface,
   contentColor = MaterialTheme.colorScheme.inverseOnSurface,
   titleContentColor = MaterialTheme.colorScheme.inverseOnSurface,
   actionContentColor = MaterialTheme.colorScheme.inversePrimary,
)