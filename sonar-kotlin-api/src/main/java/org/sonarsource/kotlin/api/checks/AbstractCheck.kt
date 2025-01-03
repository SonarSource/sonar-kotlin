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
package org.sonarsource.kotlin.api.checks

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getFunctionResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textPointerAtOffset
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

import java.util.BitSet

abstract class AbstractCheck : KotlinCheck, KtVisitor<Unit, KotlinFileContext>() {

    lateinit var ruleKey: RuleKey
        private set

    override fun initialize(ruleKey: RuleKey) {
        this.ruleKey = ruleKey
    }

    /**
     * @param textRange `null` when on file
     */
    fun KotlinFileContext.reportIssue(
        textRange: TextRange? = null,
        message: Message,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = inputFileContext.reportIssue(ruleKey, textRange, message, secondaryLocations, gap)

    fun KotlinFileContext.reportIssue(
        textRange: TextRange? = null,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = reportIssue(textRange, Message(message), secondaryLocations, gap)

    fun KotlinFileContext.locationListOf(vararg nodesForSecondaryLocations: Pair<PsiElement, String>) =
        nodesForSecondaryLocations.map { (psiElement, msg) ->
            SecondaryLocation(textRange(psiElement), msg)
        }

    fun KotlinFileContext.reportIssue(
        psiElement: PsiElement,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
        message: Message.() -> Unit
    ) = reportIssue(textRange(psiElement), Message().apply(message), secondaryLocations, gap)

    fun KotlinFileContext.reportIssue(
        psiElement: PsiElement,
        message: Message,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = reportIssue(textRange(psiElement), message, secondaryLocations, gap)

    fun KotlinFileContext.reportIssue(
        psiElement: PsiElement,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = reportIssue(textRange(psiElement), message, secondaryLocations, gap)

    fun KotlinFileContext.isInAndroid() = inputFileContext.isAndroid

    fun KtParameter.typeAsString(bindingContext: BindingContext) =
        bindingContext[BindingContext.VALUE_PARAMETER, this]?.type.toString()

    fun KtExpression.throwsException(bindingContext: BindingContext) =
        when (this) {
            is KtThrowExpression -> true
            is KtDotQualifiedExpression ->
                selectorExpression?.hasAnnotation(THROWS_FQN, bindingContext)
            is KtCallExpression ->
                hasAnnotation(THROWS_FQN, bindingContext)
            else -> false
        } ?: false

    fun KtExpression.hasAnnotation(annotation: String, bindingContext: BindingContext) =
        getFunctionResolvedCallWithAssert(bindingContext).resultingDescriptor.annotations.hasAnnotation(FqName(annotation))

    fun KtNamedFunction.listStatements(): List<KtExpression> =
        bodyBlockExpression?.statements ?: (bodyExpression?.let { listOf(it) } ?: emptyList())

    private fun getAllSuperTypesInterfaces(classes: List<ClassDescriptor>): List<ClassDescriptor> =
        classes + classes.flatMap { getAllSuperTypesInterfaces(it.getSuperInterfaces() + it.superClassAsList()) }

    private fun ClassDescriptor.superClassAsList(): List<ClassDescriptor> =
        getSuperClassNotAny()?.let { listOf(it) } ?: emptyList()

    fun KtExpression.skipParentheses(): KtExpression {
        var expr = this
        while (expr is KtParenthesizedExpression) {
            expr = expr.expression!!
        }
        return expr
    }

    fun PsiElement?.skipParentParentheses(): PsiElement? {
        var expr = this
        while (expr is KtParenthesizedExpression) {
            expr = expr.parent
        }
        return expr
    }

    fun PsiElement.hasComment(): Boolean {
        var result = false
        this.accept(object : KtTreeVisitorVoid() {
            /** Note [visitComment] not called for [org.jetbrains.kotlin.kdoc.psi.api.KDoc] */
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is PsiComment) {
                    result = true
                }
            }
        })
        return result
    }

    fun PsiElement.numberOfLinesOfCode(): Int {
        val lines = BitSet()
        val document = this.containingFile.viewProvider.document!!
        this.accept(object : KtTreeVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is LeafPsiElement && element !is PsiWhiteSpace && element !is PsiComment) {
                    lines.set(document.getLineNumber(element.textRange.startOffset))
                }
            }
        })
        return lines.cardinality()
    }

    fun KtStringTemplateExpression.asConstant() = entries.joinToString { it.text }

    fun KotlinFileContext.mergeTextRanges(ranges: Iterable<TextRange>) = ktFile.viewProvider.document!!.let { doc ->
        ranges.map {
            MutableOffsetRange(
                doc.getLineStartOffset(it.start().line() - 1) + it.start().lineOffset(),
                doc.getLineStartOffset(it.end().line() - 1) + it.end().lineOffset()
            )
        }.fold(mutableListOf<MutableOffsetRange>()) { acc, curOffsets ->
            acc.apply {
                lastOrNull()?.takeIf { prevOffsets ->
                    // Does current range overlap with previous one?
                    curOffsets.end >= prevOffsets.start && curOffsets.start <= prevOffsets.end
                }?.let { prevOffsets ->
                    // This range overlaps with the previous one => merge
                    prevOffsets.end = curOffsets.end
                }
                    ?: add(curOffsets) // This range does not overlap with the previous one (or there is no previous one) => add as separate range
            }
        }.map { (startOffset, endOffset) ->
            with(inputFileContext.inputFile) {
                newRange(textPointerAtOffset(doc, startOffset), textPointerAtOffset(doc, endOffset))
            }
        }.takeIf {
            it.isNotEmpty()
        }
    }

    private data class MutableOffsetRange(var start: Int, var end: Int)
}
