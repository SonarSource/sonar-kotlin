package checks

class BadFunctionNameCheckSampleTestFile {

    fun `should do A when B is called`() {} // Compliant - backtick name in test file

    fun `test with spaces`() {} // Compliant - backtick name in test file

    fun foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^

    fun `else`() {} // Compliant - backtick name matches regex

}
