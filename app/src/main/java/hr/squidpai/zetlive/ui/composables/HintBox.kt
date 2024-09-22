package hr.squidpai.zetlive.ui.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hr.squidpai.zetlive.Data

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
   positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
   tooltip = {
      RichTooltip(colors = TooltipDefaults.inverseRichTooltipColors()) {
         Text(hint.hintText)
      }
   },
   state,
   modifier,
   focusable = false,
   enableUserInput = false,
   content
)
