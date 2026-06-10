/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val MESSAGE = "Make sure that using this pseudorandom number generator is safe here."

private val MATH_RANDOM_MATCHER = FunMatcher(qualifier = "java.lang.Math", name = "random") {
    withNoArguments()
}

private val KOTLIN_RANDOM_MATCHER = FunMatcher(qualifier = "kotlin.random", name = "Random")

private const val LANG3_RANDOM_STRING_UTILS = "org.apache.commons.lang3.RandomStringUtils"
private val RANDOM_TYPES_MATCHER = FunMatcher {
    qualifiers = setOf(
        "java.util.concurrent.ThreadLocalRandom",
        "org.apache.commons.lang.math.RandomUtils",
        "org.apache.commons.lang3.RandomUtils",
        "org.apache.commons.lang.RandomStringUtils",
        "org.apache.commons.lang3.RandomStringUtils",
    )
}

private val RANDOM_STRING_UTILS_SECURE_INSTANCES = FunMatcher {
    definingSupertypes = setOf(LANG3_RANDOM_STRING_UTILS)
    names = setOf("secure", "secureStrong")
}

private val RANDOM_CONSTRUCTORS_MATCHER = FunMatcher(matchConstructor = true) {
    qualifiers = setOf(
        "java.util.Random",
        "org.apache.commons.lang.math.JVMRandom",
    )
}

// SONARKT-770: ported from DART-276 security-context heuristic.
// When any of these prefixes appear in the file's imports, all PRNG usages in the file are reported.
private val CRYPTO_IMPORT_PREFIXES = listOf(
    "java.security.",
    "javax.crypto.",
    "org.springframework.security.",
    "org.bouncycastle.",
    "io.jsonwebtoken.",
    "com.auth0.jwt.",
    "at.favre.lib.crypto.",
)

private val SECURITY_KEYWORDS = setOf(
    "aes", "asymmetric", "auth", "certificate", "chacha20", "cipher", "crypto", "cryptography",
    "decrypt", "ecc", "encrypt", "encryption", "hash", "hmac", "iv", "key", "nonce", "password",
    "pbkdf2", "poly1305", "randombytes", "rsa", "salt", "scrypt", "secret", "secure", "security",
    "sessionid", "signature", "symmetric", "token", "verify",
)

private val UPPER_CASE_BOUNDARY = Regex("(?=[A-Z])")

@Rule(key = "S2245")
class PseudoRandomCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        MATH_RANDOM_MATCHER,
        KOTLIN_RANDOM_MATCHER,
        RANDOM_TYPES_MATCHER,
        RANDOM_CONSTRUCTORS_MATCHER,
    )

    private var cryptoImportPresent: Boolean = false

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        cryptoImportPresent = file.importDirectives.any { directive ->
            val fqn = directive.importedFqName?.asString() ?: return@any false
            CRYPTO_IMPORT_PREFIXES.any { fqn.startsWith(it) }
        }
    }

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val calleeExpression = callExpression.calleeExpression ?: return
        if ((matchedFun == RANDOM_TYPES_MATCHER && callExpression.isChainedMethodInvocation())
            || RANDOM_STRING_UTILS_SECURE_INSTANCES.matches(callExpression)
        ) {
            return
        }
        if (!isInSecurityContext(callExpression)) {
            return
        }
        kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
    }

    private fun isInSecurityContext(callExpression: KtCallExpression): Boolean {
        if (cryptoImportPresent) {
            return true
        }
        val scope = findDeclarationScope(callExpression) ?: return false
        return collectIdentifierNames(scope).any { name ->
            tokenizeIdentifier(name).any { it in SECURITY_KEYWORDS }
        }
    }
}

private fun KtCallExpression.isChainedMethodInvocation() = parent?.let {
    it is KtDotQualifiedExpression && it.receiverExpression is KtDotQualifiedExpression &&
        (it.receiverExpression as KtDotQualifiedExpression).selectorExpression is KtCallExpression
} ?: false

// Mirrors Dart `declarationScope`: closest enclosing named function for local code; falls back to
// the enclosing class/object for property initializers and init blocks; falls back to the KtFile
// for top-level property initializers.
private fun findDeclarationScope(element: PsiElement): PsiElement? {
    var current: PsiElement? = element.parent
    while (current != null) {
        when (current) {
            is KtNamedFunction -> return current
            is KtClassOrObject -> return current
            is KtFile -> return current
        }
        current = current.parent
    }
    return null
}

private fun collectIdentifierNames(scope: PsiElement): List<String> {
    val names = mutableListOf<String>()
    PsiTreeUtil.processElements(scope) { element ->
        if (element is LeafPsiElement && element.elementType == KtTokens.IDENTIFIER) {
            names.add(element.text)
        }
        true
    }
    return names
}

// Mirrors Dart `_splitIntoWords`: split on `_` first; for each non-empty part, either keep it as
// a single lowercase word when all-uppercase (with at least one letter), or split further on
// capital-letter boundaries.
internal fun tokenizeIdentifier(identifier: String): List<String> {
    val words = mutableListOf<String>()
    for (part in identifier.split('_')) {
        if (part.isEmpty()) continue
        if (isAllUppercaseWithLetter(part)) {
            words.add(part.lowercase())
        } else {
            for (sub in part.split(UPPER_CASE_BOUNDARY)) {
                if (sub.isNotEmpty()) {
                    words.add(sub.lowercase())
                }
            }
        }
    }
    return words
}

private fun isAllUppercaseWithLetter(part: String): Boolean {
    var hasLetter = false
    for (c in part) {
        if (c.isLetter()) {
            hasLetter = true
            if (c.isLowerCase()) return false
        }
    }
    return hasLetter
}
