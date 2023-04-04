package org.sonarsource.kotlin.api

/**
 * Returns `true` if the predicate does match for all element pairs (`this[i]`,`other[i]`) of both lists.
 * This implies that both lists must have the same size.
 */
inline fun <T, U> List<T>.allPaired(other: List<U>, predicate: (T, U) -> Boolean) =
    size == other.size && run {
        forEachIndexed { index, value ->
            if (!predicate(value, other[index])) {
                return false
            }
        }
        true
    }
