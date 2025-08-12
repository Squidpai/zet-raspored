package hr.squidpai.zetlive.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.getSerializableExtraCompat
import hr.squidpai.zetlive.news.Element
import hr.squidpai.zetlive.news.NewsItem
import hr.squidpai.zetlive.news.toFormattedString
import hr.squidpai.zetlive.ui.composables.AsyncImage

private const val TAG = "NewsActivity"

class NewsActivity : ComponentActivity() {

    private lateinit var newsItem: NewsItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extraNewsItem = intent.getSerializableExtraCompat<NewsItem>(EXTRA_NEWS_ITEM)
        if (extraNewsItem == null) {
            Log.w(TAG, "onCreate: no newsItem given")
            finish()
            return
        }
        newsItem = extraNewsItem

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(
                    topBar = { MyTopAppBar() }
                ) { innerPadding ->
                    MyContent(innerPadding)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MyTopAppBar() = TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = ::finish) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Natrag")
            }
        }
    )

    @Composable
    private fun MyContent(innerPadding: PaddingValues) = LazyColumn(
        modifier = Modifier.fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentPadding = innerPadding,
    ) {
        item {
            newsItem.publicationDate?.toFormattedString()?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                newsItem.title.orEmpty(),
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        items(newsItem.elements.size) { index ->
            val element = newsItem.elements[index]

            when (element) {
                is Element.Text -> Text(element.text)
                is Element.Image -> AsyncImage(
                    sourceUrl = element.sourceUrl,
                    contentDescription = element.altText,
                    modifier = Modifier
                        .defaultMinSize(
                            minHeight = with(LocalDensity.current) {
                                element.size.height.toDp()
                            }
                        ).fillMaxWidth(),
                )
            }
        }
    }

}