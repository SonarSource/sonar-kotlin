/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.api.batch.measure.Metric
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContextFactory
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.checks.getContent
import org.sonarsource.kotlin.api.checks.linesOfCode
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor

const val NOSONAR_PREFIX = "NOSONAR"

class MetricVisitor(
    private val fileLinesContextFactory: FileLinesContextFactory,
    private val noSonarFilter: NoSonarFilter,
    private val telemetryData: TelemetryData, // Some metrics are stored in telemetry
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

        telemetryData.hasAndroidImports = telemetryData.hasAndroidImports || ktMetricVisitor.hasAndroidImports
    }

    fun commentLines() = ktMetricVisitor.commentLines.toSet()

    fun linesOfCode() = ktMetricVisitor.linesOfCode.toSet()

    fun nosonarLines() = ktMetricVisitor.nosonarLines.toSet()

    fun numberOfFunctions() = ktMetricVisitor.numberOfFunctions

    fun numberOfClasses() = ktMetricVisitor.numberOfClasses

    fun cognitiveComplexity() = ktMetricVisitor.cognitiveComplexity

    fun executableLines() = ktMetricVisitor.executableLines

    fun hasAndroidImports() = ktMetricVisitor.hasAndroidImports
}

private class KtMetricVisitor : KtTreeVisitorVoid() {
    private companion object {
        val ANDROID_PACKAGES = setOf(Name.identifier("android"), Name.identifier("androidx"))
    }

    // User metrics

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

    // Telemetry metrics

    var hasAndroidImports = false
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

        // We don't want to count file headers as comment.
        file.children.dropWhile { it is PsiComment || it is PsiWhiteSpace }.forEach { it.accept(this) }

        // Finding all multiline comments this way, as there is no an adequate visit method available in the visitor
        file.containingFile.collectDescendantsOfType<KDoc>().forEach { addCommentMetrics(it, commentLines, nosonarLines) }
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

    override fun visitImportDirective(importDirective: KtImportDirective) {
        hasAndroidImports = hasAndroidImports ||
            ANDROID_PACKAGES.any { importDirective.importPath?.fqName?.startsWith(it) ?: false }
        super.visitImportDirective(importDirective)
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
    if (isNosonarComment(comment)) {
        add(comment.textRange, nosonarLines, document)
    } else {
        add(comment.textRange, commentLines, document)
    }
}

private fun add(range: TextRange, lineNumbers: MutableSet<Int>, document: Document) {
    val startLine = document.getLineNumber(range.startOffset) + 1
    val endLine = document.getLineNumber(range.endOffset) + 1
    lineNumbers.addAll(startLine..endLine)
}

fun isNosonarComment(comment: PsiComment) =
    comment.getContent().trim { it <= ' ' }.uppercase().startsWith(NOSONAR_PREFIX)

