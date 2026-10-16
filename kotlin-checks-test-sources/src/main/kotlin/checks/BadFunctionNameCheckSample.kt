package checks

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate

annotation class Test

class BadFunctionNameCheckSample {

    fun foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^

    fun `else`() {}  // Compliant

    fun String.`foo bar`() {} // Noncompliant {{Rename function "foo bar" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//             ^^^^^^^^^

    val anonymousFunction = fun() {}

    fun `non test backtick name with spaces`() {} // Noncompliant {{Rename function "non test backtick name with spaces" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // External (FFI/JNI) functions — name is owned by the native ABI, renaming would break interop
    external fun ffi_native_long_function_name() // Compliant - external modifier
    external fun generate_seed() // Compliant - external modifier

}

// region test functions — the name describes the scenario, so any naming style is appropriate
class BadFunctionNameCheckTestSample {

    @Test
    fun `should do A when B is called`() {} // Compliant - test function

    @kotlin.test.Test
    fun `should do A when B is called (fq annotation)`() {} // Compliant - test function

    @Test
    fun regularTestName() {} // Compliant - regular name matches the regex

    @Test
    fun test_name_with_underscores() {} // Compliant - test function

    @Test
    fun oneNoteShortcut_isTypedByItsTarget() {} // Compliant - test function using the subject_scenario_expectation style

    @TestFactory
    fun `creates one dynamic test per input row`() {} // Compliant - test function

    @TestTemplate
    fun runs_against_every_context() {} // Compliant - test function

    @RepeatedTest(3)
    fun `retries the flaky call`() {} // Compliant - test function

    @BeforeEach
    fun set_up() {} // Compliant - test lifecycle hook

    @kotlin.test.AfterTest
    fun `tears down the fixture`() {} // Compliant - test lifecycle hook

    // A helper that carries no test annotation is still subject to the naming convention
    private fun build_fixture() {} // Noncompliant {{Rename function "build_fixture" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//              ^^^^^^^^^^^^^

}
// endregion

// region override — name is fixed by the supertype contract, cannot be renamed
open class BaseWithNonCompliantName {
    open fun bad_name() {} // Noncompliant {{Rename function "bad_name" to match the regular expression ^[a-zA-Z][a-zA-Z0-9]*$}}
//           ^^^^^^^^
}

class DerivedWithOverride : BaseWithNonCompliantName() {
    override fun bad_name() {} // Compliant - override, name is fixed by supertype
}

// endregion