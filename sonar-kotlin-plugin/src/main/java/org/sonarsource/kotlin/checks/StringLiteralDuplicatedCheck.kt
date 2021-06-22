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

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.asString
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.StringLiteralDuplicatedCheck]
 */
@Rule(key = "S1192")
class StringLiteralDuplicatedCheck : AbstractCheck() {

    companion object {
        private const val DEFAULT_THRESHOLD = 3
        private const val MINIMAL_LITERAL_LENGTH = 5
        private val NO_SEPARATOR_REGEXP = Regex("\\w++")
    }

    @RuleProperty(key = "threshold",
        description = "Number of times a literal must be duplicated to trigger an issue",
        defaultValue = "" + DEFAULT_THRESHOLD)
    var threshold = DEFAULT_THRESHOLD

    private fun check(
        context: KotlinFileContext,
        occurrencesMap: Map<String, List<KtStringTemplateExpression>>,
    ) {
        for ((_, occurrences) in occurrencesMap) {
            val size = occurrences.size
            if (size >= threshold) {
                val first = occurrences[0]
                context.reportIssue(
                    first,
                    "Define a constant instead of duplicating this literal \"${first.asString()}\" $size times.",
                    secondaryLocations = occurrences.asSequence()
                        .drop(1)
                        .map { SecondaryLocation(context.textRange(it), "Duplication") }
                        .toList(),
                    gap = size - 1.0,
                )
            }
        }
    }

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        val occurrences = file.collectDescendantsOfType<KtStringTemplateExpression> { !it.hasInterpolation() }
            .map { it to it.asString() }
            .filter { (_, text) -> text.length > MINIMAL_LITERAL_LENGTH && !NO_SEPARATOR_REGEXP.matches(text) }
            .groupBy({ (_, text) -> text }) { it.first }
        check(context, occurrences)
    }
}
