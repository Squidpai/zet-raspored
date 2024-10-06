package hr.squidpai.zetlive.ui.composables

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltipColors
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable

@Suppress("UnusedReceiverParameter")
@ExperimentalMaterial3Api
@Composable
fun TooltipDefaults.inverseRichTooltipColors() = RichTooltipColors(
   containerColor = MaterialTheme.colorScheme.inverseSurface,
   contentColor = MaterialTheme.colorScheme.inverseOnSurface,
   titleContentColor = MaterialTheme.colorScheme.inverseOnSurface,
   actionContentColor = MaterialTheme.colorScheme.inversePrimary,
)

@Composable
fun ButtonDefaults.textButtonColorsOnInverseRichTooltip() = textButtonColors(
   contentColor = MaterialTheme.colorScheme.inversePrimary,
   disabledContentColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .38f),
)

@Composable
fun IconButtonDefaults.errorIconButtonColors() = filledIconButtonColors(
   containerColor = MaterialTheme.colorScheme.errorContainer,
   contentColor = MaterialTheme.colorScheme.onErrorContainer,
)
