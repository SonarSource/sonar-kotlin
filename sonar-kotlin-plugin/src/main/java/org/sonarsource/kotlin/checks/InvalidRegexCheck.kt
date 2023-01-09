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
import org.sonar.check.Rule
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.regex.AbstractRegexCheck
import org.sonarsource.kotlin.api.regex.RegexContext
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE_MULTIPLE_ERRORS = "Fix the syntax errors inside this regex."

@Rule(key = "S5856")
class InvalidRegexCheck : AbstractRegexCheck() {

    override fun visitRegex(
        regex: RegexParseResult,
        regexContext: RegexContext,
        callExpression: KtCallExpression,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val syntaxErrorLocs = regex.syntaxErrors
        val textRangeTracker = regexContext.regexSource.textRangeTracker

        if (syntaxErrorLocs.size == 1) {
            val first = syntaxErrorLocs.first()
            regexContext.reportIssue(first.offendingSyntaxElement, first.message)
        } else if (syntaxErrorLocs.size > 1) {
            val secondaries = syntaxErrorLocs.mapNotNull { syntaxError ->
                kotlinFileContext
                    .mergeTextRanges(textRangeTracker.textRangesBetween(syntaxError.offendingSyntaxElement.range))
                    ?.map { SecondaryLocation(it, syntaxError.message) }
            }.flatten()

            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE_MULTIPLE_ERRORS, secondaries)
        }
    }
}
