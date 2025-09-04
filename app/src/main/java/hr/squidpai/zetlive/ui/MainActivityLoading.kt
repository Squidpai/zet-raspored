package hr.squidpai.zetlive.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hr.squidpai.zetapi.gtfs.ErrorType
import hr.squidpai.zetlive.gtfs.ScheduleManager

@Composable
fun MainActivityLoading(
    errorType: ErrorType?,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
       .fillMaxSize()
       .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    if (errorType == null || errorType == ErrorType.ALREADY_DOWNLOADING) {
        val state = ScheduleManager.downloadState.collectAsState().value
        state?.operation.message?.let { Text(it) }

        val progress = state?.progress ?: Float.NaN
        if (progress.isNaN())
            CircularProgressIndicator(Modifier.padding(8.dp))
        else
            CircularProgressIndicator({ progress }, Modifier.padding(8.dp))
    } else {
        Text(errorType.errorMessage.orEmpty())

        val context = LocalContext.current
        Button(onClick = { ScheduleManager.init(context.filesDir) }) {
            Text("Pokušaj ponovno")
        }
    }
}

private val ScheduleManager.DownloadOperation?.message
    get() = when (this) {
        null, ScheduleManager.DownloadOperation.LOADING_CACHED -> null
        ScheduleManager.DownloadOperation.DOWNLOADING -> "Preuzimanje rasporeda${Typography.ellipsis}"
        ScheduleManager.DownloadOperation.LOADING_GTFS -> "Pripremanje rasporeda${Typography.ellipsis}"
    }
