package hr.squidpai.zetlive.news

import com.prof18.rssparser.RssParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.time.Duration.Companion.hours

typealias NewsFeed = List<NewsItem>

object NewsManager {

    const val NEWS_RSS_LINK = "https://www.zet.hr/rss_novosti.aspx"
    private const val NEWS_FILE = "news.rss"
    private val newsTimeToLive = 30.hours

    const val TRAFFIC_CHANGES_RSS_LINK = "https://www.zet.hr/rss_promet.aspx"
    private const val TRAFFIC_CHANGES_FILE = "traffic_changes.rss"
    private val trafficChangesTimeToLive = 8.hours

    private val parser = RssParser()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    lateinit var newsLoader: NewsLoader
        private set

    lateinit var trafficChangesLoader: NewsLoader
        private set

    fun init(filesDir: File) {
        newsLoader = NewsLoader(
            parser,
            coroutineScope,
            cachedFeedFile = File(filesDir, NEWS_FILE),
            downloadLink = NEWS_RSS_LINK,
            timeToLive = newsTimeToLive
        )
        trafficChangesLoader = NewsLoader(
            parser,
            coroutineScope,
            cachedFeedFile = File(filesDir, TRAFFIC_CHANGES_FILE),
            downloadLink = TRAFFIC_CHANGES_RSS_LINK,
            timeToLive = trafficChangesTimeToLive,
        )
    }

}