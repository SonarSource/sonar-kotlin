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
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.impl.TextPointerImpl
import org.sonarsource.slang.impl.TextRangeImpl

/**
 * Replacement for [org.sonarsource.slang.checks.TooLongLineCheck]
 */
@Rule(key = "S103")
class TooLongLineCheck : AbstractCheck() {

    companion object {
        const val DEFAULT_MAXIMUM_LINE_LENGTH = 200
    }

    @RuleProperty(
        key = "maximumLineLength",
        description = "The maximum authorized line length.",
        defaultValue = "" + DEFAULT_MAXIMUM_LINE_LENGTH,
    )
    var maximumLineLength = DEFAULT_MAXIMUM_LINE_LENGTH

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        file.text.splitToSequence("\n", "\r\n", "\r").forEachIndexed { lineNumber, line ->
            if (line.length > maximumLineLength) {
                kotlinFileContext.reportIssue(
                    getLineRange(lineNumber, line),
                    "Split this ${line.length} characters long line (which is greater than $maximumLineLength authorized).",
                )
            }
        }
    }

    private fun getLineRange(lineNumber: Int, line: String) = TextRangeImpl(
        TextPointerImpl(lineNumber + 1, 0),
        TextPointerImpl(lineNumber + 1, line.length),
    )
}
