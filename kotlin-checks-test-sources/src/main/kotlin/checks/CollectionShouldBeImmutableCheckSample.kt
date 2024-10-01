package checks

data class BoundElement2<out E>(
    val element: E, // Noncompliant
) {
    inline fun <reified T> downcast(): BoundElement2<T>? =
        if (element is T) BoundElement2(element) else null
}