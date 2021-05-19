package checks

import java.io.File
import java.security.Provider
import java.util.Properties
import javax.crypto.Cipher

internal abstract class EncryptionAlgorithmCheckSample {
    fun foo(props: Properties) {
        /*
    should complain:
    - everytime ECB mode is used whatever the encryption algorithm
    - By default without specifying operation mode ECB is chosen
    - when CBC mode is used with PKCS5Padding or PKCS7Padding
    - when RSA is used without OAEPWithSHA-1AndMGF1Padding or OAEPWITHSHA-256ANDMGF1PADDING padding scheme
    */
        // First case
        Cipher.getInstance("AES") // Noncompliant {{Use secure mode and padding scheme.}}
//                         ^^^^^
        Cipher.getInstance("AES/ECB/NoPadding") // Noncompliant
        Cipher.getInstance("AES" + "/ECB/NoPadding") // Noncompliant
        Cipher.getInstance("AES/ECB/NoPadding", provider) // Noncompliant
        Cipher.getInstance("AES/ECB/NoPadding", "someProvider") // Noncompliant
        Cipher.getInstance("Blowfish/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("DES/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("AES/GCM/NoPadding") // Compliant

        // Second case
        Cipher.getInstance("AES/CBC/PKCS5Padding") // Compliant - CBC considered as safe
        Cipher.getInstance("Blowfish/CBC/PKCS5Padding") // Compliant - CBC considered as safe
        Cipher.getInstance("DES/CBC/PKCS5Padding") // Compliant - CBC considered as safe
        Cipher.getInstance("AES/CBC/PKCS7Padding") // Compliant - CBC considered as safe
        Cipher.getInstance("Blowfish/CBC/PKCS7Padding") // Compliant - CBC considered as safe
        Cipher.getInstance("DES/CBC/PKCS7Padding") // Compliant - CBC considered as safe
        Cipher.getInstance("DES/CBC/NoPadding") // Compliant - CBC considered as safe
        Cipher.getInstance("AES/GCM/NoPadding") // Compliant
        Cipher.getInstance("Blowfish/GCM/NoPadding") // Compliant

        // Third case
        Cipher.getInstance("RSA/NONE/NoPadding") // Noncompliant
        Cipher.getInstance("RSA/GCM/NoPadding") // Noncompliant
        Cipher.getInstance("RSA/ECB/NoPadding") // Noncompliant

        // SUN Security Provider (default for openjdk 11 for example) treats ECB as None
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding") // Compliant
        Cipher.getInstance("RSA/NONE/OAEPWithSHA-1AndMGF1Padding") // Compliant
        Cipher.getInstance("RSA/NONE/OAEPWITHSHA-256ANDMGF1PADDING") // Compliant
        Cipher.getInstance("RSA/None/OAEPWITHSHA-384ANDMGF1PADDING") // Compliant
        Cipher.getInstance("RSA/None/OAEPWITHSHA-512ANDMGF1PADDING") // Compliant

        // Other
        Cipher.getInstance(null) // Compliant
        Cipher.getInstance("") // Noncompliant
        val algo = props.getProperty("myAlgo", "AES/ECB/PKCS5Padding")
        Cipher.getInstance(algo) // Noncompliant
//                         ^^^^

        val aes = "AES"
        val ecb = "ECB"
        val pck = "PKCS5Padding"

        val algo2 = props.getProperty("myAlgo", "$aes/$ecb/$pck")
        Cipher.getInstance(algo2) // Noncompliant
//                         ^^^^^

        val aes2 = "AES"
        var ecb2 = "ECB"
        val pck2 = "PKCS5Padding"

        Cipher.getInstance(props.getProperty("myAlgo", "$aes/$ecb2/$pck")) // Compliant, ecb2 is 'var'

        val s = "RSA/NONE/NoPadding" // Compliant

        Cipher.getInstance(s) // Noncompliant
//                         ^
        val sPlus = "RSA" + "/NONE/NoPadding"
        val sInterpolated = "$RSA$NO_PADDING"
        Cipher.getInstance(sPlus) // Noncompliant
        Cipher.getInstance(sInterpolated) // Noncompliant
        Cipher.getInstance(RSA_NO_PADDING) // Noncompliant
        Cipher.getInstance(File.separator) // Compliant, can not resolve the declaration, for coverage
        
        // Case is ignored
        Cipher.getInstance("rsa/NONE/NoPadding") // Noncompliant
        Cipher.getInstance("AES/ecb/NoPadding") // Noncompliant
        Cipher.getInstance("aes/GCM/NoPadding") // Compliant
        Cipher.getInstance("DES/CBC/NOPADDING") // Compliant
        Cipher.getInstance("RSA/NONE/OAEPWITHSHA-1AndMGF1Padding") // Compliant
        val algoUpperCase = props.getProperty("myAlgo", "AES/ECB/PKCS5PADDING")

        val secureAlgo = props.getProperty("myAlgo", "DES/CBC/NOPADDING")
        Cipher.getInstance(secureAlgo) // Compliant

        val withoutDefault = props.getProperty("myAlgo")
        Cipher.getInstance(withoutDefault) // Compliant

        val algo3 = getAlgo()
        Cipher.getInstance(algo3)
        Cipher.getInstance(getAlgo())

        val algo4 = props.xyz
        Cipher.getInstance(algo4)

        val sMinus = "RSA" - "/NONE/NoPadding"
        Cipher.getInstance(sMinus) // Compliant
    }

    private infix operator fun String.minus(other: String) = this + other

    val Properties.xyz : String
    get() = "XYZ"

    private fun getAlgo(): String {
        TODO("Not yet implemented")
    }

    abstract val provider: Provider?

    companion object {
        const val RSA = "RSA"
        const val NO_PADDING = "/NONE/NoPadding"
        const val RSA_NO_PADDING = RSA + NO_PADDING
    }
}
