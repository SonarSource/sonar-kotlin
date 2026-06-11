package checks

import java.util.Random

// SONARKT-770: top-level property initializers have no enclosing function/class.
// Scope falls back to the KtFile. Identifiers across the whole file are scanned.

// File contains the keyword identifier `password` (top-level), so all top-level
// PRNG initializers should be flagged.
val password = "x"
val topLevelRng = Random() // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
val topLevelMath = Math.random() // Noncompliant

// Class-scope fallback: class name `TokenGenerator` -> tokens [token, generator]; `token` matches.
class TokenGenerator {
    val rng = Random() // Noncompliant
}

// Class with a security-keyword field name.
class Holder {
    val secret: String = "x"
    val r = Random() // Noncompliant
}
