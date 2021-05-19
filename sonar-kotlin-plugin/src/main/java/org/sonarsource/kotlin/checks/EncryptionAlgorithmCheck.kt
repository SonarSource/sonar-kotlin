/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import java.util.Locale
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.predictRuntimeStringValueWithSecondaries
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

private val ALGORITHM_PATTERN = Regex("([^/]+)/([^/]+)/([^/]+)")
private const val MESSAGE = "Use secure mode and padding scheme."

val CIPHER_GET_INSTANCE_MATCHER = FunMatcher {
    type = "javax.crypto.Cipher"
    names = listOf("getInstance")
}

@Rule(key = "S5542")
class EncryptionAlgorithmCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, context: KotlinFileContext) {
        val bindingContext = context.bindingContext
        if (!CIPHER_GET_INSTANCE_MATCHER.matches(expression, bindingContext)) return
        expression.valueArguments.firstOrNull()?.let { argument ->
            argument.getArgumentExpression()!!
                .predictRuntimeStringValueWithSecondaries(bindingContext).let { (algorithm, secondaries) ->
                    if (algorithm.isInsecure()) {
                        val locations = secondaries.map {
                            SecondaryLocation(context.textRange(it), "Transformation definition")
                        }
                        context.reportIssue(argument, MESSAGE, locations)
                    }
                }
        }
    }

    private fun String?.isInsecure(): Boolean {
        if (this == null) return false
        val matcher: MatchResult? = ALGORITHM_PATTERN.matchEntire(this)
        // First element is a full match
        matcher?.groupValues?.let { (_, algorithm, mode, padding) ->
            val isRSA = "RSA".equals(algorithm, ignoreCase = true)
            return if ("ECB".equals(mode, ignoreCase = true) && !isRSA) true
            else if ("CBC".equals(mode, ignoreCase = true)) false
            else isRSA && !padding.toUpperCase(Locale.ROOT).startsWith("OAEP")
        }
        // By default, ECB is used.
            ?: return true
    }
}
