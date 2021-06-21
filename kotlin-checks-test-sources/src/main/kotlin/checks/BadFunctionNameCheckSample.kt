package checks

class BadFunctionNameCheckSample {

    fun foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^

    fun `else`() {}  // Compliant

    fun String.`foo bar`() {} // Noncompliant {{Rename function "foo bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//             ^^^^^^^^^

    val anonymousFunction = fun() {}

}
