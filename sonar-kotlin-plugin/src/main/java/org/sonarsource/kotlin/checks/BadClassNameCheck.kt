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

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.BadClassNameCheck]
 */
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
                "Rename class \"$name\" to match the regular expression $format.",
            )
        }
    }
}
