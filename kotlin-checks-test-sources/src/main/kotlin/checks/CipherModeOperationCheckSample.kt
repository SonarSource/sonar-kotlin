package checks

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherModeOperationCheckSample {

    fun nonCompliant() {
        val key: ByteArray = "0123456789123456".toByteArray()
        val nonce: ByteArray = "7cVgr5cbdCZV".toByteArray()
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^> {{Initialization vector is configured here.}}

        val gcmSpec = GCMParameterSpec(128, nonce)
//                                          ^^^^^> {{Initialization vector is configured here.}}
        val skeySpec = SecretKeySpec(key, "AES")

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec) // Noncompliant {{The initialization vector is a static value.}}
//                                                 ^^^^^^^
    }

    fun nonCompliant2() {
        val key: ByteArray = "0123456789123456".toByteArray()

        val gcmSpec = GCMParameterSpec(128, "7cVgr5cbdCZV".toByteArray())
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^> {{Initialization vector is configured here.}}
        val skeySpec = SecretKeySpec(key, "AES")

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec) // Noncompliant {{The initialization vector is a static value.}}
//                                                 ^^^^^^^
    }

    fun compliant(key: ByteArray) {
        val skeySpec = SecretKeySpec(key, "AES")

        val nonce = ByteArray(12)
        val random = SecureRandom()
        random.nextBytes(nonce) // Random 96 bit IV
        val gcmSpec = GCMParameterSpec(128, nonce)

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec) // Compliant
    }

    fun compliant2(key: ByteArray, s: String) {
        val skeySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, s.toByteArray())

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec) // Compliant
    }

}
