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

import org.jetbrains.kotlin.psi.KtWhenEntry
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.MatchCaseTooBigCheck]
 */
@Rule(key = "S1151")
class MatchCaseTooBigCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX = 15
    }

    @RuleProperty(
        key = "max",
        description = "Maximum number of lines",
        defaultValue = "" + DEFAULT_MAX,
    )
    var max: Int = DEFAULT_MAX

    override fun visitWhenEntry(whenEntry: KtWhenEntry, kotlinFileContext: KotlinFileContext) {
        val numberOfLinesOfCode = whenEntry.numberOfLinesOfCode()
        if (numberOfLinesOfCode > max) {
            kotlinFileContext.reportIssue(
                whenEntry,
                "Reduce this case clause number of lines from $numberOfLinesOfCode to at most $max, for example by extracting code into methods.")
        }
    }

}
