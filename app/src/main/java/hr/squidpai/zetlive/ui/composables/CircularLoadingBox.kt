package hr.squidpai.zetlive.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Draws a [CircularProgressIndicator] wrapped in the center of
 * a [Box] that takes up as much space as possible, via [Modifier].[fillMaxSize].
 *
 * @param modifier the modifier to be applied to the Box
 */
@Composable
fun CircularLoadingBox(modifier: Modifier = Modifier) =
   Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
   }
