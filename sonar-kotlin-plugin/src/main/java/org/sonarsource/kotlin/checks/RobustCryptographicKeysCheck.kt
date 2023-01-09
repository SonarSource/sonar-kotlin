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
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.ArgumentMatcher
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.INT_TYPE
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.api.predictRuntimeIntValue
import org.sonarsource.kotlin.api.predictRuntimeStringValue
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val ASYMMETRIC_INITIALIZE_MATCHER = FunMatcher {
    qualifier = "java.security.KeyPairGenerator"
    withNames("initialize")
    withArguments(INT_TYPE)
    withArguments(ArgumentMatcher(INT_TYPE), ArgumentMatcher.ANY)
}

private val ASYMMETRIC_GENERATOR_GET_INSTANCE_MATCHER = FunMatcher {
    qualifier = "java.security.KeyPairGenerator"
    withNames("getInstance")
    dynamic = false
}
private val ASYMMETRIC_ALGORITHMS = setOf("dsa", "rsa", "dh", "diffiehellman")
private const val ASYMMETRIC_MIN_KEY_SIZE = 2048

private val SYMMETRIC_INIT_MATCHER = FunMatcher {
    qualifier = "javax.crypto.KeyGenerator"
    withNames("init")
    withArguments(INT_TYPE)
    withArguments(ArgumentMatcher(INT_TYPE), ArgumentMatcher.ANY)
}

private val SYMMETRIC_GENERATOR_GET_INSTANCE_MATCHER = FunMatcher {
    qualifier = "javax.crypto.KeyGenerator"
    withNames("getInstance")
    dynamic = false
}
private val SYMMETRIC_ALGORITHMS = setOf("aes")
private const val SYMMETRIC_MIN_KEY_SIZE = 128

private val EC_GEN_PARAMETER_SPEC_MATCHER = ConstructorMatcher("java.security.spec.ECGenParameterSpec")

private val INSECURE_EC_SPECS = setOf(
    "secp112r1", "secp112r2", "secp128r1", "secp128r2", "secp160k1", "secp160r1", "secp160r2", "secp192k1", "secp192r1", "prime192v2",
    "prime192v3", "sect113r1", "sect113r2", "sect131r1", "sect131r2", "sect163k1", "sect163r1", "sect163r2", "sect193r1", "sect193r2",
    "c2tnb191v1", "c2tnb191v2", "c2tnb191v3",
)
private const val EC_MIN_KEY_SIZE = 224

@Rule(key = "S4426")
class RobustCryptographicKeysCheck : AbstractCheck() {

    override fun visitCallExpression(callExpr: KtCallExpression, context: KotlinFileContext) {
        callExpr.getResolvedCall(context.bindingContext)?.let { resolvedCall ->
            when {
                resolvedCall matches ASYMMETRIC_INITIALIZE_MATCHER -> handleKeyGeneratorAndKeyPairGenerator(
                    callExpr,
                    resolvedCall,
                    ASYMMETRIC_MIN_KEY_SIZE,
                    ASYMMETRIC_ALGORITHMS,
                    ASYMMETRIC_GENERATOR_GET_INSTANCE_MATCHER,
                    context
                )
                resolvedCall matches SYMMETRIC_INIT_MATCHER -> handleKeyGeneratorAndKeyPairGenerator(
                    callExpr,
                    resolvedCall,
                    SYMMETRIC_MIN_KEY_SIZE,
                    SYMMETRIC_ALGORITHMS,
                    SYMMETRIC_GENERATOR_GET_INSTANCE_MATCHER,
                    context
                )
                resolvedCall matches EC_GEN_PARAMETER_SPEC_MATCHER -> handleECGenParameterSpec(callExpr, context)
            }
        }
    }

    private fun handleECGenParameterSpec(callExpr: KtCallExpression, context: KotlinFileContext) {
        callExpr.valueArguments[0].getArgumentExpression()?.let { specArgExpr ->
            if (specArgExpr.predictRuntimeStringValue(context.bindingContext)?.lowercase() in INSECURE_EC_SPECS) {
                context.reportIssue(specArgExpr, msg(EC_MIN_KEY_SIZE, "EC"))
            }
        }
    }

    private fun handleKeyGeneratorAndKeyPairGenerator(
        callExpr: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        minKeySize: Int,
        unsafeAlgorithms: Collection<String>,
        getInstanceMatcher: FunMatcherImpl,
        context: KotlinFileContext,
    ) {
        val bindingContext = context.bindingContext

        val keySizeExpression = resolvedCall.getFirstArgumentExpression() ?: return
        val keySize = keySizeExpression.predictRuntimeIntValue(bindingContext)
        if (keySize != null && keySize < minKeySize) {

            val getInstanceCall = callExpr.predictReceiverExpression(bindingContext, resolvedCall)?.getResolvedCall(bindingContext)
            if (getInstanceMatcher.matches(getInstanceCall)) {

                val algoExpr = getInstanceCall?.getFirstArgumentExpression()
                algoExpr?.predictRuntimeStringValue(bindingContext)?.let { algo ->

                    if (algo.lowercase() in unsafeAlgorithms) {
                        context.reportIssue(
                            keySizeExpression,
                            msg(minKeySize, algo),
                            secondaryLocations = context.locationListOf(algoExpr to "Using $algo cipher algorithm")
                        )
                    }
                }
            }
        }
    }
}

private fun msg(minKeySize: Int, algorithm: String) = "Use a key length of at least $minKeySize bits for $algorithm cipher algorithm."
