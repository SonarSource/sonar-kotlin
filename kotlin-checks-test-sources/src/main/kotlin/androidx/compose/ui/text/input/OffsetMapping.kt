package androidx.compose.ui.text.input

interface OffsetMapping {
    fun originalToTransformed(offset: Int): Int
    fun transformedToOriginal(offset: Int): Int

    companion object {
        val Identity = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                throw Exception("Stub!")
            }
            override fun transformedToOriginal(offset: Int): Int {
                throw Exception("Stub!")
            }
        }
    }
}
