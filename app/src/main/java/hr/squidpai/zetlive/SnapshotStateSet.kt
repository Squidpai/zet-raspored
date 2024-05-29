package hr.squidpai.zetlive

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord

@JvmInline
value class SnapshotStateSet<E> private constructor(private val map: SnapshotStateMap<E, Unit?>) : StateObject, MutableSet<E> {

  constructor() : this(SnapshotStateMap())

  override val firstStateRecord get() = map.firstStateRecord

  override fun prependStateRecord(value: StateRecord) = map.prependStateRecord(value)

  override fun add(element: E) = map.put(element, null) == Unit

  override fun addAll(elements: Collection<E>): Boolean {
    val map = map.toMap()
    val modified = elements.any { it in map }
    plusAssign(elements)
    return modified
  }

  @Suppress("RedundantUnitExpression")
  operator fun plusAssign(elements: Collection<E>) {
    map.putAll(elements.associateWith { Unit })
  }

  override val size get() = map.size

  override fun clear() = map.clear()

  override fun isEmpty() = map.isEmpty()

  override fun contains(element: E) = map.containsKey(element)

  override fun containsAll(elements: Collection<E>) = elements.all { contains(it) }

  override fun iterator() = map.keys.iterator()

  override fun retainAll(elements: Collection<E>) = map.keys.retainAll(elements)

  override fun removeAll(elements: Collection<E>) = map.keys.removeAll(elements)

  override fun remove(element: E) = map.keys.remove(element)

  /**
   * Returns an immutable set containing all elements from the original set.
   *
   * The content of the set returned will not change even if the content of the set is changed in
   * the same snapshot. It also will be the same instance until the content is changed. It is not,
   * however, guaranteed to be the same instance for the same content as adding and removing the
   * same item from this set might produce a different instance with the same content.
   *
   * This operation is O(1) and does not involve physically copying the set. It instead
   * returns the underlying immutable set used internally to store the content of the set.
   *
   * It is recommended to use [toSet] when using returning the value of this set from
   * [androidx.compose.runtime.snapshotFlow].
   */
  fun toSet() = map.toMap().keys

}

@StateFactoryMarker
fun <E> mutableStateSetOf() = SnapshotStateSet<E>()

@StateFactoryMarker
fun <E> mutableStateSetOf(vararg elements: E) =
  SnapshotStateSet<E>().apply { addAll(elements) }