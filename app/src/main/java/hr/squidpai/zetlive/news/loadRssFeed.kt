package hr.squidpai.zetlive.news

import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "loadRssFeed"

/**
 * Constant that can be used as the `downloadLink` parameter in [loadRssFeed] to
 * signify that a new feed should not be downloaded.
 */
const val DO_NOT_DOWNLOAD_NEW_FEED = ""

/**
 * Loads an RSS feed.
 *
 * It first checks if a cached version of the feed exists and is still valid (within the specified `timeToLive`).
 * If a valid cached feed is found, it is returned.
 * Otherwise, it attempts to download the feed from the `downloadLink`.
 * If the download is successful, the new feed is cached and returned.
 *
 * If `downloadLink` is set to [DO_NOT_DOWNLOAD_NEW_FEED], the function will only attempt to read the cached feed.
 *
 * @param cachedFeedFile The file where the RSS feed is cached.
 * @param downloadLink The URL from which to download the RSS feed. Use [DO_NOT_DOWNLOAD_NEW_FEED] to skip downloading.
 * @param timeToLive The maximum age of the cached feed before it's considered stale and a new download is attempted.
 * @return The RSS feed content as a String, or `null` if the feed could not be loaded (e.g., download failed, cache not found, or I/O errors).
 */
fun loadRssFeed(
    cachedFeedFile: File,
    downloadLink: String,
    timeToLive: Duration,
): String? {
    if (downloadLink == DO_NOT_DOWNLOAD_NEW_FEED)
        return readCachedFeed(cachedFeedFile)

    // it cachedFeedFile doesn't exist, lastModified() returns 0,
    // which is further than any reasonable timeToLive
    if ((System.currentTimeMillis() - cachedFeedFile.lastModified()).milliseconds > timeToLive)
        return readCachedFeed(cachedFeedFile)

    val connection = try {
        URI(downloadLink)
            .toURL()
            .openConnection()
    } catch (e: IOException) {
        Log.e(TAG, "Failed to open connection to RSS feed", e)
        return null
    }

    try {
        connection.connect()
    } catch (e: IOException) {
        Log.e(TAG, "Failed to connect to RSS feed", e)
        return null
    }

    val newFeed = try {
        connection.inputStream.reader().readText()
    } catch (e: IOException) {
        Log.e(TAG, "Failed to read RSS feed", e)
        return null
    }

    try {
        cachedFeedFile.writeText(newFeed)
    } catch (e: IOException) {
        Log.e(TAG, "Failed to save RSS feed", e)
    }

    return newFeed
}

/**
 * Reads the content of the cached feed file.
 *
 * @param file The [File] object representing the cached feed file.
 * @return The content of the cached feed as a [String], or `null` if the file
 *         does not exist or an error occurs while reading it.
 */
private fun readCachedFeed(file: File) =
    try {
        file.readText()
    } catch (_: FileNotFoundException) {
        Log.e(TAG, "No cached feed.")
        null
    } catch (e: IOException) {
        Log.e(TAG, "Failed to load cached feed.", e)
        null
    }