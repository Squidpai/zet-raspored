package hr.squidpai.zetlive

import androidx.collection.*
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import java.io.DataInputStream
import java.io.DataOutputStream
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
 * Concatenates this modifier with [block] if [condition] is `true`,
 * otherwise, returns just this modifier.
 */
inline fun Modifier.alsoIf(condition: Boolean, block: Modifier.() -> Modifier) =
  if (condition) this.block() else this

/**
 * Concatenates this modifier with [block] if [condition] is `false`,
 * otherwise, returns just this modifier.
 */
inline fun Modifier.alsoIfNot(condition: Boolean, block: Modifier.() -> Modifier) =
  if (!condition) this.block() else this

/**
 * Concatenates this modifier with [ifTrue] if [condition] is `true`,
 * otherwise, with [ifFalse].
 */
inline fun Modifier.ifElse(
  condition: Boolean,
  ifTrue: Modifier.() -> Modifier,
  ifFalse: Modifier.() -> Modifier,
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

private val nullState = object : State<Nothing?> {
  override val value = null
}

fun <T> nullState(): State<T?> = nullState

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
  val buff = string.encodeToByteArray()
  if (buff.size > 255)
    throw IllegalArgumentException("Short string too long (string.toByteArray().size = ${buff.size} > 255")
  writeByte(buff.size)
  write(buff)
}

fun DataInputStream.readShortString(): String {
  val size = readUnsignedByte()
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

/**
 * Returns the enum entry whose ordinal is one more than this, or
 * whose ordinal is zero, if this is the last entry.
 */
@OptIn(ExperimentalStdlibApi::class)
inline val <reified T : Enum<T>> T.next get() = enumEntries<T>().let { it[(ordinal + 1) % it.size] }
