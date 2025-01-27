package hr.squidpai.zetapi.gtfs

import hr.squidpai.zetapi.FeedInfo
import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.Schedule
import hr.squidpai.zetapi.Stops
import hr.squidpai.zetapi.realtime.RealtimeDispatcher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
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
   public fun download(
      file: File,
      link: String = LINK,
      cachedVersion: String? = null,
   ): DownloadResult {
      val url = try {
         URL(link)
      } catch (e: MalformedURLException) {
         return DownloadResult(ErrorType.MALFORMED_URL, e)
      }

      val connection = try {
         url.openConnection().also { it.connect() }
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

      if (newVersion == cachedVersion)
         return DownloadResult(ErrorType.UP_TO_DATE, version = newVersion)
      else try {
         connection.inputStream.use { input ->
            FileOutputStream(file).use { output ->
               output.channel.transferFrom(
                  Channels.newChannel(input),
                  0L, Long.MAX_VALUE,
               )
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

   /** Loads the schedule in [gtfsScheduleZip]. */
   public fun load(
      gtfsScheduleZip: File,
      realtimeDispatcher: RealtimeDispatcher
   ): Schedule {
      ZipFile(gtfsScheduleZip).use { zip ->
         val feedInfo = FeedInfo.fromZip(zip)
         val routes = GtfsRoute.loadRoutes(zip)
            .apply { sortBy { it.sortOrder } }
            .associateBy { it.id }
         val stops = Stops(loadStops(zip))
         val shapes = loadShapes(zip)

         GtfsTripLoader.loadTrips(
            zip,
            routes,
            stops,
            shapes,
            realtimeDispatcher
         )

         val calendarDates = loadCalendarDates(zip)

         val serviceIdTypes =
            Love.giveMeTheServiceIdTypes(routes, calendarDates)

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