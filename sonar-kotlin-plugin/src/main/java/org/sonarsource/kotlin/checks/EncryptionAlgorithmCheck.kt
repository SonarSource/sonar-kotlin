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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.predictRuntimeStringValueWithSecondaries
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext
        callExpression.valueArguments.firstOrNull()?.let { argument ->
            argument.getArgumentExpression()!!
                .predictRuntimeStringValueWithSecondaries(bindingContext).let { (algorithm, secondaries) ->
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
        return if ("ECB".equals(mode, ignoreCase = true) && !isRSA)
            "Use a secure cipher mode."
        else if ("CBC".equals(mode, ignoreCase = true) && !"NoPadding".equals(padding, ignoreCase = true))
            "Use another cipher mode or disable padding."
        else if (isRSA && !padding.uppercase().startsWith("OAEP"))
            "Use a secure padding scheme."
        else
            null
    }
    // By default, ECB is used.
        ?: return "Use secure mode and padding scheme."
}
