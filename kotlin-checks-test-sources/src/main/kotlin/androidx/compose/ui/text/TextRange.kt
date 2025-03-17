package androidx.compose.ui.text

fun TextRange(index: Int): TextRange {
    throw Exception("Stub!")
}

fun TextRange(start: Int, end: Int): TextRange {
    throw Exception("Stub!")
}

@JvmInline
value class TextRange internal constructor(private val packedValue: Long)
