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
package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.checks.BadClassNameCheck
import org.sonarsource.kotlin.checks.BadFunctionNameCheck
import org.sonarsource.kotlin.checks.TooManyCasesCheck
import org.sonarsource.kotlin.checks.UnusedLocalVariableCheck
import org.sonarsource.kotlin.checks.VariableAndParameterNameCheck
import org.sonarsource.kotlin.converter.Comment
import org.sonarsource.kotlin.converter.CommentAnnotationsAndTokenVisitor
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.utils.kotlinTreeOf
import org.sonarsource.kotlin.verifier.KotlinVerifier
import org.sonarsource.kotlin.verifier.TestContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.sonarsource.kotlin.verifier.DEFAULT_KOTLIN_CLASSPATH
import org.sonarsource.kotlin.verifier.KOTLIN_BASE_DIR

class IssueSuppressionVisitorTest {
    @Test
    fun `verify we actually suppress issues on various AST nodes`() {
        val withSuppressionTestFile = KOTLIN_BASE_DIR.resolve("../sample/IssueSuppressionSample.kt")
        val forNoSuppressionTestFile = KOTLIN_BASE_DIR.resolve("../sample/IssueNonSuppressionSample.kt")
        val withoutSuppressionTestFile = KOTLIN_BASE_DIR.resolve("../sample/IssueWithoutSuppressionSample.kt")
        scanWithSuppression(withSuppressionTestFile).assertOneOrMoreIssues()
        scanWithoutSuppression(forNoSuppressionTestFile).assertOneOrMoreIssues()
        scanWithSuppression(withoutSuppressionTestFile).assertOneOrMoreIssues()
    }

    private fun scanWithSuppression(path: Path) =
        scanFile(path, true, BadClassNameCheck(), BadFunctionNameCheck(), VariableAndParameterNameCheck(), UnusedLocalVariableCheck(), TooManyCasesCheck())

    private fun scanWithoutSuppression(path: Path) =
        scanFile(path, false, BadClassNameCheck(), BadFunctionNameCheck(), VariableAndParameterNameCheck(), UnusedLocalVariableCheck(), TooManyCasesCheck())

    private fun scanFile(path: Path, suppress: Boolean, check: AbstractCheck, vararg checks: AbstractCheck): SingleFileVerifier {
        val env = Environment(DEFAULT_KOTLIN_CLASSPATH, LanguageVersion.LATEST_STABLE)
        val verifier = SingleFileVerifier.create(path, StandardCharsets.UTF_8)
        val testFileContent = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val inputFile = TestInputFileBuilder("moduleKey",  "src/org/foo/kotlin.kt")
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(testFileContent)
            .build()

        val root = kotlinTreeOf(testFileContent, env, inputFile)

        CommentAnnotationsAndTokenVisitor(root.document, inputFile)
            .apply { visitElement(root.psiFile) }.allComments
            .forEach { comment: Comment ->
                val start = comment.range.start()
                verifier.addComment(start.line(), start.lineOffset() + 1, comment.text, 2, 0)
            }
        val ctx = TestContext(verifier, check, *checks)

        if (suppress) IssueSuppressionVisitor().scan(ctx, root)

        ctx.scan(root)
        return verifier
    }
}
