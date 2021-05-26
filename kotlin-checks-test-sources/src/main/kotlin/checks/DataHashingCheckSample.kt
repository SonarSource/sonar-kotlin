package checks.security

import com.google.common.hash.Hashing
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.AlgorithmParameterGenerator
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.Provider
import java.security.Signature
import java.util.Properties
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.security.authentication.encoding.Md5PasswordEncoder
import org.springframework.security.authentication.encoding.ShaPasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.security.crypto.password.LdapShaPasswordEncoder
import org.springframework.security.crypto.password.Md4PasswordEncoder
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.StandardPasswordEncoder

internal class HashMethodsCheck {
    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class)
    fun myMethod(algorithm: String?, provider: Provider?, props: Properties) {
        var md: MessageDigest? = null

        md = DigestUtils.getDigest("MD2") // Noncompliant
        md = DigestUtils.getDigest("MD4") // Noncompliant
        md = DigestUtils.getDigest("MD5") // Noncompliant
        md = DigestUtils.getDigest("SHA") // Noncompliant
        md = DigestUtils.getDigest("SHA-0") // Noncompliant
        md = DigestUtils.getDigest("SHA-224") // Noncompliant
        md = DigestUtils.getDigest("MD5") // Noncompliant
        md = DigestUtils.getDigest("SHA-1") // Noncompliant
        md = DigestUtils.getDigest("SHA-256")
        md = DigestUtils.getMd5Digest() // Noncompliant
        md = DigestUtils.getShaDigest() // Noncompliant
        md = DigestUtils.getSha1Digest() // Noncompliant
        md = DigestUtils.getSha256Digest()
        DigestUtils.md2("") // Noncompliant
        DigestUtils.md2Hex("") // Noncompliant
        DigestUtils.md5("") // Noncompliant
        DigestUtils.md5Hex("") // Noncompliant
        DigestUtils.sha1("") // Noncompliant
        DigestUtils.sha1Hex("") // Noncompliant
        DigestUtils.sha("") // Noncompliant
        DigestUtils.shaHex("") // Noncompliant
        DigestUtils.sha256("")
        DigestUtils.sha256Hex("")
        md = MessageDigest.getInstance(algorithm)
        md = DigestUtils.getDigest(algorithm)
        DigestUtils.md5Hex("") // Noncompliant
        Hashing.md5() // Noncompliant
        Hashing.sha1() // Noncompliant
        Hashing.sha256()
        md = MessageDigest.getInstance("MD5", provider) // Noncompliant
        md = MessageDigest.getInstance("MD2", provider) // Noncompliant
        md = MessageDigest.getInstance("MD4", provider) // Noncompliant
        md = MessageDigest.getInstance("SHA", provider) // Noncompliant
        md = MessageDigest.getInstance("SHA-0", provider) // Noncompliant
        md = MessageDigest.getInstance("SHA-1", provider) // Noncompliant
        md = MessageDigest.getInstance("SHA-224", provider) // Noncompliant
        md = MessageDigest.getInstance("SHA1", "provider") // Noncompliant
        md = MessageDigest.getInstance("sha-1", "provider") // Noncompliant
        val myAlgo = props.getProperty("myCoolAlgo", "SHA1")
        md = MessageDigest.getInstance(myAlgo, provider) // Noncompliant
        md = MessageDigest.getInstance(algo, provider)
        md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo", "SHA-1")) // Noncompliant
        md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo"))
        md = MessageDigest.getInstance(ALGORITHM) // Noncompliant
    }

    private val algo: String?
        private get() = null

    companion object {
        private const val ALGORITHM = "MD2"
    }
}

internal class ExtendedFile(pathname: String) : File(pathname) {
    @Throws(NoSuchAlgorithmException::class)
    fun myMethod() {
        var md: MessageDigest? = null
        md = MessageDigest.getInstance(separator)
    }
}

internal class CryptoAPIs {
    @Throws(NoSuchAlgorithmException::class)
    fun mac() {
        var mac =
            Mac.getInstance("HmacMD5") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        mac =
            Mac.getInstance("HmacSHA1") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        mac = Mac.getInstance("HmacSHA256")
    }

    @Throws(NoSuchAlgorithmException::class)
    fun signature() {
        var signature =
            Signature.getInstance("SHA1withDSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        signature =
            Signature.getInstance("SHA1withRSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        signature =
            Signature.getInstance("MD2withRSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        signature =
            Signature.getInstance("MD5withRSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        signature = Signature.getInstance("SHA256withRSA") // Compliant
    }

    @Throws(NoSuchAlgorithmException::class)
    fun keys() {
        var keyGenerator =
            KeyGenerator.getInstance("HmacSHA1") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        keyGenerator = KeyGenerator.getInstance("HmacSHA256")
        keyGenerator = KeyGenerator.getInstance("AES")
        val keyPair =
            KeyPairGenerator.getInstance("HmacSHA1") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    }

    @Throws(NoSuchAlgorithmException::class)
    fun dsa() {
        AlgorithmParameters.getInstance("DSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        AlgorithmParameters.getInstance("DiffieHellman")
        AlgorithmParameterGenerator.getInstance("DSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        AlgorithmParameterGenerator.getInstance("DiffieHellman")
        KeyPairGenerator.getInstance("DSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        KeyPairGenerator.getInstance("DiffieHellman")
        KeyFactory.getInstance("DSA") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        KeyFactory.getInstance("DiffieHellman")
    }
}

internal class DeprecatedSpring {
    fun foo() {
        ShaPasswordEncoder() // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        ShaPasswordEncoder(512) // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        Md5PasswordEncoder() // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        LdapShaPasswordEncoder() // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        LdapShaPasswordEncoder(KeyGenerators.secureRandom()) // Noncompliant
        Md4PasswordEncoder() // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        MessageDigestPasswordEncoder("algo") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        NoOpPasswordEncoder.getInstance() // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        StandardPasswordEncoder() // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        StandardPasswordEncoder("foo") // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        BCryptPasswordEncoder()
    }
}

internal class SpringDigestUtils {
    @Throws(IOException::class)
    fun digestUtils() {
        org.springframework.util.DigestUtils.appendMd5DigestAsHex(ByteArray(10), StringBuilder()) // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        org.springframework.util.DigestUtils.appendMd5DigestAsHex(FileInputStream(""), StringBuilder()) // Noncompliant
        org.springframework.util.DigestUtils.md5Digest(ByteArray(10)) // Noncompliant
        org.springframework.util.DigestUtils.md5Digest(FileInputStream("")) // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
        org.springframework.util.DigestUtils.md5DigestAsHex(ByteArray(10)) // Noncompliant
        org.springframework.util.DigestUtils.md5DigestAsHex(FileInputStream("")) // Noncompliant
    }
}
