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
package org.sonarsource.kotlin.verifier

import org.sonar.api.rule.RuleKey
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.slang.api.TextRange
import org.sonarsource.slang.api.Tree
import org.sonarsource.slang.checks.api.SecondaryLocation
import org.sonarsource.slang.plugin.InputFileContext
import java.util.function.Consumer

internal class TestContext(
    private val verifier: SingleFileVerifier,
    check: AbstractCheck,
) : InputFileContext(null, null) {
    private val visitor: KtTestChecksVisitor = KtTestChecksVisitor(check)
    fun scan(root: Tree?) {
        visitor.scan(this, root)
    }

    override fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: String,
        secondaryLocations: List<SecondaryLocation>,
        gap: Double?,
    ) {
        val start = textRange!!.start()
        val end = textRange.end()
        val issue = verifier
            .reportIssue(message)
            .onRange(start.line(), start.lineOffset() + 1, end.line(), end.lineOffset())
            .withGap(gap)
        secondaryLocations.forEach(Consumer { secondary: SecondaryLocation ->
            issue.addSecondary(
                secondary.textRange.start().line(),
                secondary.textRange.start().lineOffset() + 1,
                secondary.textRange.end().line(),
                secondary.textRange.end().lineOffset(),
                secondary.message)
        })
    }

}
