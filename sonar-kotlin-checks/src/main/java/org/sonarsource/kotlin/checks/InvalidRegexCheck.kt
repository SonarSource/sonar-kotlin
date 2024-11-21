/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.regex.AbstractRegexCheck
import org.sonarsource.kotlin.api.regex.RegexContext
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
