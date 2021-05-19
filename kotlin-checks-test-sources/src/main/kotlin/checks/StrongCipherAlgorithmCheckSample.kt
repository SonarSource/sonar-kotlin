package checks

import java.security.NoSuchAlgorithmException
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.NullCipher

class StrongCipherAlgorithmCheckSample {
    private val DES = "DES"

    fun foo() {
        Cipher.getInstance("DESede/ECB/PKCS5Padding") // Noncompliant {{Use a strong cipher algorithm.}} [[sc=28;ec=53]]
        Cipher.getInstance("DES/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("RC2/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("""DES""") // Noncompliant
        Cipher.getInstance("AES/GCM/NoPadding") //Compliant
        NullCipher() // Noncompliant {{Use a strong cipher algorithm.}} [[sc=9;ec=19]]
        NullCipher() // Noncompliant
        MyCipher()

        // DES
        Cipher.getInstance("DES") // Noncompliant
        Cipher.getInstance(DES) // Noncompliant
        Cipher.getInstance("DES/ECB") // Noncompliant
        Cipher.getInstance("DES/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("DES/GCM") // Noncompliant
        Cipher.getInstance("DES/GCM/NoPadding") // Noncompliant
        Cipher.getInstance("DES/GCM/PKCS5Padding") // Noncompliant

        // 3DES
        Cipher.getInstance("DESede") // Noncompliant
        Cipher.getInstance("DESede/ECB") // Noncompliant
        Cipher.getInstance("DESede/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("DESede/GCM") // Noncompliant
        Cipher.getInstance("DESede/GCM/NoPadding") // Noncompliant
        Cipher.getInstance("DESede/GCM/PKCS5Padding") // Noncompliant

        // DESedeWrap
        Cipher.getInstance("DESedeWrap") // Noncompliant
        Cipher.getInstance("DESedeWrap/GCM/PKCS5Padding") // Noncompliant

        // RC2
        Cipher.getInstance("RC2") // Noncompliant
        Cipher.getInstance("RC2/ECB") // Noncompliant
        Cipher.getInstance("RC2/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("RC2/GCM") // Noncompliant
        Cipher.getInstance("RC2/GCM/NoPadding") // Noncompliant
        Cipher.getInstance("RC2/GCM/PKCS5Padding") // Noncompliant

        // ARC2 (alias of RC2, not officially supported)
        Cipher.getInstance("ARC2") // Noncompliant
        Cipher.getInstance("ARC2/GCM/PKCS5Padding") // Noncompliant

        // RC4
        Cipher.getInstance("RC4") // Noncompliant
        Cipher.getInstance("RC4/ECB") // Noncompliant
        Cipher.getInstance("RC4/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("RC4/GCM") // Noncompliant
        Cipher.getInstance("RC4/GCM/NoPadding") // Noncompliant
        Cipher.getInstance("RC4/GCM/PKCS5Padding") // Noncompliant

        // ARC4
        Cipher.getInstance("ARC4") // Noncompliant
        Cipher.getInstance("ARC4/GCM/PKCS5Padding") // Noncompliant

        // ARCFOUR
        Cipher.getInstance("ARCFOUR") // Noncompliant
        Cipher.getInstance("ARCFOUR/GCM/PKCS5Padding", "IAIK") // Noncompliant

        // Blowfish
        Cipher.getInstance("Blowfish") // Noncompliant
        Cipher.getInstance("Blowfish/ECB") // Noncompliant
        Cipher.getInstance("Blowfish/ECB/PKCS5Padding") // Noncompliant
        Cipher.getInstance("Blowfish/GCM") // Noncompliant
        Cipher.getInstance("Blowfish/GCM/NoPadding") // Noncompliant
        Cipher.getInstance("Blowfish/GCM/PKCS5Padding") // Noncompliant
        Cipher.getInstance("AES/GCM/NoPadding") // Compliant
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    fun usingJavaUtilProperties(props: Properties, otherAlgo: String?) {
        val algo0 = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding")
        Cipher.getInstance(algo0) // Noncompliant
        var algo1 = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding")
        Cipher.getInstance(algo1) // Compliant (var)
        Cipher.getInstance(props.getProperty("myAlgo", "DES/ECB/PKCS5Padding")) // Noncompliant
        Cipher.getInstance(getAlgo()) // Compliant
        Cipher.getInstance("/") // Compliant
        val algo2 = props.getProperty("myAlgo")
        Cipher.getInstance(algo2) // Compliant
        var algo3 = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding")
        algo3 = "myOtherAlgo"
        Cipher.getInstance(algo3) // Compliant
        val algo4 = getAlgo()
        Cipher.getInstance(algo4) // Compliant
        Cipher.getInstance(otherAlgo) // Compliant
        val algo5 = "myAlgo"
        Cipher.getInstance(algo5) // Compliant
        val algo6 = props.getProperty("myAlgo", getAlgo())
        Cipher.getInstance(algo6) // Compliant
    }

    private fun getAlgo(): String? {
        return null
    }
}

internal class MyCipher : Cipher(null, null, "")
