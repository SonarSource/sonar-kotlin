package checks

annotation class Test

class BadFunctionNameCheckSample {

    fun foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^

    fun `else`() {}  // Compliant

    fun String.`foo bar`() {} // Noncompliant {{Rename function "foo bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//             ^^^^^^^^^

    val anonymousFunction = fun() {}

    @Test
    fun `should do A when B is called`() {} // Compliant - backtick name with @Test annotation

    @kotlin.test.Test
    fun `should do A when B is called (fq annotation)`() {}

    @Test
    fun `test with spaces in name`() {} // Compliant - backtick name with @Test annotation

    @Test
    fun regularTestName() {} // Compliant - regular name matches the regex

    @Test
    fun test_name_with_underscores() {} // Noncompliant {{Rename function "test_name_with_underscores" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^

    fun `non test backtick name with spaces`() {} // Noncompliant {{Rename function "non test backtick name with spaces" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
