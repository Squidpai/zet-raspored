package hr.squidpai.zetlive

data class ListPair<out E>(val first: List<E>, val second: List<E>) {
  override fun toString() = "($first, $second)"

  inline fun <R> map(transform: (E) -> R) = ListPair(
    first.map(transform),
    second.map(transform),
  )

  fun <R> mapNoInline(transform: (E) -> R) = map(transform)
}

private val emptyListPair = ListPair<Nothing>(emptyList(), emptyList())

fun <E> emptyListPair(): ListPair<E> = emptyListPair
