package checks

annotation class Composable
annotation class Preview

// Public @Composable + Unit + PascalCase → exempt (Compose API guidelines for UI components)
@Composable
fun MyScreen() {} // Compliant

@Composable
fun MyDialog(): Unit {} // Compliant

// Public @Composable + Unit + camelCase → matches the camelCase format already
@Composable
fun myHelperComposable() {} // Compliant

// Public @Composable + Unit + name that is neither PascalCase nor camelCase → Noncompliant
@Composable
fun My_Screen() {} // Noncompliant {{Rename function "My_Screen" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//  ^^^^^^^^^

// Public @Composable + non-Unit return type → NOT exempt even with PascalCase
@Composable
fun MyFactory(): String = "value" // Noncompliant {{Rename function "MyFactory" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//  ^^^^^^^^^

// Private @Composable + PascalCase → exempt (private allows both PascalCase and camelCase)
@Composable
private fun MyPrivateComponent() {} // Compliant

// Private @Composable + camelCase → matches the format already
@Composable
private fun myPrivateHelper() {} // Compliant

// Private @Composable + name that is neither PascalCase nor camelCase → Noncompliant
@Composable
private fun My_PrivateComponent() {} // Noncompliant {{Rename function "My_PrivateComponent" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//          ^^^^^^^^^^^^^^^^^^^

// @Preview + @Composable + backtick name → exempt (human-readable preview label in IDE)
@Preview
@Composable
fun `User Profile - Dark Mode Preview`() {} // Compliant - backtick with @Preview

// @Composable + backtick without @Preview or @Test → NOT exempt
@Composable
fun `my composable helper`() {} // Noncompliant {{Rename function "my composable helper" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//  ^^^^^^^^^^^^^^^^^^^^^^

// Non-@Composable PascalCase → Noncompliant (the @Composable exemption does not apply)
fun MyRegularFunction() {} // Noncompliant {{Rename function "MyRegularFunction" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//  ^^^^^^^^^^^^^^^^^

open class ComposableClassSample {

    // Protected @Composable + PascalCase → exempt (same as public)
    @Composable
    protected fun MyProtectedComponent() {} // Compliant

    // Internal @Composable + PascalCase → NOT exempt (only public and protected)
    @Composable
    internal fun MyInternalComponent() {} // Noncompliant {{Rename function "MyInternalComponent" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//               ^^^^^^^^^^^^^^^^^^^

}