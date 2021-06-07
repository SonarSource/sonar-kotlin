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

import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.TooManyCasesCheck]
 */
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
            val document = expression.containingKtFile.viewProvider.document!!
            kotlinFileContext.reportIssue(
                expression.whenKeyword,
                "Reduce the number of ${expression.whenKeyword.text} branches from $actual to at most $maximum.",
                secondaryLocations = expression.entries
                    .map { SecondaryLocation(KotlinTextRanges.textRange(document, it.arrow!!), null) }
                    .toList(),
            )
        }
    }

}
