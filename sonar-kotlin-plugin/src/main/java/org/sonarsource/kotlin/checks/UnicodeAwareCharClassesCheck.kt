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
import org.sonarsource.analyzer.commons.regex.finders.UnicodeUnawareCharClassFinder
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.regex.AbstractRegexCheck
import org.sonarsource.kotlin.api.regex.PATTERN_COMPILE_MATCHER
import org.sonarsource.kotlin.api.regex.RegexContext
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S5867")
class UnicodeAwareCharClassesCheck : AbstractRegexCheck() {
    override fun visitRegex(
        regex: RegexParseResult,
        regexContext: RegexContext,
        callExpression: KtCallExpression,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        UnicodeUnawareCharClassFinder(regexContext::reportIssue) { msg, _, secondaries ->
            kotlinFileContext.reportIssue(
                callExpression.calleeExpression!!,
                msg.replace("\"u\"", if (matchedFun == PATTERN_COMPILE_MATCHER) "\"UNICODE_CHARACTER_CLASS\"" else "\"U\""),
                secondaries.flatMap { it.toSecondaries(regexContext.regexSource.textRangeTracker, kotlinFileContext) },
            )
        }.visit(regex)
    }
}
