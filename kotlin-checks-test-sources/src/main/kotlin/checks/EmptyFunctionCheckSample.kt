package checks

class EmptyFunctionCheckSample {

    // Noncompliant@+1 {{Add a nested comment explaining why this function is empty or complete the implementation.}}
    fun empty() {}

    fun withComment() {
        // comment
    }

    fun withTrailingComment() {
    } // trailing comment

    fun withMultilineComment() {
        /*
        comment
        */
    }

    fun withKDoc() { /** comment */ }

    fun withStatement() {
        return
    }

    fun withExpression() = 42

    // TODO false-negative
    fun String.extension() {
    }

}
