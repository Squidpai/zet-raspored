package hr.squidpai.zetlive.ui.composables

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import hr.squidpai.zetapi.RouteId
import hr.squidpai.zetlive.Data
import hr.squidpai.zetlive.ui.Symbols

/**
 * Used to select the direction of a route.
 *
 * @param routeId the route id
 * @param commonHeadsign the labels to be displayed as the directions' headsign
 * @param direction index representing the selected direction (0 is the first direction
 * and 1 is the opposite direction)
 * @param setDirection the callback that is triggered when the direction is updated by
 * this function
 * @param isRoundRoute whether the route is a round one: it only has one direction
 * @param modifier the modifier to be applied to this layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.DirectionRow(
   routeId: RouteId,
   commonHeadsign: Pair<String, String>,
   direction: Int,
   setDirection: (Int) -> Unit,
   isRoundRoute: Boolean,
   modifier: Modifier = Modifier,
) = Row(
   modifier.align(Alignment.CenterHorizontally),
   verticalAlignment = Alignment.CenterVertically,
) {
   val firstSign = commonHeadsign.first

   if (isRoundRoute) {
      Text(firstSign, maxLines = 1, overflow = TextOverflow.Ellipsis)

      HintIconButton(
         Symbols._360,
         contentDescription = null,
         tooltipTitle = "Ovo je kružna linija.",
         tooltipText = "Kružne linije nemaju dva smjera, već naprave krug po svojoj trasi " +
               "te se vrate natrag na početno stajalište.",
      )

      Text(firstSign, maxLines = 1, overflow = TextOverflow.Ellipsis)
   } else {
      // putting an else block instead of a return in the if block
      // because an exception is thrown from Compose otherwise

      val secondSign = commonHeadsign.second

      val leftSign: String
      val rightSign: String

      if (direction == 1) {
         leftSign = firstSign
         rightSign = secondSign
      } else {
         leftSign = secondSign
         rightSign = firstSign
      }

      Text(
         leftSign,
         modifier = Modifier.weight(1f),
         textAlign = TextAlign.End,
         maxLines = 1,
         overflow = TextOverflow.Ellipsis,
      )
      HintBox(Data.hints.swapDirection) {
         IconButton(
            icon = Symbols.SwapHorizontal,
            contentDescription = "Zamijeni smjer",
            onClick = {
               setDirection(1 - direction)
               Data.updateData {
                  Data.hints.swapDirection.satisfyWithoutUpdate()
                  if (direction == 0)
                     directionSwapped += routeId
                  else
                     directionSwapped -= routeId
               }
            },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
         )
      }
      Text(
         rightSign,
         modifier = Modifier.weight(1f),
         maxLines = 1,
         overflow = TextOverflow.Ellipsis,
      )
   }
}
