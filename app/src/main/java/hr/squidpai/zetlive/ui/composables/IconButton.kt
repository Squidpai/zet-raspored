package hr.squidpai.zetlive.ui.composables

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * [Material Design standard icon button](https://m3.material.io/components/icon-button/overview)
 *
 * Icon buttons help people take supplementary actions with a single tap.
 * They’re used when a compact button is required, such as in a toolbar or image list.
 *
 * [Standard icon button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * @param icon [ImageVector] to draw inside
 * @param contentDescription text used by accessibility services to describe what this icon represents.
 * This should always be provided unless this icon is used for decorative purposes, and
 * does not represent a meaningful action that a user can take.
 * This text should be localized, such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * button in different states. See [IconButtonDefaults.iconButtonColors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this icon button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behaviour of this icon button in different states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButton(
   icon: ImageVector,
   contentDescription: String,
   onClick: () -> Unit,
   modifier: Modifier = Modifier,
   enabled: Boolean = true,
   colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
   interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = TooltipBox(
   positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
   tooltip = { PlainTooltip { Text(contentDescription) } },
   state = rememberTooltipState(),
) {
   androidx.compose.material3.IconButton(onClick, modifier, enabled, colors, interactionSource) {
      Icon(icon, contentDescription)
   }
}
