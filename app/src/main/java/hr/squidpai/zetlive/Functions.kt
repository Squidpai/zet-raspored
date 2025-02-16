package hr.squidpai.zetlive

import androidx.collection.IntIntMap
import androidx.collection.IntList
import androidx.collection.IntObjectMap
import androidx.collection.IntSet
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.compose.ui.Modifier
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.LocalDate
import kotlin.enums.enumEntries

/**
 * Returns 1 if `this` is `true`, 0 otherwise.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toInt() = if (this) 1 else 0

/**
 * Returns [trueValue] if `this` is `true`, 0 otherwise.
 */
inline fun Boolean.toInt(trueValue: () -> Int) = if (this) trueValue() else 0

/**
 * Converts the string of contents "YYYYMMDD" into a [LocalDate].
 */
fun String.dateToLocalDate(): LocalDate {
  val year = this.substring(0, 4).toInt()
  val month = this.substring(4, 6).toInt()
  val date = this.substring(6, 8).toInt()

  return LocalDate.of(year, month, date)
}

fun String.toIntOrHashCode() = toIntOrNull() ?: hashCode()

/**
 * The text "Učitavanje...".
 *
 * Used as a placeholder if some piece of text isn't loaded yet.
 */
const val LOADING_TEXT = "Učitavanje\u2026"

/**
 * Returns `this` if it is not `null`, otherwise the text "Učitavanje...".
 */
@Suppress("NOTHING_TO_INLINE")
inline fun String?.orLoading() = this ?: LOADING_TEXT

/**
 * Concatenates `this` with [block] if [condition] is `true`,
 * otherwise, returns `this`.
 */
inline fun <T : R, R> T.alsoIf(condition: Boolean, block: T.() -> R) =
  if (condition) this.block() else this

/**
 * Concatenates this modifier with [block] if [condition] is `false`,
 * otherwise, returns just this modifier.
 */
inline fun Modifier.alsoIfNot(condition: Boolean, block: Modifier.() -> Modifier) =
  if (!condition) this.block() else this

/**
 * Concatenates `this` with [ifTrue] if [condition] is `true`,
 * and with [ifFalse] otherwise.
 */
inline fun <T : R, R> T.ifElse(
  condition: Boolean,
  ifTrue: T.() -> R,
  ifFalse: T.() -> R,
) = if (condition) this.ifTrue() else this.ifFalse()

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original `IntList`.
 */
inline fun <R> IntList.map(transform: (Int) -> R) =
  List(this.size) { transform(get(it)) }

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original `IntList`.
 */
inline fun <R : Any> IntList.mapNotNull(transform: (Int) -> R?): List<R> {
  val list = ArrayList<R>()
  forEach { element -> transform(element)?.let { list.add(it) } }
  return list
}

/**
 * Returns an [IntObjectMap] where keys are elements from the given [IntList] and values are
 * produced by the [valueSelector] function applied to each element.
 *
 * If any two elements are equal, the last one gets added to the map.
 */
inline fun <V> IntList.associateWith(valueSelector: (Int) -> V): IntObjectMap<V> {
  val result = MutableIntObjectMap<V>(size)
  forEach {
    result[it] = valueSelector(it)
  }
  return result
}

/** Returns `true` if no elements match the given [predicate]. */
inline fun IntList.none(predicate: (Int) -> Boolean): Boolean {
  forEach { if (predicate(it)) return false }
  return true
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original `IntSet`.
 */
inline fun <R> IntSet.map(transform: (Int) -> R): List<R> {
  val list = ArrayList<R>(size)
  forEach { list += transform(it) }
  return list
}

/**
 * Returns an [IntArray] which contains all elements from
 * the original `IntSet`.
 */
fun IntSet.toIntArray(): IntArray {
  val array = IntArray(size)
  var i = 0
  forEach { array[i++] = it }

  return array
}

/**
 * Represents a pair of [Int] and a generic object.
 *
 * @param B type of the second value.
 * @property first First value.
 * @property second Second value.
 * @constructor Creates a new instance of Pair.
 */
data class IntObjectPair<out B>(val first: Int, val second: B) {
  override fun toString() = "($first, $second)"
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun <B> Int.to(that: B) = IntObjectPair(this, that)

fun <V> intObjectMapOf(vararg pairs: IntObjectPair<V>): IntObjectMap<V> = MutableIntObjectMap<V>().apply {
  for ((key, value) in pairs) {
    put(key, value)
  }
}


data class IntPair(val first: Int, val second: Int) {
  override fun toString() = "($first, $second)"
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun Int.to(that: Int) = IntPair(this, that)

fun intIntMapOf(vararg pairs: IntPair): IntIntMap = MutableIntIntMap().apply {
  for ((key, value) in pairs) {
    put(key, value)
  }
}

inline fun <V> buildIntObjectMap(builderAction: MutableIntObjectMap<V>.() -> Unit): IntObjectMap<V> =
  MutableIntObjectMap<V>().apply(builderAction)

private val intExtractRegex = Regex("\\D")

fun String.extractInt(): Int {
  val num = this.replace(intExtractRegex, "")
  return if (num.isEmpty()) -1 else num.toLong().toInt()
}

fun Any?.printIsNull(objName: String) = objName + (if (this == null) " is null" else " is not null")

fun String.decrementInt(): String {
  val output = this.toCharArray()
  for (i in length - 1 downTo 0) {
    if (output[i] in '1'..'9') {
      output[i]--
      break
    } else if (output[i] == '0') {
      output[i] = '9'
    }
  }
  return output.concatToString()
}

fun DataOutputStream.writeShortString(string: String) {
  if (string.isEmpty()) {
    writeByte(0)
    return
  }
  val buff = string.encodeToByteArray()
  if (buff.size > 255)
    throw IllegalArgumentException("Short string too long: 255 < string.toByteArray().size = ${buff.size}")
  writeByte(buff.size)
  write(buff)
}

fun DataInputStream.readShortString(): String {
  val size = readUnsignedByte()
  if (size == 0)
    return ""
  val buff = ByteArray(size)
  read(buff)
  return buff.decodeToString()
}

/**
 * Returns the [first][Pair.first] value if [index] is 0,
 * the [second][Pair.second] value if [index] is 1, and
 * throws [IndexOutOfBoundsException] otherwise.
 */
operator fun <T> Pair<T, T>.get(index: Int) = when (index) {
  0 -> first
  1 -> second
  else -> throw IndexOutOfBoundsException("Index out of range: $index")
}

operator fun <T> Pair<T, *>?.component1() = this?.first

operator fun <T> Pair<*, T>?.component2() = this?.second

/**
 * Returns the enum entry whose ordinal is one more than this, or
 * whose ordinal is zero, if this is the last entry.
 */
inline val <reified T : Enum<T>> T.next get() = enumEntries<T>().let { it[(ordinal + 1) % it.size] }

/**
 * Checks if all elements are sorted according to the natural sort order
 * of the value returned by the specified [selector] function.
 * If not, it sorts them by that.
 *
 * This function is useful if the list is very probably already sorted,
 * as to not create a large memory overhead from the merge sort.
 *
 * The sort is _stable_. It means that equal elements preserve
 * their order relative to each other after sorting.
 */
// This function is a tad bit too long to be inlined.
fun <T, R : Comparable<R>> Iterable<T>.sortedByIfNotAlready(
  selector: (T) -> R?
): Iterable<T> {
  val iterator = iterator()
  if (!iterator.hasNext())
    return this

  var previous = iterator.next()
  var sorted = true
  for (current in iterator) {
    // if previous > current
    if (compareValues(selector(previous), selector(current)) > 0) {
      sorted = false
      break
    }
    previous = current
  }
  if (sorted)
    return this

  return sortedBy(selector)
}
