package hr.squidpai.zetapi.gtfs

import com.opencsv.CSVReader
import hr.squidpai.zetapi.CalendarDates
import hr.squidpai.zetapi.MalformedCsvException
import hr.squidpai.zetapi.dateToLocalDate
import java.util.zip.ZipFile

internal fun loadCalendarDates(zip: ZipFile): CalendarDates {
   val allEntries = CSVReader(
      zip.getInputStream(zip.getEntry("calendar_dates.txt")).reader()
   ).use { it.readAll() }

   if (allEntries.size < 2)
      throw MalformedCsvException("Empty CalendarDates file.")

   val entryIterator = allEntries.iterator()

   val header = entryIterator.next()
   val serviceIdOffset = header.indexOf("service_id")
   val dateOffset = header.indexOf("date")

   if (serviceIdOffset == -1) throw MalformedCsvException("no service_id")
   if (dateOffset == -1) throw MalformedCsvException("no date")

   val firstDate = allEntries[1][dateOffset].dateToLocalDate()

   return CalendarDates(
      data = Array(allEntries.size - 1) {
         entryIterator.next()[serviceIdOffset]
      },
      firstDate,
      firstDateEpoch = firstDate.toEpochDay(),
      lastDate = allEntries.last()[dateOffset].dateToLocalDate(),
   )
}