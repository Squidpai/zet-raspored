package hr.squidpai.zetlive.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.rssparser.model.RssItem
import hr.squidpai.zetlive.news.NewsLoader
import hr.squidpai.zetlive.ui.composables.disabled

@Composable
fun MainActivityNews(source: NewsLoader) {
    LaunchedEffect(Unit) { source.initNews() }

    val feedItems = source.feed.collectAsState().value

    if (feedItems == null) {
        LoadingNews()
        return
    }

    LazyColumn {
        items(feedItems.size) { index ->
            NewsEntry(feedItems[index])
        }
    }
}

//@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingNews() = Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text("Učitavanje${Typography.ellipsis}")
    CircularProgressIndicator(Modifier.padding(8.dp))
    // TODO update to include LoadingIndicator(Modifier.padding(8.dp))
}

@Composable
private fun NewsEntry(item: RssItem) = Surface(
    onClick = { /* TODO */ },
    modifier = Modifier.fillMaxWidth()
        .padding(8.dp),
) {
    Column {
        item.title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        item.description?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.disabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}