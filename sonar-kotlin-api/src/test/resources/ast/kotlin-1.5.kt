@JvmRecord
data class User(val name: String, val age: Int)

sealed interface Polygon

@JvmInline
value class Password(val s: String)


