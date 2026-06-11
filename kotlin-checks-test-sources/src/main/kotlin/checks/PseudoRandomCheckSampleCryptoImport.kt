package checks

import java.util.Random
import org.springframework.security.crypto.encrypt.Encryptors

// file imports `org.springframework.security.*` -> all PRNG calls flagged
class PseudoRandomCheckSampleCryptoImport {

    val cryptoFactory: () -> Any = { Encryptors.noOpText() }

    fun noKeywordsInScope() {
        var counter = 0
        val r = Random() // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
        counter += r.nextInt()
    }

    fun anotherNeutralMethod(): Double = Math.random() // Noncompliant
}
