package checks

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class AuthorisingNonAuthenticatedUsersCheckSample {
    
    fun nonCompliant() { 
        
        KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT) // Noncompliant 
//                          ^^^^^^^
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT) // Noncompliant
//                          ^^^^^^^
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
//           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
            .build()
    }

    fun compliant() {
        KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .build()
    }

    fun compliant2(value: Boolean) {
        KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(value)
            .build()
    }

    fun extractedVariables() {
        val builder = KeyGenParameterSpec.Builder("test_secret_key",     // Noncompliant
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)

        builder
            .setUserAuthenticationRequired(false)
            .build()
    }

    fun extractedVariablesNotFinal() {
        var builder = KeyGenParameterSpec.Builder("test_secret_key",     // FN, `var` can be reassigned
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)

        builder
            .setUserAuthenticationRequired(false)
            .build()
    }

    // False-negative, because builder wasn't created here
    fun builderParam(builder: KeyGenParameterSpec.Builder) {
        builder
            .setUserAuthenticationRequired(false)
            .build()
    }

    fun functionalStyle() {
        KeyGenParameterSpec.Builder("test_secret_key",     // Noncompliant
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).let {
            it.setUserAuthenticationRequired(false)
                .build()
        }
    }
}
