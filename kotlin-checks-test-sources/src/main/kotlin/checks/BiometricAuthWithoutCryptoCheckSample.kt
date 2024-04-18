package checks

import android.os.CancellationSignal
import android.hardware.biometrics.BiometricPrompt as BiometricPromptAndroid

fun android() {
    val biometricPrompt = BiometricPromptAndroid()

    biometricPrompt.authenticate(CancellationSignal(), {  }, BiometricPromptAndroid.AuthenticationCallback()) // Noncompliant {{Make sure performing a biometric authentication without a "CryptoObject" is safe here.}}
    
    // Noncompliant@+1
    biometricPrompt.authenticate     (null, CancellationSignal(), {  }, BiometricPromptAndroid.AuthenticationCallback())
//                  ^^^^^^^^^^^^>     ^^^^

    biometricPrompt.authenticate(BiometricPromptAndroid.CryptoObject(), CancellationSignal(), {  }, BiometricPromptAndroid.AuthenticationCallback()) // Compliant
}
