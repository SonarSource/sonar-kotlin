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
package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.api.batch.measure.Metric
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContextFactory
import org.sonarsource.kotlin.api.getContent
import org.sonarsource.kotlin.api.linesOfCode
import org.sonarsource.kotlin.visiting.KotlinFileVisitor
import org.sonarsource.slang.plugin.InputFileContext

const val NOSONAR_PREFIX = "NOSONAR"

class MetricVisitor(
    private val fileLinesContextFactory: FileLinesContextFactory,
    private val noSonarFilter: NoSonarFilter,
) : KotlinFileVisitor() {
    private lateinit var ktMetricVisitor: KtMetricVisitor

    override fun visit(kotlinFileContext: KotlinFileContext) {
        ktMetricVisitor = KtMetricVisitor()
        val (ctx, file) = kotlinFileContext
        file.accept(ktMetricVisitor)

        saveMetric(ctx, CoreMetrics.NCLOC, ktMetricVisitor.linesOfCode.size)
        saveMetric(ctx, CoreMetrics.COMMENT_LINES, ktMetricVisitor.commentLines.size)
        saveMetric(ctx, CoreMetrics.FUNCTIONS, ktMetricVisitor.numberOfFunctions)
        saveMetric(ctx, CoreMetrics.CLASSES, ktMetricVisitor.numberOfClasses)
        saveMetric(ctx, CoreMetrics.COMPLEXITY, ktMetricVisitor.complexity)
        saveMetric(ctx, CoreMetrics.STATEMENTS, ktMetricVisitor.statements)
        saveMetric(ctx, CoreMetrics.COGNITIVE_COMPLEXITY, ktMetricVisitor.cognitiveComplexity)

        val fileLinesContext = fileLinesContextFactory.createFor(ctx.inputFile)
        ktMetricVisitor.linesOfCode.forEach { line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1) }
        ktMetricVisitor.executableLines.forEach { line -> fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1) }
        fileLinesContext.save()
        noSonarFilter.noSonarInFile(ctx.inputFile, ktMetricVisitor.nosonarLines)
    }

    fun commentLines() = ktMetricVisitor.commentLines.toSet()

    fun linesOfCode() = ktMetricVisitor.linesOfCode.toSet()

    fun nosonarLines() = ktMetricVisitor.nosonarLines.toSet()

    fun numberOfFunctions() = ktMetricVisitor.numberOfFunctions

    fun numberOfClasses() = ktMetricVisitor.numberOfClasses

    fun cognitiveComplexity() = ktMetricVisitor.cognitiveComplexity

    fun executableLines() = ktMetricVisitor.executableLines
}

private class KtMetricVisitor : KtTreeVisitorVoid() {
    var linesOfCode: Set<Int> = mutableSetOf()
        private set

    var commentLines = mutableSetOf<Int>()
        private set

    var nosonarLines = mutableSetOf<Int>()
        private set

    var executableLines = mutableSetOf<Int>()
        private set

    var numberOfFunctions = 0
        private set

    var numberOfClasses = 0
        private set

    var complexity = 0
        private set

    var statements = 0
        private set

    var cognitiveComplexity = 0
        private set

    override fun visitKtFile(file: KtFile) {
        linesOfCode = file.linesOfCode()

        val complexityVisitor = CyclomaticComplexityVisitor()
        file.accept(complexityVisitor)
        complexity = complexityVisitor.complexityTrees().size

        cognitiveComplexity = org.sonarsource.kotlin.checks.CognitiveComplexity(file).value()

        val statementsVisitor = StatementsVisitor()
        file.accept(statementsVisitor)
        statements = statementsVisitor.statements

        super.visitKtFile(file)
    }

    override fun visitComment(comment: PsiComment) {
        addCommentMetrics(comment, commentLines, nosonarLines)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.hasBody() && function.name != null) {
            numberOfFunctions++
        }
        super.visitNamedFunction(function)
    }

    override fun visitClass(klass: KtClass) {
        numberOfClasses++
        super.visitClass(klass)
    }

    override fun visitBlockExpression(expression: KtBlockExpression) {
        addExecutableLines(expression.statements, expression.containingKtFile.viewProvider.document!!)
        super.visitBlockExpression(expression)
    }

    private fun addExecutableLines(elements: List<KtElement>, document: Document) {
        elements.asSequence()
            .filterNot {
                it is KtPackageDirective
                    || it is KtImportDirective
                    || it is KtClassOrObject
                    || it is KtNamedFunction
                    || it is KtBlockExpression
            }
            .forEach { executableLines.add(document.getLineNumber(it.textRange.startOffset) + 1) }
    }
}

private fun saveMetric(ctx: InputFileContext, metric: Metric<Int>, value: Int) =
    ctx.sensorContext.newMeasure<Int>()
        .on(ctx.inputFile)
        .forMetric(metric)
        .withValue(value)
        .save()

private fun addCommentMetrics(comment: PsiComment, commentLines: MutableSet<Int>, nosonarLines: MutableSet<Int>) {
    val document = comment.containingFile.viewProvider.document!!
    add(comment.textRange, commentLines, document)
    if (isNosonarComment(comment)) {
        add(comment.textRange, nosonarLines, document)
    }
}

private fun add(range: TextRange, lineNumbers: MutableSet<Int>, document: Document) {
    val startLine = document.getLineNumber(range.startOffset) + 1
    val endLine = document.getLineNumber(range.endOffset) + 1
    lineNumbers.addAll(startLine..endLine)
}

fun isNosonarComment(comment: PsiComment) =
    comment.getContent().trim { it <= ' ' }.uppercase().startsWith(NOSONAR_PREFIX)

