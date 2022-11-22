package checks

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherModeOperationCheckSample {

    fun nonCompliant() {
        val key: ByteArray = "0123456789123456".toByteArray()
        val nonce: ByteArray = "7cVgr5cbdCZV".toByteArray()
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^> {{The initialization vector is a static value.}}

        val gcmSpec = GCMParameterSpec(128, nonce)
//                                          ^^^^^> {{Initialization vector is configured here.}}
        val skeySpec = SecretKeySpec(key, "AES")

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec) // Noncompliant {{Use a dynamically-generated initialization vector (IV) to avoid IV-key pair reuse.}}
//             ^^^^                                ^^^^^^^< {{Initialization vector is configured here.}}
    }

    fun nonCompliant2() {
        val key: ByteArray = "0123456789123456".toByteArray()

        val gcmSpec = GCMParameterSpec(128, "7cVgr5cbdCZV".toByteArray())
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^> {{The initialization vector is a static value.}}
        val skeySpec = SecretKeySpec(key, "AES")

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec) // Noncompliant {{Use a dynamically-generated initialization vector (IV) to avoid IV-key pair reuse.}}
//             ^^^^                                ^^^^^^^< {{Initialization vector is configured here.}}
    }

    fun letApplyRunWithAlso(secretKey: String) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val bytesGCM = "7cVgr5cbdCZV".toByteArray(charset("UTF-8"))
        val gcm = GCMParameterSpec(128, bytesGCM)

        Cipher.getInstance("AES/CBC/NoPadding").let {
            it.init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Noncompliant
        }

        Cipher.getInstance("AES/CBC/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Noncompliant
        }

        Cipher.getInstance("AES/CBC/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Noncompliant
        }

        Cipher.getInstance("AES/CBC/NoPadding").also {
            it.init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Noncompliant
        }

        with(Cipher.getInstance("AES/CBC/NoPadding")) {
            init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Noncompliant
        }

    }

    fun compliantEncryptMode() {
        val key: ByteArray = "0123456789123456".toByteArray()
        val nonce: ByteArray = "7cVgr5cbdCZV".toByteArray()

        val gcmSpec = GCMParameterSpec(128, nonce)
        val skeySpec = SecretKeySpec(key, "AES")

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, gcmSpec) // Compliant
    }

    fun compliant1(key: ByteArray) {
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

    fun compliant3(secretKey: String, gcmParameterSpec: GCMParameterSpec) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmParameterSpec) // Compliant
    }

    fun compliant4(secretKey: String, gcmParameterSpec: GCMParameterSpec) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val gcm = gcmParameterSpec

        val cipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Compliant
    }

    fun compliant5(secretKey: String, bytes: ByteArray) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val gcm = GCMParameterSpec(128, bytes)

        val cipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcm) // Compliant
    }

    fun compliant6(secretKey: String) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val bytes1 = ByteArray(100)
        val bytes = bytes1
        val gcmParameterSpec = GCMParameterSpec(128, bytes)

        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmParameterSpec) // Compliant
    }

}
