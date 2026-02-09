package pt.rikmartins.clubemg.mobile

/**
 * Adds the [element] to the collection if it satisfies the [predicate].
 *
 * @return `true` if the element was added, `false` otherwise. Note that for collections
 * that do not allow duplicates (like a [Set]), this method will also return `false`
 * if the predicate is true but the element is already present in the collection.
 */
internal fun <E> MutableCollection<E>.addWhen(element: E, predicate: (E) -> Boolean): Boolean =
    if (predicate(element)) add(element)
    else false

/**
 * Adds all elements from the given [elements] collection that satisfy the [predicate].
 * @return `true` if the collection was changed as a result of the call, `false` otherwise.
 */
internal fun <E> MutableCollection<E>.addAllWhen(elements: Collection<E>, predicate: (E) -> Boolean): Boolean {
    var modified = false
    for (element in elements) {
        modified = addWhen(element, predicate) || modified
    }
    return modified
}
