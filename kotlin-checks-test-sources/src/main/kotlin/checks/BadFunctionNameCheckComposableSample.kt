package checks

annotation class Composable
annotation class Preview

class RememberedState

// Public @Composable + Unit + PascalCase → exempt (Compose API guidelines for UI components)
@Composable
fun MyScreen() {} // Compliant

@Composable
fun MyDialog(): Unit {} // Compliant

@Composable
fun MyScreen2(): kotlin.Unit {} // Compliant

// Public @Composable + Unit + camelCase → Compliant when the configured format already accepts it
@Composable
fun myHelperComposable() {} // Compliant - matches the configured format (^[a-z][a-zA-Z0-9]*$), no additional Compose check is applied

// Public @Composable + Unit + name that is neither PascalCase nor camelCase → Noncompliant
@Composable
fun My_Screen() {} // Noncompliant {{Rename function "My_Screen" to use PascalCase}}
//  ^^^^^^^^^

// Public @Composable + non-Unit return type → must follow camelCase (composable factory function)
@Composable
fun MyFactory(): String = "value" // Noncompliant {{Rename function "MyFactory" to use camelCase}}
//  ^^^^^^^^^

// Expression-body @Composable with inferred non-Unit return type → camelCase required
@Composable
fun rememberSomething() = RememberedState() // Compliant - inferred return type is RememberedState, not Unit

@Composable
fun RememberSomething() = RememberedState() // Noncompliant {{Rename function "RememberSomething" to use camelCase}}
//  ^^^^^^^^^^^^^^^^^

// Private @Composable + PascalCase → exempt (private allows both PascalCase and camelCase)
@Composable
private fun MyPrivateComponent() {} // Compliant

// Private @Composable + camelCase → matches the format already
@Composable
private fun myPrivateHelper() {} // Compliant

// Private @Composable + name that is neither PascalCase nor camelCase → Noncompliant
@Composable
private fun My_PrivateComponent() {} // Noncompliant {{Rename function "My_PrivateComponent" to use PascalCase or camelCase}}
//          ^^^^^^^^^^^^^^^^^^^

// @Preview + @Composable + backtick name → exempt (human-readable preview label in the Android Studio IDE)
@Preview
@Composable
fun `User Profile - Dark Mode Preview`() {} // Compliant - backtick with @Preview and @Composable

// @Preview without @Composable + backtick → NOT exempt (@Preview alone is not sufficient)
@Preview
fun `My Preview`() {} // Noncompliant {{Rename function "My Preview" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//  ^^^^^^^^^^^^

// @Composable + backtick without @Preview or @Test → NOT exempt
@Composable
fun `my composable helper`() {} // Noncompliant {{Rename function "my composable helper" to use PascalCase}}
//  ^^^^^^^^^^^^^^^^^^^^^^

// Non-@Composable PascalCase → Noncompliant (the @Composable exemption does not apply)
fun MyRegularFunction() {} // Noncompliant {{Rename function "MyRegularFunction" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//  ^^^^^^^^^^^^^^^^^

open class ComposableClassSample {

    // Protected @Composable + PascalCase → exempt (same as public)
    @Composable
    protected fun MyProtectedComponent() {} // Compliant

    // Internal @Composable + PascalCase → exempt (private/internal also allow PascalCase)
    @Composable
    internal fun MyInternalComponent() {} // Compliant

}

// @Composable override — name is fixed by the supertype, no convention can be enforced
fun interface ComposableFactory {
    fun CreateFoo(): RememberedState // Noncompliant {{Rename function "CreateFoo" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//      ^^^^^^^^^
}

class ConcreteFactory : ComposableFactory {
    @Composable
    override fun CreateFoo(): RememberedState = RememberedState() // Compliant - override, name cannot be changed
}

// Local @Composable functions — not part of the public API, skip Compose naming conventions
@Composable
fun ScreenWithLocalComposable() {
    @Composable
    fun LocalSection(): RememberedState = RememberedState() // Compliant - local function
}
