package hr.squidpai.zetlive

open class SortedListMap<K : Comparable<K>, V>(
  source: List<V>,
  assumeSorted: Boolean = false,
  val keyFactory: (V) -> K,
) : List<V>, Map<K, V> {

  protected val list = if (assumeSorted) source else source.sortedBy(keyFactory)

  override val size get() = list.size

  override fun get(index: Int) = list[index]

  override fun isEmpty() = list.isEmpty()

  open fun binarySearchKey(key: K) = binarySearch { keyFactory(it).compareTo(key) }

  open fun binarySearchValue(value: V) = binarySearchKey(keyFactory(value))

  override fun indexOf(element: V) = binarySearchValue(element).coerceAtLeast(-1)

  override fun lastIndexOf(element: V) = indexOf(element)

  override fun contains(element: V) = indexOf(element) >= 0

  override fun containsAll(elements: Collection<V>) = elements.all { contains(it) }

  override fun iterator() = list.iterator()

  override fun listIterator() = list.listIterator()

  override fun listIterator(index: Int) = list.listIterator(index)

  override fun subList(fromIndex: Int, toIndex: Int) = SortedListMap(list.subList(fromIndex, toIndex), assumeSorted = true, keyFactory)

  protected var entriesSet: EntriesSet? = null

  override val entries: Set<Map.Entry<K, V>> get() = entriesSet ?: EntriesSet().also { entriesSet = it }

  protected var keySet: KeyListSet? = null

  override val keys: Set<K> get() = keySet ?: KeyListSet().also { keySet = it }

  override val values get() = list

  override fun get(key: K) = binarySearchKey(key).let {
    if (it >= 0) list[it]
    else null
  }

  override fun containsValue(value: V) = contains(value)

  override fun containsKey(key: K) = binarySearchKey(key) >= 0

  protected open inner class KeyIterator : Iterator<K> {
    protected val iterator = list.iterator()

    override fun hasNext() = iterator.hasNext()

    override fun next() = keyFactory(iterator.next())
  }

  protected open inner class KeyListSet : Set<K> {

    override val size get() = list.size

    override fun isEmpty() = this@SortedListMap.isEmpty()

    override fun iterator(): Iterator<K> = KeyIterator()

    override fun contains(element: K) = containsKey(element)

    override fun containsAll(elements: Collection<K>) = elements.all { contains(it) }

  }

  protected open inner class EntriesIterator : Iterator<Map.Entry<K, V>> {
    protected val iterator = this@SortedListMap.iterator()

    override fun hasNext() = iterator.hasNext()

    override fun next() = object : Map.Entry<K, V> {
      override val value = iterator.next()

      override val key get() = keyFactory(value)
    }
  }

  protected open inner class EntriesSet : Set<Map.Entry<K, V>> {

    override val size get() = list.size

    override fun isEmpty() = this@SortedListMap.isEmpty()

    override fun iterator(): Iterator<Map.Entry<K, V>> = EntriesIterator()

    override fun contains(element: Map.Entry<K, V>): Boolean {
      val index = this@SortedListMap.indexOf(element.value)

      return index >= 0 && element.key == keyFactory(get(index))
    }

    override fun containsAll(elements: Collection<Map.Entry<K, V>>) = elements.all { contains(it) }
  }

  override fun toString() = list.joinToString(prefix = "SortedListMap[", postfix = "]")

  fun <N : Comparable<N>> resorted(newKeyFactory: (V) -> N): SortedListMap<N, V> {
    return SortedListMap(list, assumeSorted = false, newKeyFactory)
  }

}

@Suppress("ObjectPropertyName")
val _emptySortedListMap = SortedListMap<Nothing, Nothing>(emptyList()) { throw NoSuchElementException() }

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <K : Comparable<K>, V> emptySortedListMap(): SortedListMap<K, V> = _emptySortedListMap as SortedListMap<K, V>

@Suppress("NOTHING_TO_INLINE")
inline fun <K : Comparable<K>, V> List<V>.toSortedListMap(noinline keyFactory: (V) -> K) = SortedListMap(this, assumeSorted = false, keyFactory)

@Suppress("NOTHING_TO_INLINE")
inline fun <K : Comparable<K>, V> List<V>.asSortedListMap(noinline keyFactory: (V) -> K) = SortedListMap(this, assumeSorted = true, keyFactory)

/**
 * Returns a [SortedListMap] containing all key-value pairs with keys matching the given [predicate].
 *
 * The returned `SortedListMap` preserves the entry iteration order of the original `SortedListMap`.
 */
inline fun <K : Comparable<K>, V> SortedListMap<K, V>.filterByKey(predicate: (K) -> Boolean) = buildList {
  for (v in this@filterByKey) if (predicate(keyFactory(v))) add(v)
}.asSortedListMap(keyFactory)

/**
 * Returns a [SortedListMap] containing all key-value pairs with values matching the given [predicate].
 *
 * The returned `SortedListMap` preserves the entry iteration order of the original `SortedListMap`.
 */
inline fun <K : Comparable<K>, V> SortedListMap<K, V>.filterByValue(predicate: (V) -> Boolean) =
  (this as List<V>).filter(predicate).asSortedListMap(keyFactory)

inline fun <K : Comparable<K>, V> SortedListMap<K, V>.mapSorted(transform: (V) -> V) =
  (this as List<V>).map(transform).asSortedListMap(keyFactory)

/** Returns `true` if at least one element matches the given [predicate]. */
inline fun <K: Comparable<K>, V> SortedListMap<K, V>.any(predicate: (V) -> Boolean): Boolean {
  for (v in this) if (predicate(v)) return true
  return false
}
