package sources.kotlin
fun check(any: Any) = java.lang.String::class.isInstance(any) // Noncompliant
