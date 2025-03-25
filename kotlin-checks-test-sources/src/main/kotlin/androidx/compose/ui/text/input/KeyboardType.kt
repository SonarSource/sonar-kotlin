package androidx.compose.ui.text.input

@JvmInline
value class KeyboardType private constructor(private val value: Int) {
    companion object {
        val Unspecified: KeyboardType = KeyboardType(0)
        val Text: KeyboardType = KeyboardType(1)
        val Ascii: KeyboardType = KeyboardType(2)
        val Number: KeyboardType = KeyboardType(3)
        val Phone: KeyboardType = KeyboardType(4)
        val Uri: KeyboardType = KeyboardType(5)
        val Email: KeyboardType = KeyboardType(6)
        val Password: KeyboardType = KeyboardType(7)
        val NumberPassword: KeyboardType = KeyboardType(8)
        val Decimal: KeyboardType = KeyboardType(9)
    }
}
