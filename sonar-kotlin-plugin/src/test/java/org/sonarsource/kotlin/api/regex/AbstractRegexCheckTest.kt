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
package org.sonarsource.kotlin.api.regex

import org.junit.jupiter.api.Test
import org.sonarsource.analyzer.commons.regex.RegexIssueLocation
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassTree
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor
import org.sonarsource.kotlin.verifier.KotlinVerifier

class AbstractRegexCheckTest {
    @Test
    fun `test dummy regex check`() {
        KotlinVerifier(ReportEveryRegexDummyCheck()) {
            fileName = "DummyRegexCheckSample.kt"
        }.verify()

        KotlinVerifier(ReportEveryRegexDummyCheck2()) {
            fileName = "DummyRegexCheckSample.kt"
        }.verify()
    }

    @Test
    fun `test character class regex check`() {
        KotlinVerifier(ReportCharacterClassRegexDummyCheck()) {
            fileName = "ReportCharacterClassRegexDummyCheckSample.kt"
        }.verify()
    }
}

private class ReportEveryRegexDummyCheck : AbstractRegexCheck() {
    override fun visitRegex(regex: RegexParseResult, regexContext: RegexContext) {
        regexContext.reportIssue(regex.result, "Flags: ${regex.result.activeFlags().mask}")
    }
}

private class ReportEveryRegexDummyCheck2 : AbstractRegexCheck() {
    private var counter = 0

    override fun visitRegex(regex: RegexParseResult, regexContext: RegexContext) {
        regexContext.reportIssue(
            regex.result,
            "Flags: ${regex.result.activeFlags().mask}",
            if (counter == 0) null else counter,
            emptyList()
        )
        counter++
    }

}

private class ReportCharacterClassRegexDummyCheck : AbstractRegexCheck() {
    override fun visitRegex(regex: RegexParseResult, regexContext: RegexContext) {
        val trees = mutableListOf<CharacterClassTree>()
        regex.result.accept(object : RegexBaseVisitor() {
            override fun visitCharacterClass(tree: CharacterClassTree) {
                trees.add(tree)
            }
        })
        regexContext.reportIssue(trees[0], "Character class found", trees.drop(1).map { RegexIssueLocation(it, "+1") })
    }
}
