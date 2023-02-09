/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ANY
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.ArgumentMatcher
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.GET_INSTANCE
import org.sonarsource.kotlin.api.STRING_TYPE
import org.sonarsource.kotlin.api.predictRuntimeStringValue
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        withArguments(ArgumentMatcher(typeName = STRING_TYPE), ANY)
    }
}

private val DEPRECATED_SPRING_PASSWORD_ENCODER_METHODS = DEPRECATED_SPRING_PASSWORD_ENCODERS.map(::ConstructorMatcher).toList() +
        FunMatcher(qualifier = "org.springframework.security.crypto.password.NoOpPasswordEncoder", name = GET_INSTANCE)

@Rule(key = "S4790")
class DataHashingCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext
        val calleeExpression = expression.calleeExpression ?: return

        if (DEPRECATED_SPRING_PASSWORD_ENCODER_METHODS.any { it.matches(expression, bindingContext) }) {
            kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
        } else if (WEAK_METHOD_MATCHERS.any { it.matches(expression, bindingContext) }) {
            val algorithm = ALGORITHM_BY_METHOD_NAME[calleeExpression.text]
                ?: algorithm(expression.valueArguments.firstOrNull()?.getArgumentExpression(), bindingContext)
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

private fun algorithm(invocationArgument: KtExpression?, bindingContext: BindingContext) =
    invocationArgument?.predictRuntimeStringValue(bindingContext)?.let { algorithmName ->
        InsecureAlgorithm.values().firstOrNull { it.match(algorithmName) }
    }
