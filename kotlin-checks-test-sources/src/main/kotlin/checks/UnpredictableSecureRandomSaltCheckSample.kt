package checks

import java.security.SecureRandom

class UnpredictableSecureRandomSaltCheckSample {
    fun noncompliant() {
        val sr1 = SecureRandom()
        sr1.setSeed(123456L) // Noncompliant {{Change this seed value to something unpredictable, or remove the seed.}}
//                  ^^^^^^^
        sr1.setSeed("foo".toByteArray()) // Noncompliant
        val v1 = sr1.nextInt()

        val sr2 = SecureRandom("abcdefghijklmnop".toByteArray(charset("us-ascii"))) // Noncompliant
        val v2 = sr2.nextInt()

        // Noncompliant@+3
        val predictableByteArray = ByteArray(100)
//                                 ^^^^^^^^^^^^^^>
        SecureRandom(predictableByteArray)
//                   ^^^^^^^^^^^^^^^^^^^^
    }

    fun compliant() {
        val sr = SecureRandom()
        val v = sr.nextInt()

        val byteArray = ByteArray(50)
        sr.nextBytes(byteArray)
        SecureRandom(byteArray)
        sr.setSeed(sr.nextLong())
    }
}
