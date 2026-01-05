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

import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1479")
class TooManyCasesCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX = 30
    }

    @RuleProperty(
        key = "maximum",
        description = "Maximum number of branches",
        defaultValue = "" + DEFAULT_MAX)
    var maximum = DEFAULT_MAX

    override fun visitWhenExpression(expression: KtWhenExpression, kotlinFileContext: KotlinFileContext) {
        val actual = expression.entries.size
        if (actual > maximum) {
            kotlinFileContext.reportIssue(
                expression.whenKeyword,
                "Reduce the number of ${expression.whenKeyword.text} branches from $actual to at most $maximum.",
                secondaryLocations = expression.entries
                    .map { SecondaryLocation(kotlinFileContext.textRange(it.arrow!!), null) }
                    .toList(),
            )
        }
    }

}
