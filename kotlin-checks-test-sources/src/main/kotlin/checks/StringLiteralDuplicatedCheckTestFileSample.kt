package checks

@Suppress("UNUSED_EXPRESSION")
class StringLiteralDuplicatedCheckTestFileSample {
    fun duplicatesAreIgnored() {
        "duplicate in a test file!"
        "duplicate in a test file!"
        "duplicate in a test file!"
    }
}
