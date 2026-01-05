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

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S101")
class BadClassNameCheck : AbstractCheck() {
    companion object {
        private const val DEFAULT_FORMAT = "^[A-Z][a-zA-Z0-9]*$"
    }

    @RuleProperty(
        key = "format",
        description = "Regular expression used to check the class names against.",
        defaultValue = DEFAULT_FORMAT)
    var format = DEFAULT_FORMAT

    private lateinit var formatRegex: Regex

    override fun initialize(ruleKey: RuleKey) {
        formatRegex = Regex(format)
        super.initialize(ruleKey)
    }

    override fun visitClass(ktClass: KtClass, context: KotlinFileContext) {
        val name = ktClass.name!!
        if (ktClass !is KtEnumEntry && !name.matches(formatRegex)) {
            context.reportIssue(
                ktClass.nameIdentifier!!,
                """Rename class "$name" to match the regular expression $format""",
            )
        }
    }
}
