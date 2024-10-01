package checks

data class BoundElement<out E>(
    val element: E, // Noncompliant
) {
    inline fun <reified T> downcast(): BoundElement<T>? =
        if (element is T) BoundElement(element) else null
}
