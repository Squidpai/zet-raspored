package hr.squidpai.zetlive.gtfs

import com.opencsv.CSVReader
import java.io.InputStream
import java.time.LocalDate
import java.util.zip.ZipFile

/**
 * Mapping that maps which header entry is at which index,
 * to more quickly and efficiently load csv data.
 */
typealias CsvHeaderMapping = (header: Array<out String>) -> IntArray

/**
 * Function that takes in the data from csv and using
 * the header map created by [CsvHeaderMapping] to construct
 * the object of type `T`.
 */
typealias CsvFactory<T> = (headerMap: IntArray, data: Array<out String>) -> T

/**
 * Function that takes in the data from csv and using
 * the header map created by [CsvHeaderMapping] to construct
 * the object of type `T`.
 *
 * This function also takes in the previous object created
 * (which is `null` on the first element) and an arbitrary
 * context which can be of any type.
 *
 * If this function returns `null`, it means not to append this
 * element, for example, because it just updated data of the
 * previous element.
 */
typealias SequentialCsvFactory<T, C> = (headerMap: IntArray, data: Array<out String>, previous: T?, context: C) -> T?

/**
 * Converts the csv file [name] in this zip file into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the zip file.
 */
fun <T> ZipFile.csvToListFromEntry(name: String, headerMapping: CsvHeaderMapping, factory: CsvFactory<T>) =
  getInputStream(getEntry(name)).use { it.csvToList(headerMapping, factory) }

/**
 * Converts the csv data read by this `InputStream`
 * into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the input stream.
 */
fun <T> InputStream.csvToList(headerMapping: CsvHeaderMapping, factory: CsvFactory<T>) =
  CSVReader(this.bufferedReader()).toList(headerMapping, factory)

/**
 * Converts the csv data into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the reader.
 *
 * @throws MalformedCsvException if any [Exception] is thrown while reading the data
 */
fun <T> CSVReader.toList(headerMapping: CsvHeaderMapping, factory: CsvFactory<T>): MutableList<T> {
  val allEntries = this.readAll()
  val entryIterator = allEntries.iterator()

  if (!entryIterator.hasNext()) {
    return mutableListOf()
  }

  val headerMap = headerMapping(entryIterator.next())

  return MutableList(allEntries.size - 1) {
    val line = entryIterator.next()
    try {
      factory(headerMap, line)
    } catch (e: Exception) {
      throw MalformedCsvException("for line ${line.contentToString()}", e)
    }
  }
}

/**
 * Converts the csv data into a [MutableList] of type `T`.
 *
 * This method uses the [SequentialCsvFactory] which takes in an
 * arbitrary [context] as well.
 *
 * This method also updates the [Schedule.loadingState] as the
 * data is loading.
 *
 * **Note:** it is the caller's responsibility to close the reader.
 *
 * @throws MalformedCsvException if any [Exception] is thrown while reading the data
 */
fun <T, C> CSVReader.toListSequential(
  context: C,
  headerMapping: CsvHeaderMapping,
  factory: SequentialCsvFactory<T, C>,
  priorityLevel: TripsLoader.PriorityLevel,
): MutableList<T> {
  val headerMap = headerMapping(readNext() ?: return mutableListOf())

  val list = ArrayList<T>(1_300_000)

  var previous: T? = null
  var count = 0

  val loadingState = when (priorityLevel) {
    TripsLoader.PriorityLevel.Hidden -> null

    TripsLoader.PriorityLevel.Background ->
      Schedule.Companion.TrackableLoadingState("Ažuriranje rasporeda${Typography.ellipsis}")
        .also { Schedule.loadingState = it }

    TripsLoader.PriorityLevel.Foreground ->
      Schedule.Companion.TrackableLoadingState("Pripremanje rasporeda za prvo pokretanje${Typography.ellipsis}")
        .also { Schedule.priorityLoadingState = it }
  }

  while (true) {
    val line = readNext() ?: break
    count++

    if (count % 20_000 == 0) {
      loadingState?.progress = count / 1_100_000f
    }

    val current = try {
      factory(headerMap, line, previous, context)
    } catch (e: Exception) {
      throw MalformedCsvException("for line ${line.contentToString()}", e)
    }

    if (current != null) {
      list += current
      previous = current
    }
  }

  when (priorityLevel) {
    TripsLoader.PriorityLevel.Hidden -> {}
    TripsLoader.PriorityLevel.Background -> Schedule.loadingState = null
    TripsLoader.PriorityLevel.Foreground -> Schedule.priorityLoadingState = null
  }

  return list
}

class MalformedCsvException(message: String, cause: Exception? = null) : RuntimeException(message, cause)

/**
 * Converts the string of contents "YYYYMMDD" into a [LocalDate].
 */
fun String.dateToLocalDate(): LocalDate {
  val year = this.substring(0, 4).toInt()
  val month = this.substring(4, 6).toInt()
  val date = this.substring(6, 8).toInt()

  return LocalDate.of(year, month, date)
}
