package hr.squidpai.zetapi.gtfs

import hr.squidpai.zetapi.FeedInfo
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.Schedule
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.maxOf
import hr.squidpai.zetapi.realtime.RealtimeDispatcher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.util.zip.ZipFile

public object GtfsScheduleLoader {

    public const val LINK: String = "https://www.zet.hr/gtfs-scheduled/latest"

    public fun versionLink(version: String): String =
        "https://www.zet.hr/gtfs-scheduled/scheduled-000-$version.zip"

    public data class DownloadResult(
        val errorType: ErrorType? = null,
        val exception: Exception? = null,
        val version: String? = null,
    ) {
        public companion object {
            public val SUCCESS: DownloadResult = DownloadResult()
        }
    }

    /**
     * Downloads the GTFS schedule from the specified [link] to [file].
     *
     * The schedule won't be downloaded if its version matches [cachedVersion].
     */
    private fun download0(
        useHeadMethod: Boolean,
        file: File,
        vararg cachedVersions: String?,
        link: String = LINK,
        onDownloadProgress: (Float) -> Unit = { },
    ): DownloadResult {
        val url = try {
            URI(link).toURL()
        } catch (e: MalformedURLException) {
            return DownloadResult(ErrorType.MALFORMED_URL, e)
        } catch (e: URISyntaxException) {
            return DownloadResult(ErrorType.MALFORMED_URL, e)
        }

        val connection = try {
            url.openConnection()
        } catch (e: IOException) {
            return DownloadResult(ErrorType.OPENING_CONNECTION, e)
        }

        if (useHeadMethod)
            (connection as HttpURLConnection).requestMethod = "HEAD"

        try {
            connection.connect()
        } catch (e: IOException) {
            return DownloadResult(ErrorType.OPENING_CONNECTION, e)
        }

        val fieldValue = connection.getHeaderField("Content-Disposition")
        // no content, do not download anything
            ?: return DownloadResult(ErrorType.NO_CONTENT)

        val index = fieldValue.indexOf("filename=")
        if (index == -1)
        // no file name, do not download
            return DownloadResult(ErrorType.NO_FILENAME)

        val newVersion = FeedInfo.versionFromFileName(
            fieldValue
                .substring(index + 9)
                .replace("\"", "")
        )

        val contentLength = connection.contentLength.toFloat()

        val latestVersion = maxOf(*cachedVersions)

        if (latestVersion != null && latestVersion >= newVersion)
            return DownloadResult(ErrorType.UP_TO_DATE)
        else if (useHeadMethod)
            // there is no body, simply send a successful response
            return DownloadResult(version = newVersion)
        else try {
            val buffer = ByteArray(4096)
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                        onDownloadProgress(count / contentLength)
                    }
                }
            }
        } catch (e: IOException) {
            // failed to download the new version,
            // delete the incomplete schedule.
            file.delete()
            return DownloadResult(ErrorType.DOWNLOAD_ERROR, e, newVersion)
        }

        return DownloadResult(version = newVersion)
    }

    /**
     * Downloads the GTFS schedule from the specified [link] to [file].
     *
     * The schedule won't be downloaded if its version matches [cachedVersion].
     */
    public fun download(
        file: File,
        vararg cachedVersions: String?,
        link: String = LINK,
        onDownloadProgress: (Float) -> Unit = { },
    ): DownloadResult {
        // first just use the head method to see if there is a new version
        // doing a HEAD request saves around 700 kB of bandwidth if there's no version
        val headResult = download0(
            useHeadMethod = true, file, cachedVersions = cachedVersions, link, onDownloadProgress
        )
        // no new version, or error occurred: return head result
        if (headResult.version == null)
            return headResult

        // new version found: download it (also checks headers again but whatever)
        return download0(
            useHeadMethod = false, file, cachedVersions = cachedVersions, link, onDownloadProgress
        )
    }

    /** Loads the schedule in [gtfsScheduleZip]. */
    public fun load(
        gtfsScheduleZip: File,
        realtimeDispatcher: RealtimeDispatcher,
        onLoadProgress: (Float) -> Unit,
    ): Schedule {
        ZipFile(gtfsScheduleZip).use { zip ->
            val feedInfo = FeedInfo.fromZip(zip)
            val routes = GtfsRoute.loadRoutes(zip)
                .apply { sortBy { it.sortOrder } }
                .associateBy { it.id }

            // magic numbers given in onLoadProgress are approximate values
            // calculated from practical tests
            onLoadProgress(.01f)

            val stops = Stops(Love.giveMeBetterStops(loadStops(zip)))
            onLoadProgress(.07f)

            var shapes = loadShapes(zip)
            onLoadProgress(.30f)

            val fauxShapes = GtfsTripLoader.loadTrips(
                zip,
                routes,
                stops,
                shapes,
                realtimeDispatcher,
                onLoadProgress,
            )
            if (shapes is MutableMap)
                shapes.putAll(fauxShapes)
            else
                shapes = shapes + fauxShapes

            val calendarDates = loadCalendarDates(zip)
            val serviceIdTypes = Love.giveMeTheServiceIdTypes(routes, calendarDates)

            onLoadProgress(Float.NaN)

            return Schedule(
                feedInfo,
                routes,
                stops,
                shapes,
                calendarDates,
                serviceIdTypes,
            )
        }
    }

    public fun getFeedInfo(file: File): FeedInfo =
        ZipFile(file).use { FeedInfo.fromZip(it) }

    public fun getFeedInfoOrNull(file: File): FeedInfo? = try {
        getFeedInfo(file)
    } catch (_: Exception) {
        null
    }

}