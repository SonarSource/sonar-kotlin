/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
        if (!name.matches(formatRegex)) {
            kotlinFileContext.reportIssue(
                function.nameIdentifier!!,
                """Rename function "$name" to match the regular expression $format"""
            )
        }
    }

}
