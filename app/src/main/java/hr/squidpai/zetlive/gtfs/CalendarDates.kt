package hr.squidpai.zetlive.gtfs

import com.opencsv.CSVReader
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipFile

typealias ServiceId = String

/** Class containing data from the GTFS schedule file "calendar_dates.txt". */
class CalendarDates : AbstractList<ServiceId> {

  /**
   * The [ServiceId] used in each date of this schedule.
   *
   * Index 0 represents the [firstDate].
   */
  private val data: Array<ServiceId>

  /**
   * The first date that this schedule is available,
   * or `null` if it's empty.
   */
  private val firstDate: LocalDate?

  /**
   * The first date that this schedule is available,
   * or `0L` if it's empty.
   */
  private val firstDateEpoch: Long

  /**
   * Creates an empty `CalendarDates` instance.
   */
  constructor() {
    data = emptyArray()
    firstDate = null
    firstDateEpoch = 0L
  }

  /**
   * Creates a `CalendarDates` instance from the specified [zip] file.
   *
   * Loads from the file [name], which is by default "calendar_dates.txt".
   */
  constructor(zip: ZipFile, name: String = "calendar_dates.txt") {
    val allEntries = CSVReader(
      zip.getInputStream(zip.getEntry(name)).reader()
    ).use { it.readAll() }
    val entryIterator = allEntries.iterator()

    if (!entryIterator.hasNext()) {
      data = emptyArray()
      firstDate = null
      firstDateEpoch = 0L
      return
    }

    val header = entryIterator.next()
    val serviceIdOffset = header.indexOf("service_id")
    val dateOffset = header.indexOf("date")

    if (!entryIterator.hasNext()) {
      data = emptyArray()
      firstDate = null
      firstDateEpoch = 0L
      return
    }

    if (serviceIdOffset == -1) throw MalformedCsvException("no service_id")
    if (dateOffset == -1) throw MalformedCsvException("no date")

    data = Array(allEntries.size - 1) { entryIterator.next()[serviceIdOffset] }
    firstDate = allEntries[1][dateOffset].dateToLocalDate()
    firstDateEpoch = firstDate.toEpochDay()
  }

  override val size get() = data.size

  /**
   * Returns the [ServiceId] directly from the data.
   *
   * [index] is a regular index, which goes from 0 until [size].
   */
  override fun get(index: Int) = data[index]

  /**
   * Returns the [ServiceId] operating at the given date [epoch],
   * or `null` if there is no service that day.
   */
  operator fun get(epoch: Long) = data.getOrNull((epoch - firstDateEpoch).toInt())

  /**
   * Returns the [ServiceId] operating at the given [date],
   * or `null` if there is no service that day.
   */
  operator fun get(date: LocalDate) =
    firstDate?.let { data.getOrNull(ChronoUnit.DAYS.between(date, it).toInt()) }

  /**
   * Returns a list of [ServiceId] operating from epoch [dateEpoch] + [range].[first][IntRange.first]
   * to (including) [dateEpoch] + [range].[last][IntRange.last].
   */
  fun relativeSubList(dateEpoch: Long, range: IntRange): List<ServiceId?> {
    val dateOffset = dateEpoch - firstDateEpoch

    return RelativeSubList((dateOffset + range.first).toInt(), (dateOffset + range.last + 1).toInt())
  }

  private inner class RelativeSubList(private val fromIndex: Int, toIndex: Int) : AbstractList<ServiceId?>(), RandomAccess {
    private val _size: Int = toIndex - fromIndex

    override fun get(index: Int) = data.getOrNull(fromIndex + index)

    override val size: Int get() = _size
  }

}