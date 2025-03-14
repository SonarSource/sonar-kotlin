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

import org.jetbrains.kotlin.psi.KtFile
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1451")
class FileHeaderCheck : AbstractCheck() {
    @RuleProperty(
        key = "headerFormat",
        description = "Expected copyright and license header",
        defaultValue = "",
        type = "TEXT")
    var headerFormat = ""

    @RuleProperty(
        key = "isRegularExpression",
        description = "Whether the headerFormat is a regular expression",
        defaultValue = "false")
    var isRegularExpression = false

    private lateinit var searchPattern: Regex
    private lateinit var expectedLines: List<String>

    override fun initialize(ruleKey: RuleKey) {
        super.initialize(ruleKey)
        if (isRegularExpression) {
            try {
                searchPattern = Regex("^$headerFormat")
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "[" + javaClass.simpleName + "] Unable to compile the regular expression: " + headerFormat,
                    e)
            }
        } else {
            expectedLines = headerFormat.lines()
        }
    }

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        if (!hasExpectedHeader(file)) {
            kotlinFileContext.reportIssue(null, "Add or update the header of this file.")
        }
    }

    private fun hasExpectedHeader(file: KtFile): Boolean {
        return if (isRegularExpression) {
            searchPattern.find(file.text) != null
        } else {
            expectedLines == file.text.lineSequence().take(expectedLines.size).toList()
        }
    }

}
