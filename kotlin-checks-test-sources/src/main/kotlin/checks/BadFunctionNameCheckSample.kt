package checks

class BadFunctionNameCheckSample {

    fun foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^

    fun `else`() {}  // Compliant

    // TODO false-negative
    fun String.`foo bar`() {}

    val anonymousFunction = fun() {}

}
