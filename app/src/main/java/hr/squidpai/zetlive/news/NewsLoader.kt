package hr.squidpai.zetlive.news

import android.util.Log
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.RssParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "NewsLoader"

class NewsLoader(
    private val parser: RssParser,
    private val coroutineScope: CoroutineScope,
    private val cachedFeedFile: File,
    private val downloadLink: String,
    val timeToLive: Duration,
) {

    enum class State { NOT_LOADING, LOADING, SUCCESS, FAILURE }

    private val mState = MutableStateFlow(State.NOT_LOADING)

    val state = mState.asStateFlow()

    private val mFeed = MutableStateFlow<NewsFeed?>(null)

    val feed = mFeed.asStateFlow()

    fun initNews() {
        if (mFeed.value != null)
            return

        loadRssFeed(forceRedownload = false)
    }

    fun refreshNews() = loadRssFeed(forceRedownload = true)

    @OptIn(ExperimentalAtomicApi::class)
    private val alreadyLoading = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    private fun loadRssFeed(forceRedownload: Boolean) {
        if (!alreadyLoading.compareAndSet(expectedValue = false, newValue = true))
            return

        mState.value = State.LOADING

        coroutineScope.launch {
            var failedToLoadCached = false

            if (!forceRedownload &&
                // if cachedFeedFile doesn't exist, lastModified() returns 0,
                // which is further than any reasonable timeToLive
                (System.currentTimeMillis() - cachedFeedFile.lastModified()).milliseconds < timeToLive
            ) {
                val newFeed = readCachedFeed()?.parseRssFeed()
                if (newFeed != null) {
                    mState.value = State.SUCCESS
                    mFeed.value = newFeed
                    return@launch
                } else
                    failedToLoadCached = true
            }

            var failedToDownload = false

            run download@{
                val connection = try {
                    URI(downloadLink)
                        .toURL()
                        .openConnection()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to open connection to RSS feed", e)
                    failedToDownload = true
                    return@download
                }

                try {
                    connection.connect()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to connect to RSS feed", e)
                    failedToDownload = true
                    return@download
                }

                val newFeedText = try {
                    connection.inputStream.reader().readText()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to read RSS feed", e)
                    failedToDownload = true
                    return@download
                }

                try {
                    cachedFeedFile.writeText(newFeedText)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to save RSS feed", e)
                }

                val newFeed = newFeedText.parseRssFeed()
                if (newFeed != null) {
                    mState.value = State.SUCCESS
                    mFeed.value = newFeed
                    return@launch
                } else
                    failedToLoadCached = true
            }

            if (failedToDownload && !failedToLoadCached) {
                val newFeed = readCachedFeed()?.parseRssFeed()
                if (newFeed != null) {
                    mState.value = State.SUCCESS
                    mFeed.value = newFeed
                    return@launch
                }
            }

            mState.value = State.FAILURE
        }
    }

    /**
     * Reads the content of the [cachedFeedFile].
     *
     * @return The content of the cached feed as a [String], or `null` if the file
     *         does not exist or an error occurs while reading it.
     */
    private fun readCachedFeed() =
        try {
            cachedFeedFile.readText()
        } catch (_: FileNotFoundException) {
            Log.e(TAG, "No cached feed.")
            null
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load cached feed.", e)
            null
        }

    /**
     * Parses the given [String] as an RSS feed and returns the parsed items,
     * or `null` if parsing fails.
     */
    private suspend fun String.parseRssFeed() =
        try {
            parser.parse(this@parseRssFeed).items
        } catch (e: RssParsingException) {
            Log.e(TAG, "Failed to parse feed", e)
            null
        }

}