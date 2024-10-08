package hr.squidpai.zetlive.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HintIconButton(
   icon: ImageVector,
   contentDescription: String?,
   tooltipTitle: String,
   tooltipText: String,
   modifier: Modifier = Modifier,
   iconTint: Color = MaterialTheme.colorScheme.secondary,
   showClickIndication: Boolean = false,
   action: (@Composable () -> Unit)? = null,
) {
   val state = rememberTooltipState(isPersistent = true)
   TooltipBox(
      positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
      tooltip = {
         RichTooltip(
            title = { Text(tooltipTitle) },
            action = action,
            colors = TooltipDefaults.inverseRichTooltipColors(),
         ) {
            Text(tooltipText)
         }
      },
      state,
      modifier,
      focusable = false,
      enableUserInput = false,
   ) {
      val scope = rememberCoroutineScope()
      Icon(
         icon,
         contentDescription,
         modifier = Modifier
            .minimumInteractiveComponentSize()
            .run {
               val onClick: () -> Unit = {
                  if (!state.isVisible)
                     scope.launch { state.show() }
               }

               if (showClickIndication) clickable(onClick = onClick)
               else pointerInput(Unit) {
                  while (true)
                     detectTapGestures { onClick() }
               }
            },
         iconTint,
      )
   }
}
