package checks

import java.nio.charset.Charset
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

fun noncompliant(password: CharArray) {
    // Noncompliant@+3 {{Make this salt unpredictable.}}
    val saltConst1 = "notrandom".toByteArray()
//                   ^^^^^^^^^^^^^^^^^^^^^^^^^>
    val cipherSpec1 = PBEParameterSpec(saltConst1, 10000)
//                                     ^^^^^^^^^^
    val spec1a = PBEKeySpec(password, saltConst1, 10000, 256) // Noncompliant {{Make this salt unpredictable.}}
    val spec1b = PBEKeySpec(password, saltConst1, 10000) // Noncompliant {{Make this salt unpredictable.}}

    val saltConstJava1 = ("notrandom" as java.lang.String).getBytes()
    PBEParameterSpec(saltConstJava1, 10000) // Noncompliant {{Make this salt unpredictable.}}
    PBEKeySpec(password, saltConstJava1, 10000, 256) // Noncompliant {{Make this salt unpredictable.}}

    val saltConstJava2 = ("notrandom" as java.lang.String).bytes
    PBEParameterSpec(saltConstJava2, 10000) // Noncompliant {{Make this salt unpredictable.}}
    PBEKeySpec(password, saltConstJava2, 10000, 256) // Noncompliant {{Make this salt unpredictable.}}

    val saltConst2 = ByteArray(16)
    PBEParameterSpec(saltConst2, 10000) // Noncompliant {{Make this salt unpredictable.}}
    PBEKeySpec(password, saltConst2, 10000, 256) // Noncompliant {{Make this salt unpredictable.}}

    val random = SecureRandom()
    // Noncompliant@+4 {{Make this salt at least 16 bytes.}}
    val saltTooShort = ByteArray(15)
//                     ^^^^^^^^^^^^^>
    random.nextBytes(saltTooShort)
    val cipherSpec2 = PBEParameterSpec(saltTooShort, 10000)
//                                     ^^^^^^^^^^^^
    val spec2 = PBEKeySpec(password, saltTooShort, 10000, 256) // Noncompliant {{Make this salt at least 16 bytes.}}

    PBEKeySpec(password) // Noncompliant {{Add an unpredictable salt value to this hash.}}
//  ^^^^^^^^^^^^^^^^^^^^

    PBEParameterSpec("notRandomAtAll".toByteArray(Charset.defaultCharset()), 10000) // Noncompliant {{Make this salt unpredictable.}}
    val someConst = "constantNotRandom"
    PBEParameterSpec(someConst.toByteArray(Charset.defaultCharset()), 10000) // Noncompliant {{Make this salt unpredictable.}}
    PBEParameterSpec(ByteArray(16), 10000) // Noncompliant {{Make this salt unpredictable.}}

    val saltInitializedTooLate = ByteArray(16)
    PBEParameterSpec(saltInitializedTooLate, 10000) // Noncompliant {{Make this salt unpredictable.}}
    SecureRandom().nextBytes(saltInitializedTooLate)
}

fun compliant(password: CharArray) {
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)
    val cipherSpec = PBEParameterSpec(salt, 10000) // Compliant
    val spec = PBEKeySpec(password, salt, 10000, 256) // Compliant
    PBEKeySpec(password, salt, 10000) // Compliant

    PBEKeySpec(password, ByteArray(16) { it.toByte() }, 10000) // Compliant

    if (true) {
        PBEKeySpec(password, salt, 10000) // Compliant
    }
}
