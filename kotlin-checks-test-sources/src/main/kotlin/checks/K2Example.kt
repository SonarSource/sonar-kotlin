package checks

// https://kotlinlang.org/docs/whatsnew20.html#smart-cast-improvements
private fun k2(any: Any) {
    if (any is MutableList<*>) {
        any.get(0) // Noncompliant
    }

    val isList = any is MutableList<*>
    if (isList) {
        any.get(0) // Noncompliant
    }
}
