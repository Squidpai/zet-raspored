package hr.squidpai.zetlive.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.news.NewsItem
import hr.squidpai.zetlive.news.NewsLoader
import hr.squidpai.zetlive.news.toFormattedString

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
private fun NewsEntry(item: NewsItem) = Surface(
    modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = MaterialTheme.shapes.large,
) {
    Column(Modifier.padding(horizontal = 4.dp)) {
        item.publicationDate?.let {
            Text(
                text = it.toFormattedString(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        item.title?.let {
            Text(
                text = it,
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            text = item.briefDescription,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )

        val context = LocalContext.current

        TextButton(onClick = {
            context.startActivity(
                Intent(context, NewsActivity::class.java)
                    .putExtra(EXTRA_NEWS_ITEM, item)
            )
        }, Modifier.padding(12.dp)) {
            Text("Opširnije")
        }
    }
}