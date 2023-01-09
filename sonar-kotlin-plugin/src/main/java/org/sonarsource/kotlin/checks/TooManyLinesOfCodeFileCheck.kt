/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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

@Rule(key = "S104")
class TooManyLinesOfCodeFileCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX = 1000
    }

    @RuleProperty(
        key = "max",
        description = "Maximum authorized lines of code in a file.",
        defaultValue = "" + DEFAULT_MAX,
    )
    var max: Int = DEFAULT_MAX

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        val numberOfLinesOfCode = file.numberOfLinesOfCode()
        if (numberOfLinesOfCode > max) {
            kotlinFileContext.reportIssue(
                null,
                // TODO add filename to the message
                "File has $numberOfLinesOfCode lines, which is greater than $max authorized. Split it into smaller files.")
        }
    }

}
