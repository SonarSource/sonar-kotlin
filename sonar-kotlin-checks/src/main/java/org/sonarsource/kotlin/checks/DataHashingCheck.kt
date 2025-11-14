/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.ArgumentMatcher
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.GET_INSTANCE
import org.sonarsource.kotlin.api.checks.STRING_TYPE
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val MESSAGE = "Make sure this weak hash algorithm is not used in a sensitive context here."

private val ALGORITHM_BY_METHOD_NAME: Map<String, InsecureAlgorithm> = mapOf(
    "getMd2Digest" to InsecureAlgorithm.MD2,
    "getMd5Digest" to InsecureAlgorithm.MD5,
    "getShaDigest" to InsecureAlgorithm.SHA1,
    "getSha1Digest" to InsecureAlgorithm.SHA1,
    "md2" to InsecureAlgorithm.MD2,
    "md2Hex" to InsecureAlgorithm.MD2,
    "md5" to InsecureAlgorithm.MD5,
    "md5Hex" to InsecureAlgorithm.MD5,
    "sha1" to InsecureAlgorithm.SHA1,
    "sha1Hex" to InsecureAlgorithm.SHA1,
    "sha" to InsecureAlgorithm.SHA1,
    "shaHex" to InsecureAlgorithm.SHA1,
    "md5Digest" to InsecureAlgorithm.MD5,
    "md5DigestAsHex" to InsecureAlgorithm.MD5,
    "appendMd5DigestAsHex" to InsecureAlgorithm.MD5,
)

private val CRYPTO_APIS = listOf(
    "java.security.AlgorithmParameters",
    "java.security.AlgorithmParameterGenerator",
    "java.security.MessageDigest",
    "java.security.KeyFactory",
    "java.security.KeyPairGenerator",
    "java.security.Signature",
    "javax.crypto.Mac",
    "javax.crypto.KeyGenerator"
)

private val DEPRECATED_SPRING_PASSWORD_ENCODERS = setOf(
    "org.springframework.security.authentication.encoding.Md5PasswordEncoder",
    "org.springframework.security.authentication.encoding.ShaPasswordEncoder",
    "org.springframework.security.crypto.password.LdapShaPasswordEncoder",
    "org.springframework.security.crypto.password.Md4PasswordEncoder",
    "org.springframework.security.crypto.password.MessageDigestPasswordEncoder",
    "org.springframework.security.crypto.password.StandardPasswordEncoder",
    "org.springframework.security.crypto.password.NoOpPasswordEncoder",
)

private val WEAK_METHOD_MATCHERS = listOf(
    FunMatcher(qualifier = "org.apache.commons.codec.digest.DigestUtils", name = "getDigest") {
        withArguments(STRING_TYPE)
    },
    FunMatcher(qualifier = "org.apache.commons.codec.digest.DigestUtils") {
        withNames(*ALGORITHM_BY_METHOD_NAME.keys.toTypedArray())
    },
    FunMatcher(qualifier = "org.springframework.util.DigestUtils") {
        withNames("appendMd5DigestAsHex", "md5Digest", "md5DigestAsHex")
    },
    FunMatcher(qualifier = "com.google.common.hash.Hashing") {
        withNames("md5", "sha1")
        withNoArguments()
    },
) + CRYPTO_APIS.map {
    FunMatcher(qualifier = it, name = GET_INSTANCE) {
        withArguments(STRING_TYPE)
        withArguments(ArgumentMatcher(typeName = STRING_TYPE), ArgumentMatcher.ANY)
    }
}

private val DEPRECATED_SPRING_PASSWORD_ENCODER_METHODS = DEPRECATED_SPRING_PASSWORD_ENCODERS.map(::ConstructorMatcher).toList() +
        FunMatcher(qualifier = "org.springframework.security.crypto.password.NoOpPasswordEncoder", name = GET_INSTANCE)

@Rule(key = "S4790")
class DataHashingCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val calleeExpression = expression.calleeExpression ?: return

        if (DEPRECATED_SPRING_PASSWORD_ENCODER_METHODS.any { it.matches(expression) }) {
            kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
        } else if (WEAK_METHOD_MATCHERS.any { it.matches(expression) }) {
            val algorithm = ALGORITHM_BY_METHOD_NAME[calleeExpression.text]
                ?: algorithm(expression.valueArguments.firstOrNull()?.getArgumentExpression())
            algorithm?.let { kotlinFileContext.reportIssue(calleeExpression, MESSAGE) }
        }
    }
}

enum class InsecureAlgorithm {
    MD2, MD4, MD5, MD6, RIPEMD, HAVAL128, SHA1, SHA0, SHA224,
    SHA {
        override fun match(algorithm: String): Boolean {
            // exact match required for SHA, so it doesn't match SHA-512
            return algorithm.equals("sha", ignoreCase = true)
        }
    },
    DSA {
        override fun match(algorithm: String): Boolean {
            // exact match required for DSA, so it doesn't match ECDSA
            return algorithm.equals("dsa", ignoreCase = true)
        }
    };

    open fun match(algorithm: String): Boolean {
        val normalizedName = algorithm.replace("-", "").lowercase()
        return normalizedName.contains(name.lowercase())
    }
}

private fun algorithm(invocationArgument: KtExpression?) =
    invocationArgument?.predictRuntimeStringValue()?.let { algorithmName ->
        InsecureAlgorithm.values().firstOrNull { it.match(algorithmName) }
    }
