/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValueWithSecondaries
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val ALGORITHM_PATTERN = Regex("([^/]+)/([^/]+)/([^/]+)")

val CIPHER_GET_INSTANCE_MATCHER = FunMatcher {
    qualifier = "javax.crypto.Cipher"
    name = "getInstance"
}

@Rule(key = "S5542")
class EncryptionAlgorithmCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(CIPHER_GET_INSTANCE_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        callExpression.valueArguments.firstOrNull()?.let { argument ->
            argument.getArgumentExpression()!!
                .predictRuntimeStringValueWithSecondaries().let { (algorithm, secondaries) ->
                    algorithm?.getInsecureAlgorithmMessage()?.let { errorMessage ->
                        val locations = secondaries.map { secondaryLocation ->
                            SecondaryLocation(kotlinFileContext.textRange(secondaryLocation), "Transformation definition")
                        }
                        kotlinFileContext.reportIssue(argument, errorMessage, locations)
                    }
                }
        }
    }
}

private fun String.getInsecureAlgorithmMessage(): String? {
    val matcher: MatchResult? = ALGORITHM_PATTERN.matchEntire(this)
    // First element is a full match
    matcher?.groupValues?.let { (_, algorithm, mode, padding) ->
        val isRSA = "RSA".equals(algorithm, ignoreCase = true)
        return when {
            "ECB".equals(mode, ignoreCase = true) && !isRSA -> "Use a secure cipher mode."
            "CBC".equals(mode, ignoreCase = true) && !"NoPadding".equals(padding, ignoreCase = true) -> "Use another cipher mode or disable padding."
            isRSA && !padding.uppercase().startsWith("OAEP") -> "Use a secure padding scheme."
            else -> null
        }
    }
    // By default, ECB is used.
        ?: return "Use secure mode and padding scheme."
}
