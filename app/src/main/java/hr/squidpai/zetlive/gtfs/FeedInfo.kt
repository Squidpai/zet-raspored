package hr.squidpai.zetlive.gtfs

import com.opencsv.CSVReader
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipFile

/** Class containing data from the GTFS schedule file "feed_info.txt". */
class FeedInfo(
  val startDate: LocalDate,
  val version: String,
)

/**
 * Creates a [FeedInfo] instance from the specified zip file.
 */
val ZipFile.feedInfo: FeedInfo
  get() {
    val (header, data) =
      CSVReader(getInputStream(getEntry("feed_info.txt")).reader())
        .use { it.readAll() }
    var startDateIndex = -1
    var versionIndex = -1
    header.forEachIndexed { i, h ->
      if (h == "feed_start_date")
        startDateIndex = i
      else if (h == "feed_version")
        versionIndex = i
    }
    return FeedInfo(data[startDateIndex].dateToLocalDate(), data[versionIndex])
  }

/**
 * Creates a [FeedInfo] instance from the specified file.
 */
val File.feedInfo
  get() = ZipFile(this).use { it.feedInfo }

/**
 * Returns the feed version from the file name.
 *
 * The string is expected to have the form `"zet-gtfs-scheduled-000-nnnnnn.zip"`
 * where `nnnnnn` is the returned value.
 */
val String.feedVersion
  // zet-gtfs-scheduled-000-xxxxxx.zip
  //           returns this ^^^^^^
  get() = this.substring(23, 29)

/**
 * Creates a [FeedInfo] instance from the specified file
 * and returns the version associated with it.
 */
val File.feedVersion
  get() = ZipFile(this).use { it.feedInfo.version }
