@file:Suppress("NOTHING_TO_INLINE")

package hr.squidpai.zetapi

import androidx.collection.IntIntMap
import androidx.collection.IntIntPair
import androidx.collection.IntList
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.time.LocalDate

/**
 * Converts the string of contents "YYYYMMDD" into a [LocalDate].
 */
internal fun String.dateToLocalDate(): LocalDate {
   val year = substring(0, 4).toInt()
   val month = substring(4, 6).toInt()
   val date = substring(6, 8).toInt()

   return LocalDate.of(year, month, date)
}

internal fun LocalDate.localDateToDate() =
   "$year${month.value.toString().padStart(2, '0')}${
      dayOfMonth.toString().padStart(2, '0')
   }"

public fun String.toIntOrHashCode(): Int = toIntOrNull() ?: hashCode()

public fun intIntMapOf(vararg pairs: Int): IntIntMap {
   val map = MutableIntIntMap()
   repeat(pairs.size / 2) { i ->
      map.put(pairs[i * 2], pairs[i * 2 + 1])
   }
   return map
}

/**
 * Returns the [first][Pair.first] value if [DirectionId.isZero],
 * the [second][Pair.second] value if [DirectionId.isOne].
 */
public operator fun <T> Pair<T, T>.get(directionId: DirectionId): T =
   if (directionId.isZero) first else second

/**
 * Returns the [first][IntIntPair.first] value if [DirectionId.isZero],
 * the [second][IntIntPair.second] value if [DirectionId.isOne].
 */
public operator fun IntIntPair.get(directionId: DirectionId): Int =
   if (directionId.isZero) first else second

/**
 * Retains only elements of this [MutableList] that are not `null`.
 *
 * @return `this`
 */
public fun <T> MutableList<T?>.filterInPlaceAllNotNull(): MutableList<T & Any> {
   retainAll { it != null }
   @Suppress("UNCHECKED_CAST")
   return this as MutableList<T & Any>
}

/**
 * Returns a new map with entries having the keys and the values
 * obtained by applying the [transform] function to each entry in this [Map].
 *
 * The returned map preserves the entry iteration order of the original map.
 */
public inline fun <K, V, NK, NV> Map<out K, V>.mapEntries(
   transform: (Map.Entry<K, V>) -> Pair<NK, NV>
): Map<NK, NV> = entries.associate(transform)

public inline fun Boolean.putFlag(place: Int): Int =
   if (this) 1 shl place else 0

public inline fun Int.getFlag(place: Int): Boolean =
   this and (1 shl place) != 0

internal fun CSVReader.readNextIntList(): IntList {
   val next = readNext() ?: return emptyIntList()
   val result = MutableIntList(next.size)
   for (i in next.indices)
      result.add(next[i].toInt())
   return result
}

/** Writes the next line to the file. */
// special function for writing ints to avoid boxing
internal inline fun CSVWriter.writeNext(vararg nextLine: Int) =
   writeNext(Array(nextLine.size) { nextLine[it].toString() })

internal fun CSVWriter.writeNext(nextLine: IntList) =
   writeNext(Array(nextLine.size) { nextLine[it].toString() })

/** Writes the next line to the file. */
// special function for writing strings to avoid creating an additional array
internal inline fun CSVWriter.writeNext(vararg nextLine: String?) =
   writeNext(nextLine)

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
public fun lerp(start: Int, stop: Int, fraction: Float): Int =
   Math.round(start + ((stop - start) * fraction.toDouble())).toInt()

/**
 * Returns the greater of two values.
 *
 * If values are equal, returns the first one.
 *
 * `null` is considered the smallest element.
 */
public fun <T : Comparable<T>> maxOf(a: T?, b: T?): T? = when {
   b == null -> a
   a == null -> b
   a >= b -> a
   else -> b
}

/**
 * Returns the greatest of the given values.
 *
 * If there are multiple values that are the greatest, returns the first one.
 *
 * `null` is considered the smallest element.
 *
 * If no elements are given, `null` is returned.
 */
public fun <T : Comparable<T>> maxOf(vararg elements: T?): T? {
   if (elements.isEmpty()) return null
   var max = elements.first()
   for (i in 1..<elements.size)
      max = maxOf(max, elements[i])
   return max
}