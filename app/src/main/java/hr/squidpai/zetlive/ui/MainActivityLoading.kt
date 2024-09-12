package hr.squidpai.zetlive.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.gtfs.Schedule

@Composable
fun MainActivityLoading(
   errorType: Schedule.ErrorType?,
   modifier: Modifier = Modifier,
) = Column(
   modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
   verticalArrangement = Arrangement.Center,
   horizontalAlignment = Alignment.CenterHorizontally,
) {
   if (errorType == null || errorType == Schedule.ErrorType.ALREADY_DOWNLOADING) {
      Text("Preuzimanje rasporeda${Typography.ellipsis}")
      CircularProgressIndicator(Modifier.padding(8.dp))
   } else {
      Text(errorType.errorMessage.orEmpty())

      val context = LocalContext.current
      Button(onClick = { Schedule.init(context.filesDir) }) {
         Text("Pokušaj ponovno")
      }
   }
}