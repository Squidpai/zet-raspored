package hr.squidpai.zetapi

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.time.LocalDate
import java.util.zip.ZipFile

/** Class containing data from the GTFS schedule file "feed_info.txt". */
public data class FeedInfo internal constructor(
   val startDate: LocalDate,
   val version: String,
) {
   internal fun save(writer: CSVWriter) {
      writer.writeNext("feed_start_date", "feed_version")
      writer.writeNext(startDate.localDateToDate(), version)
   }

   public fun started(epoch: Long): Boolean = epoch >= startDate.toEpochDay()

   internal companion object {
      internal fun fromZip(
         zip: ZipFile,
         name: String = "feed_info.txt",
      ): FeedInfo {
         val (header, data) =
            CSVReader(zip.getInputStream(zip.getEntry(name)).reader())
               .use { it.readAll() }
         var startDateIndex = -1
         var versionIndex = -1
         header.forEachIndexed { i, h ->
            if (h == "feed_start_date")
               startDateIndex = i
            else if (h == "feed_version")
               versionIndex = i
         }
         return FeedInfo(
            data[startDateIndex].dateToLocalDate(),
            data[versionIndex],
         )
      }

      /**
       * Returns the feed version from the file name.
       *
       * The string is expected to have the form `"zet-gtfs-scheduled-000-nnnnnn.zip"`
       * where `nnnnnn` is the returned value.
       */
      internal fun versionFromFileName(name: String): String =
         name.substring(23, 29)
   }
}
