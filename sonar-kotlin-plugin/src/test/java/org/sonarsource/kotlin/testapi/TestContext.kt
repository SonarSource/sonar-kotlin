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
package org.sonarsource.kotlin.testapi

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.rule.RuleKey
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
// TODO: testapi should not depend on api module.
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.contains
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import java.util.function.Consumer

internal class TestContext(
    private val verifier: SingleFileVerifier,
    check: AbstractCheck,
    vararg furtherChecks: AbstractCheck,
    override val inputFile: InputFile = DummyInputFile(),
    override val isAndroid: Boolean = false,
) : InputFileContext {
    private val visitor: KtTestChecksVisitor = KtTestChecksVisitor(listOf(check) + furtherChecks)
    override var filteredRules: Map<String, Set<TextRange>> = emptyMap()

    override val sensorContext: SensorContext
        get() = throw NotImplementedError()

    fun scan(root: KotlinTree) {
        visitor.scan(this, root)
    }

    override fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: Message,
        secondaryLocations: List<SecondaryLocation>,
        gap: Double?,
    ) {
        if (textRange != null &&
            filteredRules.getOrDefault(ruleKey.toString(), emptySet()).any { other: TextRange -> textRange in other }
        ) {
            // Issue is filtered by one of the filter.
            return
        }

        val issue = textRange?.let {
            val start = textRange.start()
            val end = textRange.end()
            verifier.reportIssue(message.asStringWithBackticks())
                .onRange(start.line(), start.lineOffset() + 1, end.line(), end.lineOffset())
        } ?: verifier.reportIssue(message.asStringWithBackticks()).onFile()
        issue.withGap(gap)
        secondaryLocations.forEach(Consumer { secondary: SecondaryLocation ->
            issue.addSecondary(
                secondary.textRange.start().line(),
                secondary.textRange.start().lineOffset() + 1,
                secondary.textRange.end().line(),
                secondary.textRange.end().lineOffset(),
                secondary.message
            )
        })
    }

    override fun reportAnalysisParseError(repositoryKey: String, inputFile: InputFile, location: TextPointer?) {
        throw NotImplementedError()
    }

    override fun reportAnalysisError(message: String?, location: TextPointer?) {
        throw NotImplementedError()
    }
}

/**
 * For easier testing, we format messages such that code is wrapped with backticks
 */
private fun Message.asStringWithBackticks(): String {
    var subStringStart = 0
    val formatted = ranges.sortedBy { it.first }
        .flatMap { (first, second) -> sequenceOf(first, second) }
        .fold("") { acc, index ->
            val newAcc = (acc + (text.substring(subStringStart, index))) + '`'
            subStringStart = index
            newAcc
        }
    return formatted + text.substring(subStringStart)
}
