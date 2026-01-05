/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.finders.UnicodeUnawareCharClassFinder
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.regex.AbstractRegexCheck
import org.sonarsource.kotlin.api.regex.PATTERN_COMPILE_MATCHER
import org.sonarsource.kotlin.api.regex.RegexContext
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
