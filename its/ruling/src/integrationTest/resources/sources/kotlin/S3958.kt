package sources.kotlin

fun s3958() {
    sequenceOf("a", "b")
        .map { it.uppercase() } // Noncompliant
}
