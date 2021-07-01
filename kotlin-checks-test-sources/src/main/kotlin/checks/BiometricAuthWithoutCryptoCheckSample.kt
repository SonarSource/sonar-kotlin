package checks

import android.hardware.biometrics.BiometricPrompt as BiometricPromptAndroid
import androidx.biometric.BiometricPrompt as BiometricPromptAndroidX

val promptInfo = ""
val cipher = ""

fun android() {
    val biometricPrompt = BiometricPromptAndroid()

    biometricPrompt.authenticate(promptInfo) // Noncompliant {{Make sure performing a biometric authentication without a CryptoObject is safe here}}
    // Noncompliant@+1
    biometricPrompt.authenticate(promptInfo, null)
//                  ^^^^^^^^^^^^>            ^^^^

    biometricPrompt.authenticate(promptInfo, BiometricPromptAndroid.CryptoObject(cipher)) // Compliant
}

fun androidx() {
    val biometricPrompt = BiometricPromptAndroidX()

    biometricPrompt.authenticate(promptInfo) // Noncompliant
    // Noncompliant@+1
    biometricPrompt.authenticate(promptInfo, null)
//                  ^^^^^^^^^^^^>            ^^^^

    biometricPrompt.authenticate(promptInfo, BiometricPromptAndroidX.CryptoObject(cipher))  // Compliant
}
