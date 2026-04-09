/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val TEST_ANNOTATION_NAMES = setOf("Test", "ParameterizedTest", "RepeatedTest")

@Rule(key = "S100")
class BadFunctionNameCheck : AbstractCheck() {

    companion object {
        const val DEFAULT_FORMAT = "^[a-zA-Z][a-zA-Z0-9]*$"
    }

    @RuleProperty(
        key = "format",
        description = "Regular expression used to check the function names against.",
        defaultValue = DEFAULT_FORMAT,
    )
    var format: String = DEFAULT_FORMAT

    private lateinit var formatRegex: Regex

    override fun initialize(ruleKey: RuleKey) {
        super.initialize(ruleKey)
        formatRegex = Regex(format)
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val name = function.name ?: /* in case of anonymous functions */ return
        if (!name.matches(formatRegex) && !isBacktickedTestFunction(function)) {
            kotlinFileContext.reportIssue(
                function.nameIdentifier!!,
                """Rename function "$name" to match the regular expression $format"""
            )
        }
    }

    private fun isBacktickedTestFunction(function: KtNamedFunction): Boolean {
        val nameIdentifierText = function.nameIdentifier?.text ?: return false
        if (!nameIdentifierText.startsWith('`')) return false

        return hasTestAnnotation(function)
    }

    private fun hasTestAnnotation(function: KtNamedFunction): Boolean =
        function.annotationEntries.any { TEST_ANNOTATION_NAMES.contains(it.shortName?.asString()) }

}
