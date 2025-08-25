package hr.squidpai.zetlive.ui.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hr.squidpai.zetlive.Data

/**
 * A default implementation for a [TooltipBox] displaying a [hint].
 *
 * The `TooltipBox` isn't focusable, doesn't enable user input, and by default
 * is visible if [Data.hints.shouldBeVisible] is `true` for the given `hint`,
 * and is persistent.
 *
 * @param hint the hint to be displayed in the `TooltipBox`
 * @param modifier the [Modifier] to be applied to the `TooltipBox`
 * @param state handles the state of the tooltip's visibility; by default,
 *              it is persistent, and is visible if `hint.shouldBeVisible()` is `true`
 * @param content the composable that the tooltip will anchor to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HintBox(
    hint: Data.hints,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(
        initialIsVisible = hint.shouldBeVisible(),
        isPersistent = true,
    ),
    content: @Composable () -> Unit
) = TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = {
        RichTooltip(colors = TooltipDefaults.inverseRichTooltipColors()) {
            Text(hint.hintText)
        }
    },
    state,
    modifier,
    onDismissRequest = null, // TODO add implementation to allow dismissal only after a second of being displayed
    focusable = false,
    enableUserInput = false,
    hasAction = false,
    content
)
