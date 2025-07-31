package hr.squidpai.zetlive.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.rssparser.model.RssItem
import hr.squidpai.zetlive.news.NewsLoader

@Composable
fun MainActivityNews(source: NewsLoader) {
    LaunchedEffect(Unit) { source.initNews() }

    val feed = source.feed.collectAsState().value

    if (feed == null) {
        LoadingNews()
        return
    }

    for (item in feed) {

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingNews() = Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text("Učitavanje${Typography.ellipsis}")
    LoadingIndicator(Modifier.padding(8.dp))
}

@Composable
private fun NewsEntry(item: RssItem) {

}