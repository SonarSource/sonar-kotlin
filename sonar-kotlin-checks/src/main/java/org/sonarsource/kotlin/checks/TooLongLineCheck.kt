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

import org.jetbrains.kotlin.psi.KtFile
import org.sonar.api.batch.fs.InputFile
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
                    getLineRange(kotlinFileContext.inputFileContext.inputFile, lineNumber, line),
                    "Split this ${line.length} characters long line (which is greater than $maximumLineLength authorized).",
                )
            }
        }
    }

    private fun getLineRange(inputFile: InputFile, lineNumber: Int, line: String) = inputFile.newRange(
        inputFile.newPointer(lineNumber + 1, 0),
        inputFile.newPointer(lineNumber + 1, line.length)
    )
}
