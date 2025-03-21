package checks

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.TextField
import androidx.compose.ui.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class AndroidKeyboardCacheOnPasswordInputCheckSample {
    class CustomVisualTransformation : VisualTransformation { // Compliant
        override fun filter(text: AnnotatedString): TransformedText = throw NotImplementedError()
    }

    val valPropertyKeyboardOptionsDefault = KeyboardOptions.Default
    var varPropertyKeyboardOptionsDefault = KeyboardOptions.Default
    val valPropertyKeyboardOptionsConstructorWithCacheEnabledKeyboardType = KeyboardOptions(keyboardType = KeyboardType.Text)
    val valPropertyKeyboardOptionsConstructorWithCacheDisabledKeyboardType = KeyboardOptions(keyboardType = KeyboardType.Password)

    val textFieldInValProperty = TextField( // Noncompliant {{Set "keyboardOptions" to disable the keyboard cache.}}
        //                       ^^^^^^^^^
        value = "",
        onValueChange = { },
        visualTransformation = PasswordVisualTransformation(),
        //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{}}
    )

    var textFieldInVarProperty = androidx.compose.material3.TextField( // Noncompliant
        //                                                  ^^^^^^^^^
        value = "",
        onValueChange = { },
        visualTransformation = PasswordVisualTransformation(),
    )

    fun nonCompliant() {

        fun material3TextField_NoKeyboardOptions(
            passwordVisible: Boolean,
            concealData: Boolean,
            textFieldInFunParam: Unit = TextField( // Noncompliant
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
            ),
        ) {
                        TextField( // Noncompliant
                //      ^^^^^^^^^
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
            )
            var assignedToLocal = TextField( // Noncompliant
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
            )
            var passwordVisualTransformationWithParam = TextField( // Noncompliant
                //                                      ^^^^^^^^^
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(mask = 'o'),
                //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
            )
            var visualTransformationParenthesized = TextField( // Noncompliant
                //                                  ^^^^^^^^^
                value = "",
                onValueChange = { },
                visualTransformation = (PasswordVisualTransformation()),
                //                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
            )
            var visualTransformationMultiLine = TextField( // Noncompliant
                //                              ^^^^^^^^^
                value = "",
                onValueChange = { },
                visualTransformation = (
                    PasswordVisualTransformation(
                //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
                    )
                ),
            )
            val contextualVisualTransformation = TextField( // Noncompliant
                value = "",
                onValueChange = { },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            )
            val multipleVisualTransformationInstantiated = TextField( // Noncompliant
                value = "",
                onValueChange = { },
                visualTransformation = when {
                    passwordVisible && !concealData -> PasswordVisualTransformation()
                    concealData -> PasswordVisualTransformation()
                    else -> CustomVisualTransformation()
                }
            )
            val textField = ::TextField
            textField( // FN: requires flow analysis
                "",
                { },
                PasswordVisualTransformation(),
                KeyboardOptions.Default,
            )
            val valPasswordVisualTransformation = PasswordVisualTransformation()
            TextField( // FN: requires matching argument type
                value = "",
                onValueChange = { },
                visualTransformation = valPasswordVisualTransformation,
            )
            var varPasswordVisualTransformation = PasswordVisualTransformation()
            TextField( // FN: variable can potentially mutate
                value = "",
                onValueChange = { },
                visualTransformation = varPasswordVisualTransformation,
            )
            val visualTransformationProvider = { PasswordVisualTransformation() }
            TextField( // FN: requires flow analysis
                value = "",
                onValueChange = { },
                visualTransformation = visualTransformationProvider(),
            )
        }

        fun material3TextField_DefaultKeyboardOptions(
            passwordVisible: Boolean,
            concealData: Boolean,
            textFieldInFunParam: Unit = TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^> {{}}
                keyboardOptions = KeyboardOptions.Default, // Noncompliant {{Set the "keyboardType" to "KeyboardType.Password" to disable the keyboard cache.}}
                //                ^^^^^^^^^^^^^^^^^^^^^^^
            ),
        ) {
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default, // Noncompliant
            )
            val invertedParamOrder = TextField(
                value = "",
                onValueChange = { },
                keyboardOptions = KeyboardOptions.Default, // Noncompliant
                //                ^^^^^^^^^^^^^^^^^^^^^^^
                visualTransformation = PasswordVisualTransformation(),
                //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
            )
            val keyboardOptionsWithFqn = TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^> 1 {{}}
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default, // Noncompliant
                //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 1
            )
            val defaultKeyboardOptions = KeyboardOptions.Default
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = defaultKeyboardOptions, // Noncompliant
            )
        }

        fun material3TextField_KeyboardOptionsConstructor_OmittedKeyboardType() {
            val noArgumentToKeyboardOptions = TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(), // Noncompliant
            )
            val differentArgumentToKeyboardOptions = TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = true), // Noncompliant
            )
            val valCacheEnabledKeyboardOptions = KeyboardOptions(autoCorrectEnabled = true)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = valCacheEnabledKeyboardOptions, // Noncompliant
            )
            val varCacheEnabledKeyboardOptions = KeyboardOptions(autoCorrectEnabled = true)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = varCacheEnabledKeyboardOptions, // Noncompliant
            )
        }

        fun material3TextField_KeyboardOptionsConstructor_KeyboardTypeWithCacheEnabled() {
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^> 1
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), // Noncompliant
                //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            )
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Unspecified), // Noncompliant
            )
            val valCacheEnabledKeyboardOptions = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = valCacheEnabledKeyboardOptions, // Noncompliant
            )
            val varCacheEnabledKeyboardOptions = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = varCacheEnabledKeyboardOptions, // Noncompliant
            )
            var varCacheEnabledKeyboardOptionsModified = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Password)
            varCacheEnabledKeyboardOptionsModified = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = varCacheEnabledKeyboardOptionsModified, // FN: var can be reassigned, so it requires flow analysis
            )
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = valPropertyKeyboardOptionsDefault, // Noncompliant
            )
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = varPropertyKeyboardOptionsDefault, // FN: var can be reassigned, so it requires flow analysis
            )
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = valPropertyKeyboardOptionsConstructorWithCacheEnabledKeyboardType, // Noncompliant
            )

            fun withCacheEnabledKeyboardOptionAsFunParam(
                keyboardOptionsInner: KeyboardOptions = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            ) {
                TextField(
                    value = "",
                    onValueChange = { },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboardOptionsInner, // Noncompliant
                )
            }
            val keyboardOptionsUsedAsFunParamDefaultValue = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            fun withCacheEnabledKeyboardOptionCopyAsFunParam(
                keyboardOptionsInner: KeyboardOptions = keyboardOptionsUsedAsFunParamDefaultValue.copy(keyboardType = KeyboardType.Number)
            ) {
                TextField(
                    value = "",
                    onValueChange = { },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboardOptionsInner, // Noncompliant
                )
            }
        }

        fun material3TextField_KeyboardOptionsCopy_OmittedKeyboardType() {
            val keyboardOptions = KeyboardOptions.Default
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions.copy(),  // FN: requires flow as keyboard options are copied from a variable
            )
        }

        fun material3TextField_KeyboardOptionsCopy_KeyboardTypeWithCacheEnabled() {
            val keyboardOptions = KeyboardOptions.Default
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                //                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^>
                keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Ascii),  // Noncompliant
                //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            )
            val keyboardOptionsCopy = keyboardOptions.copy(keyboardType = KeyboardType.Text)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptionsCopy,  // Noncompliant
            )
            fun withCacheEnabledKeyboardOptionAsFunParam(
                keyboardOptionsInner: KeyboardOptions = keyboardOptions.copy(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            ) {
                TextField(
                    value = "",
                    onValueChange = { },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboardOptionsInner, // Noncompliant
                )
            }
        }

        fun materialTextField() {
            androidx.compose.material.TextField( // Noncompliant
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        fun materialOutlinedTextField() {
            androidx.compose.material.OutlinedTextField( // Noncompliant
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        fun material3OutlinedTextField() {
            OutlinedTextField( // Noncompliant
    //      ^^^^^^^^^^^^^^^^^
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
            )
        }
    }

    fun compliant() {

        fun material3TextField() {
            val implicitNoneTransformation = TextField( // Compliant: no visual transformation => cannot assume it's a password field
                value = "",
                onValueChange = { },
            )
            val explicitNoneTransformation = TextField( // Compliant: no visual transformation => cannot assume it's a password field
                value = "",
                onValueChange = { },
                visualTransformation = VisualTransformation.None,
            )
            val customTransformation = TextField( // Compliant: custom visual transformation => cannot assume it's a password field
                value = "",
                onValueChange = { },
                visualTransformation = CustomVisualTransformation(),
            )
            val secureTextField = SecureTextField() // Compliant: secure text field => safe
        }

        fun material3TextField_keyboardOptions(keyboardOptions: KeyboardOptions) {
            val customTransformation_cacheEnabledKeyboardType = TextField( // Compliant: custom visual transformation => cannot assume it's a password field
                value = "",
                onValueChange = { },
                visualTransformation = CustomVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            )
            val customTransformation_KeyboardOptionsDefault = TextField( // Compliant: custom visual transformation => cannot assume it's a password field
                value = "",
                onValueChange = { },
                visualTransformation = CustomVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default,
            )
            val passwordTransformation_KeyboardOptionsConstructor_KeyboardTypePassword = TextField( // Compliant: KeyboardType.Password disables the cache
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            val passwordTransformation_KeyboardOptionsConstructor_KeyboardTypeNumberPassword = TextField( // Compliant: KeyboardType.NumberPassword disables the cache
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            val passwordTransformation_KeyboardOptionsCopy_OmittedKeyboardType = TextField( // Compliant: keyboard type not specified in copy => cannot assume cache enabled
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions.copy(autoCorrectEnabled = true),
            )
            val passwordTransformation_KeyboardOptionsCopy_KeyboardTypePassword = TextField( // Compliant: KeyboardType.Password disables the cache
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Password),
            )
            val passwordTransformation_KeyboardOptionsCopy_KeyboardTypeNumberPassword = TextField( // Compliant: KeyboardType.NumberPassword disables the cache
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.NumberPassword),
            )
            val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions.copy(),  // Compliant: copy preserves the keyboard type when not specified
            )
            fun withCacheDisabledKeyboardOptionAsFunParam(
                keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            ) {
                TextField(
                    value = "",
                    onValueChange = { },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboardOptions, // Compliant: keyboard options parameter default is cache disabled
                )
            }
            val keyboardOptionsUsedAsFunParamDefaultValue = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            fun withCacheEnabledKeyboardOptionCopyAsFunParam(
                keyboardOptionsInner: KeyboardOptions = keyboardOptionsUsedAsFunParamDefaultValue.copy(keyboardType = KeyboardType.Password)
            ) {
                TextField(
                    value = "",
                    onValueChange = { },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboardOptionsInner, // Compliant: keyboard type is explicitly set to Password in copy
                )
            }
            var varCacheEnabledKeyboardOptionsModified = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Ascii)
            varCacheEnabledKeyboardOptionsModified = KeyboardOptions(autoCorrectEnabled = true, keyboardType = KeyboardType.Password)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = varCacheEnabledKeyboardOptionsModified, // Compliant: reassigned to a cache disabled keyboard type
            )
            val valPropertyKeyboardOptionsConstructorWithCacheDisabledKeyboardType = KeyboardOptions(keyboardType = KeyboardType.Password)
            TextField(
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = valPropertyKeyboardOptionsConstructorWithCacheDisabledKeyboardType, // Compliant: cache disabled keyboard type
            )
        }

        fun material3OutlinedTextField() {
            OutlinedTextField( // Compliant: no visual transformation => cannot assume it's a password field
                state = androidx.compose.foundation.text.input.TextFieldState(),
                modifier = androidx.compose.ui.Modifier,
            )
        }

        fun material3TextField_UnknownKeyboardOptions(keyboardOptions: KeyboardOptions) {
            TextField( // Compliant: unknown externally defined, without default
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions,
            )
        }

        fun materialTextField() {
            androidx.compose.material.TextField( // Compliant: no visual transformation => cannot assume it's a password field
                value = "",
                onValueChange = { },
            )
        }

        fun materialOutlinedTextField_UnknownKeyboardOptions(keyboardOptions: KeyboardOptions) {
            androidx.compose.material.OutlinedTextField( // Compliant: unknown externally defined, without default
                value = "",
                onValueChange = { },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = keyboardOptions,
            )
        }
    }
}
