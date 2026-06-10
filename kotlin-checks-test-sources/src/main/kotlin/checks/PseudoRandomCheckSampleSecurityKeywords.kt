package checks

import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.RandomStringUtils

// SONARKT-770: no crypto import. Security keywords reached via per-scope identifier scan.
class PseudoRandomCheckSampleSecurityKeywords {

    // --- Method-scope keyword tokenized from camelCase (`userPassword` -> [user, password]). ---
    fun camelCaseLocal() {
        val userPassword = "x"
        val r = Random() // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
//              ^^^^^^
        r.nextInt()
    }

    // --- Method-scope keyword tokenized from snake_case (`user_token` -> [user, token]). ---
    fun snakeCaseLocal() {
        val user_token = "x"
        val d = Math.random() // Noncompliant
    }

    // --- Method-scope keyword from all-uppercase (`HMAC` -> [hmac]). ---
    fun upperCaseLocal() {
        val HMAC = 32
        val v = ThreadLocalRandom.current().nextInt() // Noncompliant
    }

    // --- Keyword in the method name itself. ---
    fun encryptPayload() {
        val f = RandomUtils.nextFloat() // Noncompliant
    }

    // --- Keyword in a parameter name. ---
    fun randomFromToken(token: String): String {
        return RandomStringUtils.random(1) // Noncompliant
    }

    // --- Whole-identifier match (`password`). ---
    fun wholeIdentifierMatch() {
        val password = "x"
        val r = Random() // Noncompliant
    }

    // --- Keyword "key" (new in Kotlin spec, absent from Java). ---
    fun keyDerivation() {
        val key = ByteArray(16)
        val kr = kotlin.random.Random(0) // Noncompliant
    }

    // --- Keyword "hash" (new in Kotlin spec, absent from Java). ---
    fun computeHash() {
        val r = Random() // Noncompliant
    }

    // --- Keyword "symmetric" (new in Kotlin spec, absent from Java). ---
    fun symmetricCipherSetup() {
        val r = Random() // Noncompliant
    }

    // --- No keyword in scope: must NOT be flagged (heuristic suppresses it). ---
    fun unrelatedScope() {
        var counter = 0
        val r = Random() // Compliant
        counter += r.nextInt()
    }

    // --- camelCase that DOES NOT match keyword after tokenization. ---
    // `randomBytes` tokenizes to [random, bytes]; neither is in the keyword set
    // (the keyword `randombytes` only matches an all-lowercase or all-uppercase literal).
    fun splitRandomBytes() {
        val randomBytes = ByteArray(16)
        val r = Random() // Compliant
        r.nextBytes(randomBytes)
    }
}
