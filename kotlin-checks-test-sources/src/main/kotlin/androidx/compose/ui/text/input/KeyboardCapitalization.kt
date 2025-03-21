package androidx.compose.ui.text.input

@JvmInline
value class KeyboardCapitalization private constructor(private val value: Int) {
    companion object {
        val Unspecified = KeyboardCapitalization(-1)
        val None = KeyboardCapitalization(0)
        val Characters = KeyboardCapitalization(1)
        val Words = KeyboardCapitalization(2)
        val Sentences = KeyboardCapitalization(3)
    }
}
