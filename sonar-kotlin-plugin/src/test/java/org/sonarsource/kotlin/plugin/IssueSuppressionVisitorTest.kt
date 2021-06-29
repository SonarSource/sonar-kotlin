package org.sonarsource.kotlin.plugin

import org.junit.jupiter.api.Test
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.checks.BadClassNameCheck
import org.sonarsource.kotlin.checks.BadFunctionNameCheck
import org.sonarsource.kotlin.checks.UnusedLocalVariableCheck
import org.sonarsource.kotlin.checks.VariableAndParameterNameCheck
import org.sonarsource.kotlin.converter.Comment
import org.sonarsource.kotlin.converter.CommentAnnotationsAndTokenVisitor
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.verifier.KotlinVerifier
import org.sonarsource.kotlin.verifier.TestContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class IssueSuppressionVisitorTest {
    @Test
    fun `verify we actually suppress issues on various AST nodes`() {
        val withSuppressionTestFile = KotlinVerifier.KOTLIN_BASE_DIR.resolve("../sample/IssueSuppressionSample.kt")
        val forNoSuppressionTestFile = KotlinVerifier.KOTLIN_BASE_DIR.resolve("../sample/IssueNonSuppressionSample.kt")
        val withoutSuppressionTestFile = KotlinVerifier.KOTLIN_BASE_DIR.resolve("../sample/IssueWithoutSuppressionSample.kt")
        scanWithSuppression(withSuppressionTestFile).assertOneOrMoreIssues()
        scanWithSuppression(withoutSuppressionTestFile).assertOneOrMoreIssues()
        scanWithoutSuppression(forNoSuppressionTestFile).assertOneOrMoreIssues()
    }

    private fun scanWithSuppression(path: Path) =
        scanFile(path, true, BadClassNameCheck(), BadFunctionNameCheck(), VariableAndParameterNameCheck(), UnusedLocalVariableCheck())

    private fun scanWithoutSuppression(path: Path) =
        scanFile(path, false, BadClassNameCheck(), BadFunctionNameCheck(), VariableAndParameterNameCheck(), UnusedLocalVariableCheck())

    private fun scanFile(path: Path, suppress: Boolean, check: AbstractCheck, vararg checks: AbstractCheck): SingleFileVerifier {
        val env = Environment(emptyList())
        val verifier = SingleFileVerifier.create(path, StandardCharsets.UTF_8)
        val testFileContent = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val root = KotlinTree.of(testFileContent, env)

        CommentAnnotationsAndTokenVisitor(root.document).apply { visitElement(root.psiFile) }.allComments
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
