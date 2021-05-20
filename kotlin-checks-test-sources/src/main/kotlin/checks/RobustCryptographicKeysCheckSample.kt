package checks

import java.security.InvalidAlgorithmParameterException
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.util.Optional
import javax.crypto.KeyGenerator

internal class CryptographicKeySizeCheckRSA {
    @Throws(NoSuchAlgorithmException::class)
    fun key_variable() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024) // Noncompliant  {{Use a key length of at least 2048 bits for RSA cipher algorithm.}} [[sc=5;ec=28]]
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_assign_after_decl() {
        var keyGen: KeyPairGenerator
        keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024) // Compliant FN - we currently don't track data in vars
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_lowercase() {
        val keyGen = KeyPairGenerator.getInstance("rsa")
        keyGen.initialize(1024) // Noncompliant
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_compliant() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048) // Compliant
    }

    fun also_let_run_constructs() {
        KeyPairGenerator.getInstance("RSA").also {
            it.initialize(1024) // Noncompliant
        }

        KeyPairGenerator.getInstance("RSA").run {
            initialize(1024) // Noncompliant
        }

        KeyPairGenerator.getInstance("RSA").run {
            this.initialize(1024) // Compliant FN - We currently don't resolve the 'this'
        }

        KeyPairGenerator.getInstance("RSA").run {
            this.let {
                it.initialize(1024) // Compliant FN - We currently don't resolve the 'this'
            }
        }

        KeyPairGenerator.getInstance("RSA").also {
            it.run {
                initialize(1024) // Noncompliant
            }
        }

        KeyPairGenerator.getInstance("RSA").let {
            it.initialize(1024) // Noncompliant
        }

        KeyPairGenerator.getInstance("RSA").let { foo ->
            foo.initialize(1024) // Noncompliant
        }

        KeyPairGenerator.getInstance("RSA").also {
            it.initialize(1024) // Noncompliant
        }.let {
            println("do stuff here with $it")
        }

        KeyPairGenerator.getInstance("RSA").also {
            "some".run {
                "thing".let { _ ->
                    it.initialize(1024) // Noncompliant
                }
            }
        }

        KeyPairGenerator.getInstance("RSA").also { kpg ->
            1024.let {
                kpg.initialize(it) // Noncompliant
            }
        }

        KeyPairGenerator.getInstance("RSA").also {
            1024.let { size ->
                it.initialize(size) // Noncompliant
            }
        }

        KeyPairGenerator.getInstance("RSA").also {
            if("the moon and the stars".length > Math.random()) {
                it.initialize(1024) // Noncompliant
            }
        }

        KeyPairGenerator.getInstance("RSA").also {
            1024.run {
                it.initialize(this) // Compliant FN - we currently don't resolve the 'this'
            }
        }
    }
    fun with_construct() {
        with(KeyPairGenerator.getInstance("RSA")) {
            initialize(1024) // Noncompliant
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun report_twice() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024) // Noncompliant
        keyGen.initialize(1023) // Noncompliant
    }

    @Throws(NoSuchAlgorithmException::class)
    fun report_only_once(size: Int) {
        if (size == 1) {
            val keyGen = KeyPairGenerator.getInstance("DH")
            keyGen.initialize(1) // Noncompliant {{Use a key length of at least 2048 bits for DH cipher algorithm.}}
        } else if (size == 2) {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2) // Noncompliant {{Use a key length of at least 2048 bits for RSA cipher algorithm.}}
        } else if (size == 3) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(3) // Noncompliant {{Use a key length of at least 128 bits for AES cipher algorithm.}}
        } else {
            val keyGen = KeyPairGenerator.getInstance("DH")
            keyGen.initialize(2048) // Compliant
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun report_only_once_2(size: Int) {
        if (size == 1) {
            val keyGen = KeyPairGenerator.getInstance("DH")
            Optional.of(keyGen).get().initialize(1) // Compliant, FN
        } else if (size == 2) {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            Optional.of(keyGen).get().initialize(2) // Compliant, FN
        } else if (size == 3) {
            val keyGen = KeyGenerator.getInstance("AES")
            Optional.of(keyGen).get().init(3) // Compliant, FN
        } else {
            val keyGen = KeyPairGenerator.getInstance("DH")
            Optional.of(keyGen).get().initialize(2048) // Compliant, FN
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun chained_2(size: Int) {
        KeyPairGenerator.getInstance("RSA").initialize(1) // Noncompliant
        KeyPairGenerator.getInstance("RSA").initialize(1024) // Noncompliant
        KeyPairGenerator.getInstance("RSA").initialize(2048) // Compliant
    }

    @Throws(NoSuchAlgorithmException::class)
    fun not_assigned() {
        KeyPairGenerator.getInstance("RSA") // Not assigned, nothing to do
    }
}

internal class CryptographicKeySizeCheckRSA2 {
    var keyGen = KeyPairGenerator.getInstance("RSA")
    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_this() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        this.keyGen.initialize(1024) // Compliant, only support when declared in the same method
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_instance() {
        val rsa2 = CryptographicKeySizeCheckRSA2()
        rsa2.keyGen = KeyPairGenerator.getInstance("RSA")
        rsa2.setKeyGen()
        rsa2.keyGen.initialize(1024) // Compliant, only support when declared in the same method
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun setKeyGen() {
        keyGen = KeyPairGenerator.getInstance("DH")
    }
}


internal interface CryptographicKeySizeCheckI {
    companion object {
        val r = Runnable {
            try {
                val keyGen = KeyPairGenerator.getInstance("RSA")
            } catch (e: NoSuchAlgorithmException) {
            }
        }
    }
}

internal class CryptographicKeySizeCheckDH {
    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_DH() {
        val keyGen = KeyPairGenerator.getInstance("DH")
        keyGen.initialize(1024) // Noncompliant  {{Use a key length of at least 2048 bits for DH cipher algorithm.}} [[sc=5;ec=28]]
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_compliant_DH() {
        val keyGen = KeyPairGenerator.getInstance("DH")
        keyGen.initialize(2048) // Compliant
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable() {
        val keyGen = KeyPairGenerator.getInstance("DiffieHellman")
        keyGen.initialize(1024) // Noncompliant {{Use a key length of at least 2048 bits for DiffieHellman cipher algorithm.}} [[sc=5;ec=28]]
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_compliant() {
        val keyGen = KeyPairGenerator.getInstance("DiffieHellman")
        keyGen.initialize(2048) // Compliant
    }
}

internal class CryptographicKeySizeCheckDSA {
    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_DSA() {
        val keyGen = KeyPairGenerator.getInstance("DSA")
        keyGen.initialize(1024, // Noncompliant {{Use a key length of at least 2048 bits for DSA cipher algorithm.}} [[sc=27;ec=31]]
            SecureRandom())
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_compliant_DSA() {
        val keyGen = KeyPairGenerator.getInstance("DSA")
        keyGen.initialize(2048, SecureRandom()) // Compliant
    }
}

internal class CryptographicKeySizeCheckAES {
    @Throws(NoSuchAlgorithmException::class)
    fun key_variable() {
        val keyGen1 = KeyGenerator.getInstance("AES")
        keyGen1.init(64) // Noncompliant {{Use a key length of at least 128 bits for AES cipher algorithm.}} [[sc=22;ec=24]]
    }

    @Throws(NoSuchAlgorithmException::class)
    fun key_variable_compliant() {
        val keyGen2 = KeyGenerator.getInstance("AES")
        keyGen2.init(128) // Compliant
    }
}

internal class CryptographicKeySizeCheckEC {
    @Throws(InvalidAlgorithmParameterException::class, NoSuchAlgorithmException::class)
    fun key_EC() {
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        val ecSpec1 =
            ECGenParameterSpec("secp112r1") // Noncompliant {{Use a key length of at least 224 bits for EC cipher algorithm.}} [[sc=34;ec=69]]
        keyPairGen.initialize(ecSpec1)
        val ecSpec2 = ECGenParameterSpec("secp112r2") // Noncompliant
        keyPairGen.initialize(ecSpec2)
        val ecSpec3 = ECGenParameterSpec("secp128r1") // Noncompliant
        keyPairGen.initialize(ecSpec3)
        val ecSpec4 = ECGenParameterSpec("secp128r2") // Noncompliant
        keyPairGen.initialize(ecSpec4)
        val ecSpec5 = ECGenParameterSpec("secp160k1") // Noncompliant
        keyPairGen.initialize(ecSpec5)
        val ecSpec6 = ECGenParameterSpec("secp160r1") // Noncompliant
        keyPairGen.initialize(ecSpec6)
        val ecSpec7 = ECGenParameterSpec("secp160r2") // Noncompliant
        keyPairGen.initialize(ecSpec7)
        val ecSpec8 = ECGenParameterSpec("secp192k1") // Noncompliant
        keyPairGen.initialize(ecSpec8)
        val ecSpec9 = ECGenParameterSpec("secp192r1") // Noncompliant
        keyPairGen.initialize(ecSpec9)
        val ecSpec10 = ECGenParameterSpec("secp224k1") // compliant
        keyPairGen.initialize(ecSpec10)
        val ecSpec11 = ECGenParameterSpec("secp224r1") // compliant
        keyPairGen.initialize(ecSpec11)
        val ecSpec12 = ECGenParameterSpec("secp256k1") // compliant
        keyPairGen.initialize(ecSpec12)
        val ecSpec13 = ECGenParameterSpec("secp256r1") // compliant
        keyPairGen.initialize(ecSpec13)
        val ecSpec14 = ECGenParameterSpec("secp384r1") // compliant
        keyPairGen.initialize(ecSpec14)
        val ecSpec15 = ECGenParameterSpec("secp521r1") // compliant
        keyPairGen.initialize(ecSpec15)
        val ecSpec16 = ECGenParameterSpec("prime192v2") // Noncompliant
        keyPairGen.initialize(ecSpec16)
        val ecSpec17 = ECGenParameterSpec("prime192v3") // Noncompliant
        keyPairGen.initialize(ecSpec17)
        val ecSpec18 = ECGenParameterSpec("prime239v1") // compliant
        keyPairGen.initialize(ecSpec18)
        val ecSpec19 = ECGenParameterSpec("prime239v2") // compliant
        keyPairGen.initialize(ecSpec19)
        val ecSpec20 = ECGenParameterSpec("prime239v3") // compliant
        keyPairGen.initialize(ecSpec20)
        val ecSpec21 = ECGenParameterSpec("sect113r1") // Noncompliant
        keyPairGen.initialize(ecSpec21)
        val ecSpec22 = ECGenParameterSpec("sect113r2") // Noncompliant
        keyPairGen.initialize(ecSpec22)
        val ecSpec24 = ECGenParameterSpec("sect131r1") // Noncompliant
        keyPairGen.initialize(ecSpec24)
        val ecSpec25 = ECGenParameterSpec("sect131r2") // Noncompliant
        keyPairGen.initialize(ecSpec25)
        val ecSpec26 = ECGenParameterSpec("sect163k1") // Noncompliant
        keyPairGen.initialize(ecSpec26)
        val ecSpec27 = ECGenParameterSpec("sect163r1") // Noncompliant
        keyPairGen.initialize(ecSpec27)
        val ecSpec28 = ECGenParameterSpec("sect163r2") // Noncompliant
        keyPairGen.initialize(ecSpec28)
        val ecSpec29 = ECGenParameterSpec("sect193r1") // Noncompliant
        keyPairGen.initialize(ecSpec29)
        val ecSpec30 = ECGenParameterSpec("sect193r2") // Noncompliant
        keyPairGen.initialize(ecSpec30)
        val ecSpec31 = ECGenParameterSpec("sect233k1") // compliant
        keyPairGen.initialize(ecSpec31)
        val ecSpec32 = ECGenParameterSpec("sect233r1") // compliant
        keyPairGen.initialize(ecSpec32)
        val ecSpec33 = ECGenParameterSpec("sect239k1") // compliant
        keyPairGen.initialize(ecSpec33)
        val ecSpec34 = ECGenParameterSpec("sect283k1") // compliant
        keyPairGen.initialize(ecSpec34)
        val ecSpec35 = ECGenParameterSpec("sect283r1") // compliant
        keyPairGen.initialize(ecSpec35)
        val ecSpec36 = ECGenParameterSpec("sect409k1") // compliant
        keyPairGen.initialize(ecSpec36)
        val ecSpec37 = ECGenParameterSpec("sect409r1") // compliant
        keyPairGen.initialize(ecSpec37)
        val ecSpec38 = ECGenParameterSpec("sect571k1") // compliant
        keyPairGen.initialize(ecSpec38)
        val ecSpec39 = ECGenParameterSpec("sect571r1") // compliant
        keyPairGen.initialize(ecSpec39)
        val ecSpec40 = ECGenParameterSpec("c2tnb191v1") // Noncompliant
        keyPairGen.initialize(ecSpec40)
        val ecSpec41 = ECGenParameterSpec("c2tnb191v2") // Noncompliant
        keyPairGen.initialize(ecSpec41)
        val ecSpec42 = ECGenParameterSpec("c2tnb191v3") // Noncompliant
        keyPairGen.initialize(ecSpec42)
        val ecSpec43 = ECGenParameterSpec("c2tnb239v1") // compliant
        keyPairGen.initialize(ecSpec43)
        val ecSpec44 = ECGenParameterSpec("c2tnb239v2") // compliant
        keyPairGen.initialize(ecSpec44)
        val ecSpec45 = ECGenParameterSpec("c2tnb239v3") // compliant
        keyPairGen.initialize(ecSpec45)
        val ecSpec46 = ECGenParameterSpec("c2tnb359v1") // compliant
        keyPairGen.initialize(ecSpec46)
        val ecSpec47 = ECGenParameterSpec("c2tnb431r1") // compliant
        keyPairGen.initialize(ecSpec47)
        val ecSpec48 = ECGenParameterSpec("some123v123") // compliant, unexpected key
        keyPairGen.initialize(ecSpec48)
        val ecSpec49 = ECGenParameterSpec("EC") // compliant, unexpected key
        keyPairGen.initialize(ecSpec49)
        val ecSpec50 = ECGenParameterSpec("primee123v23") // compliant, unexpected key
        keyPairGen.initialize(ecSpec50)
    }

    internal class InvalidAlgo {
        fun no_such_algo() {
            val keyGen = KeyPairGenerator.getInstance("fooAlgo that isn't noncompliant")
            keyGen.initialize(999999)
        }
    }
}
