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
package org.sonarsource.kotlin.metrics

import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.visiting.Comment
import org.sonarsource.kotlin.api.visiting.CommentAnnotationsAndTokenVisitor
// TODO: refactor later; metrics should not depend on checks!
import org.sonarsource.kotlin.checks.BadClassNameCheck
import org.sonarsource.kotlin.checks.BadFunctionNameCheck
import org.sonarsource.kotlin.checks.DeprecatedCodeUsedCheck
import org.sonarsource.kotlin.checks.TooManyCasesCheck
import org.sonarsource.kotlin.checks.UnusedLocalVariableCheck
import org.sonarsource.kotlin.checks.UselessNullCheckCheck
import org.sonarsource.kotlin.checks.VariableAndParameterNameCheck
import org.sonarsource.kotlin.testapi.DEFAULT_KOTLIN_CLASSPATH
import org.sonarsource.kotlin.testapi.KOTLIN_BASE_DIR
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import org.sonarsource.kotlin.testapi.TestContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

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
        scanFile(
            path,
            true,
            BadClassNameCheck(),
            BadFunctionNameCheck(),
            VariableAndParameterNameCheck(),
            UnusedLocalVariableCheck(),
            TooManyCasesCheck(),
            DeprecatedCodeUsedCheck(),
            UselessNullCheckCheck(),
        )

    private fun scanWithoutSuppression(path: Path) =
        scanFile(
            path,
            false,
            BadClassNameCheck(),
            BadFunctionNameCheck(),
            VariableAndParameterNameCheck(),
            UnusedLocalVariableCheck(),
            TooManyCasesCheck(),
            DeprecatedCodeUsedCheck(),
            UselessNullCheckCheck(),
        )

    private fun scanFile(path: Path, suppress: Boolean, check: AbstractCheck, vararg checks: AbstractCheck): SingleFileVerifier {
        val env = Environment(System.getProperty("java.class.path").split(File.pathSeparatorChar) + DEFAULT_KOTLIN_CLASSPATH, LanguageVersion.LATEST_STABLE)
        val verifier = SingleFileVerifier.create(path, StandardCharsets.UTF_8)
        val testFileContent = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val inputFile = TestInputFileBuilder("moduleKey", "src/org/foo/kotlin.kt")
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
