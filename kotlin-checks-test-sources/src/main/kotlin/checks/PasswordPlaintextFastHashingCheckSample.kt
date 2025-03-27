package checks

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

typealias SecretKeyFactoryAlias = SecretKeyFactory
typealias PBEKeySpecAlias = PBEKeySpec

class PasswordPlaintextFastHashingCheckSample {
    // region Noncompliant cases

    class NoncompliantConstantIterations {
        companion object {
            private const val PBKDF2_ITERATIONS = 120000
            //                                    ^^^^^^>
        }

        fun noncompliantConstantIterations(password: String, salt: ByteArray) {
            val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256) // Noncompliant {{Use at least 210000 PBKDF2 iterations.}}
            //                                                     ^^^^^^^^^^^^^^^^^
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
            //                                                  ^^^^^^^^^^^^^^^^^^^^^^<
            secretKeyFactory.generateSecret(keySpec)
        }
    }

    class NoncompliantConstantAlgorithm {
        companion object {
            private const val SHA512_ALGORITHM = "PBKDF2withHmacSHA512"
            //                                   ^^^^^^^^^^^^^^^^^^^^^^>
        }

        fun noncompliantConstantAlgorithm(password: String, salt: ByteArray) {
            val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Noncompliant {{Use at least 210000 PBKDF2 iterations.}}
            //                                                     ^^^^^^
            val secretKeyFactory = SecretKeyFactory.getInstance(SHA512_ALGORITHM)
            secretKeyFactory.generateSecret(keySpec)
        }
    }

    fun noncompliantNoKeyLength(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000) // Noncompliant
        //                                                     ^^^^^^
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        //                                                  ^^^^^^^^^^^^^^^^^^^^^^<
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantIntLiteralIterationWithSha512(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Noncompliant {{Use at least 210000 PBKDF2 iterations.}}
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantIntLiteralIterationWithSha256(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 300000, 256) // Noncompliant {{Use at least 600000 PBKDF2 iterations.}}
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantIntLiteralIterationWithSha1(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 1_200_000, 256) // Noncompliant {{Use at least 1300000 PBKDF2 iterations.}}
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantLocalVariableIteration(password: String, salt: ByteArray) {
        val iterations = 120000
        //               ^^^^^^>
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256) // Noncompliant
        //                                                     ^^^^^^^^^^
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        //                                                  ^^^^^^^^^^^^^^^^^^^^^^<
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantLocalVariableIterationAndAlgorithm(password: String, salt: ByteArray) {
        val algorithm = "PBKDF2withHmacSHA512"
        //              ^^^^^^^^^^^^^^^^^^^^^^>
        val iterations = 120000
        //               ^^^^^^>
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256) // Noncompliant
        //                                                     ^^^^^^^^^^
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm)
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantComplexFlow(password: String, salt: ByteArray) {
        val iterations = 500_000
        //               ^^^^^^^>
        if (salt.size > 10) {
            print("Some flow between relevant code")
        }
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256) // Noncompliant
        //                                                     ^^^^^^^^^^
        while (salt.size < 10) {
            if (salt.hashCode() == 42) {
                print("Some other flow between relevant code")

                val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256")
                //                                                  ^^^^^^^^^^^^^^^^^^^^^^<
                val aLambda = {
                    secretKeyFactory.generateSecret(keySpec)
                }
            }
        }
    }

    fun noncompliantStringConcatenation(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Noncompliant
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmac" + "SHA512")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantToString(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // FN
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512".toString())
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantInlineSecretKeyFactoryGetInstance(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Noncompliant
        SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(keySpec)
    }

    fun noncompliantEverythingInline(password: String, salt: ByteArray) {
        SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(
        //                           ^^^^^^^^^^^^^^^^^^^^^^>
            PBEKeySpec(password.toCharArray(), salt, 110000, 256) // Noncompliant
            //                                       ^^^^^^
        )
    }

    fun noncompliantEverythingInlineWithLet(password: String, salt: ByteArray) {
        PBEKeySpec(password.toCharArray(), salt, 110000, 256).let { // Noncompliant
            //                                   ^^^^^^
            SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(it)
            //                           ^^^^^^^^^^^^^^^^^^^^^^<
        }
    }

    fun noncompliantWithAliases(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpecAlias(password.toCharArray(), salt, 120000, 256) // Noncompliant
        SecretKeyFactoryAlias.getInstance("PBKDF2withHmacSHA512").generateSecret(keySpec)
    }

    fun noncompliantDefaultArgumentIteration(password: String, salt: ByteArray, iteration: Int = 120000) {
        //                                                                                       ^^^^^^>
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iteration, 256) // Noncompliant
        //                                                     ^^^^^^^^^
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        //                                                  ^^^^^^^^^^^^^^^^^^^^^^<
        secretKeyFactory.generateSecret(keySpec)
    }

    fun noncompliantDefaultArgumentIterationAndAlgorithm(
        password: String,
        salt: ByteArray,
        iteration: Int = 110000,
        //               ^^^^^^>
        algorithm: String = "PBKDF2withHmacSHA512",
        //                  ^^^^^^^^^^^^^^^^^^^^^^>
    ) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iteration, 256) // Noncompliant
        //                                                     ^^^^^^^^^
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm)
        secretKeyFactory.generateSecret(keySpec)
    }

    class NoncompliantDefaultPrimaryConstructorArgumentIterationAndAlgorithm(
        password: String,
        salt: ByteArray,
        val iteration: Int = 110000,
        //                   ^^^^^^>
        val algorithm: String = "PBKDF2withHmacSHA512",
        //                      ^^^^^^^^^^^^^^^^^^^^^^>
    ) {
        fun test(password: String, salt: ByteArray) {
            val keySpec = PBEKeySpec(password.toCharArray(), salt, iteration, 256) // Noncompliant
            //                                                     ^^^^^^^^^
            val secretKeyFactory = SecretKeyFactory.getInstance(algorithm)
            secretKeyFactory.generateSecret(keySpec)
        }
    }

    // endregion

    // region Compliant cases

    fun compliantIntLiteralAboveThresholdForSHA512(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 210000, 256) // Compliant: 210_000 >= 210_000
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun compliantIntLiteralAboveThresholdForSHA256(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 700000, 256) // Compliant: 700_000 >= 600_000
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun compliantIntLiteralAboveThresholdForSHA1(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 1_400_000, 256) // Compliant: 1_400_000 >= 1_300_000
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun compliantUnknownAlgorithm(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Compliant: unknown algorithm
        val secretKeyFactory = SecretKeyFactory.getInstance("unknown")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun compliantComplexFlow(password: String, salt: ByteArray) {
        val iterations = if (salt.size > 10) 210000 else 60000
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256) // Compliant: salt.size can be anything
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        secretKeyFactory.generateSecret(keySpec)
    }

    fun compliantWithoutSecretKeyFactoryGenerateSecret(password: String, salt: ByteArray) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Compliant: no generateSecret
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
    }

    fun compliantWithoutSecretKeyFactoryGetInstance(password: String, salt: ByteArray, secretKeyFactory: SecretKeyFactory) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120000, 256) // Compliant: no getInstance
        secretKeyFactory.generateSecret(keySpec)
    }

    fun compliantMultipleKeySpec(password: String, salt: ByteArray) {
        val keySpec1 = PBEKeySpec(password.toCharArray(), salt, 200000, 256)
        val keySpec2 = PBEKeySpec(password.toCharArray(), salt, 220000, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        // This is commented out: secretKeyFactory.generateSecret(keySpec1)
        secretKeyFactory.generateSecret(keySpec2) // Compliant: keySpec2 has enough iterations
    }

    fun compliantDefaultArgumentIterationAndAlgorithm(
        password: String,
        salt: ByteArray,
        iteration: Int = 220000,
        algorithm: String = "PBKDF2withHmacSHA512",
    ) {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iteration, 256) // Compliant: 220_000 >= 210_000
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm)
        secretKeyFactory.generateSecret(keySpec)
    }

    // endregion
}
