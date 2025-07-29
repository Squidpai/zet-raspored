package hr.squidpai.zetapi

import com.opencsv.CSVReader
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Mapping that maps which header entry is at which index,
 * to more quickly and efficiently load csv data.
 */
internal typealias CsvHeaderMapping = (header: Array<out String>) -> IntArray

/**
 * Function that takes in the data from csv and using
 * the header map created by [CsvHeaderMapping] to construct
 * the object of type `T`.
 */
internal typealias CsvFactory<T> =
            (headerMap: IntArray, data: Array<out String>) -> T

/**
 * Function that takes in the data from csv and using
 * the header map created by [CsvHeaderMapping] to construct
 * the object of type `T`.
 *
 * This function also takes in the previous object created
 * (which is `null` on the first element) and an arbitrary
 * context which can be of any type.
 *
 * If this function returns `null`, it means not to append anything,
 * for example, because new data was appended to the previous element.
 */
internal typealias SequentialCsvFactory<T> = (
    headerMap: IntArray,
    data: Array<out String>,
    previous: T?,
) -> T?

/**
 * Converts the csv file [name] in this zip file into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the zip file.
 */
internal fun <T> ZipFile.csvToListFromEntry(
    name: String,
    headerMapping: CsvHeaderMapping,
    factory: CsvFactory<T>
) = getInputStream(getEntry(name)).use { it.csvToList(headerMapping, factory) }

/**
 * Converts the csv data read by this `InputStream`
 * into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the input stream.
 */
internal fun <T> InputStream.csvToList(
    headerMapping: CsvHeaderMapping,
    factory: CsvFactory<T>
) = CSVReader(this.bufferedReader()).toList(headerMapping, factory)

/**
 * Converts the csv data into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the reader.
 *
 * @throws MalformedCsvException if any [Exception] is thrown
 * while reading the data
 */
internal fun <T> CSVReader.toList(
    headerMapping: CsvHeaderMapping,
    factory: CsvFactory<T>
): MutableList<T> {
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
 * Converts the csv file [name] in this zip file into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the zip file.
 */
internal fun <T> ZipFile.csvToListSequentialFromEntry(
    name: String,
    estimatedListSize: Int,
    headerMapping: CsvHeaderMapping,
    factory: SequentialCsvFactory<T>,
) = getInputStream(getEntry(name)).use {
    it.csvToListSequential(
        estimatedListSize,
        headerMapping,
        factory,
    )
}

/**
 * Converts the csv data read by this `InputStream`
 * into a [MutableList] of type `T`.
 *
 * **Note:** it is the caller's responsibility to close the input stream.
 */
internal fun <T> InputStream.csvToListSequential(
    estimatedListSize: Int,
    headerMapping: CsvHeaderMapping,
    factory: SequentialCsvFactory<T>,
) = CSVReader(this.bufferedReader()).toListSequential(
    estimatedListSize,
    headerMapping,
    factory,
)

/**
 * Converts the csv data into a [MutableList] of type `T`.
 *
 * This method uses the [SequentialCsvFactory] which takes in
 * the previously saved element.
 *
 * **Note:** it is the caller's responsibility to close the reader.
 *
 * @throws MalformedCsvException if any [Exception] is thrown while reading the data
 */
internal fun <T> CSVReader.toListSequential(
    estimatedListSize: Int,
    headerMapping: CsvHeaderMapping,
    factory: SequentialCsvFactory<T>,
): MutableList<T> {
    val headerMap = headerMapping(readNext() ?: return mutableListOf())

    val list = ArrayList<T>(estimatedListSize)

    var previous: T? = null
    var count = 0

    while (true) {
        val line = readNext() ?: break
        count++

        /*if (count and (1 shl 14) - 1 == 0)
           onUpdateLoadingState(count.toFloat() / estimatedListSize)*/

        val current = try {
            factory(headerMap, line, previous)
        } catch (e: Exception) {
            throw MalformedCsvException("for line ${line.contentToString()}", e)
        }

        if (current != null) {
            list += current
            previous = current
        }
    }

    return list
}

internal class MalformedCsvException(
    message: String,
    cause: Exception? = null
) : RuntimeException(message, cause)
