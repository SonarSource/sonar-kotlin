package androidx.compose.ui.text.input

import androidx.compose.ui.AnnotatedString

class TransformedText(val text: AnnotatedString, val offsetMapping: OffsetMapping)

fun interface VisualTransformation {
    fun filter(text: AnnotatedString): TransformedText

    companion object {
        val None: VisualTransformation = VisualTransformation {
            throw Exception("Stub!")
        }
    }
}

class PasswordVisualTransformation(val mask: Char = '\u2022') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        throw Exception("Stub!")
    }
}
