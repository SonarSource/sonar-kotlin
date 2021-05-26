package checks

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherBlockChainingCheckSample {

    fun nonCompliant(secretKey: String) {
        val bytesIV = "7cVgr5cbdCZVw5WY".toByteArray(charset("UTF-8")) // Predictable / hardcoded IV

        val iv = IvParameterSpec(bytesIV)
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant {{Use a dynamically-generated, random IV.}}
        
        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
    }

    fun letApplyRunWithAlso(secretKey: String) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val bytesIV = "7cVgr5cbdCZVw5WY".toByteArray(charset("UTF-8")) // Predictable / hardcoded IV
        val iv = IvParameterSpec(bytesIV)

        Cipher.getInstance("AES/CBC/PKCS5PADDING").let {
            it.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant
        }

        Cipher.getInstance("AES/CBC/PKCS5PADDING").apply {
            init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant
        }

        Cipher.getInstance("AES/CBC/PKCS5PADDING").run {
            init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant
        }

        Cipher.getInstance("AES/CBC/PKCS5PADDING").also {
            it.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant
        }

        with(Cipher.getInstance("AES/CBC/PKCS5PADDING")) {
            init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant
        }

    }

    fun letApplyRunWithAlso2(secretKey: String) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val bytesIV = "7cVgr5cbdCZVw5WY".toByteArray(charset("UTF-8")) // Predictable / hardcoded IV

        IvParameterSpec(bytesIV).let {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
                .init(Cipher.ENCRYPT_MODE, skeySpec, it) // Noncompliant

        }

        IvParameterSpec(bytesIV).apply {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
                .init(Cipher.ENCRYPT_MODE, skeySpec, this) // FN, can't resolve 'this'

        }

        IvParameterSpec(bytesIV).run {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
                .init(Cipher.ENCRYPT_MODE, skeySpec, this) // FN, can't resolve 'this'

        }

        IvParameterSpec(bytesIV).also {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
                .init(Cipher.ENCRYPT_MODE, skeySpec, it) // Noncompliant

        }

        with(IvParameterSpec(bytesIV)) {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
                .init(Cipher.ENCRYPT_MODE, skeySpec, this) // FN, can't resolve 'this'
        }
    }

    fun compliantIV(secretKey: String, bytesIV: ByteArray) {

        val iv = IvParameterSpec(bytesIV)
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
    }

    fun compliantAlgorithm(secretKey: String) {
        val bytesIV = "7cVgr5cbdCZVw5WY".toByteArray(charset("UTF-8")) // Predictable / hardcoded IV

        val iv = IvParameterSpec(bytesIV)
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("AES/CCC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
    }

    fun compliant(secretKey: String) {
        val random: SecureRandom = SecureRandom()

        val bytesIV: ByteArray = ByteArray(16)
        random.nextBytes(bytesIV); // Unpredictable / random IV

        val iv = IvParameterSpec(bytesIV)
        
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) //Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
    }

    fun compliant2(secretKey: String, algo: String) {
        val bytesIV = "7cVgr5cbdCZVw5WY".toByteArray(charset("UTF-8")) // Predictable / hardcoded IV

        val iv = IvParameterSpec(bytesIV)
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance(algo)
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

    fun compliant3(secretKey: String, ivParameterSpec: IvParameterSpec) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("CBC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParameterSpec) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

    fun compliant4(secretKey: String, ivParameterSpec: IvParameterSpec) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val iv = ivParameterSpec

        val cipher: Cipher = Cipher.getInstance("CBC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

    fun compliant5(secretKey: String, ivParameterSpec: IvParameterSpec, bytes: ByteArray) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val iv = IvParameterSpec(bytes)

        val cipher: Cipher = Cipher.getInstance("CBC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

    fun compliant6(secretKey: String, ivParameterSpec: IvParameterSpec) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val bytes = ByteArray(100)
        val iv = IvParameterSpec(bytes)

        val cipher: Cipher = Cipher.getInstance("CBC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

    fun compliant7(secretKey: String, ivParameterSpec: IvParameterSpec) {
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val bytes1 = ByteArray(100)
        val bytes = bytes1
        val iv = IvParameterSpec(bytes)

        val cipher: Cipher = Cipher.getInstance("CBC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Compliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

    fun non_compliant2(secretKey: String, pass: String) {
        val bytesIV = pass.toByteArray(charset("UTF-8")) // Predictable / hardcoded IV

        val iv = IvParameterSpec(bytesIV)
        val skeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        val cipher: Cipher = Cipher.getInstance("CBC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv) // Noncompliant

        val encryptedBytes: ByteArray = cipher.doFinal("foo".toByteArray())
   }

}
