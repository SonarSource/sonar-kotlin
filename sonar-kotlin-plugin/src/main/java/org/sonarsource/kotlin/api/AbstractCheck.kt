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
package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
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
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getFunctionResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation
import java.util.BitSet
import org.sonarsource.slang.api.TextRange as SonarTextRange


abstract class AbstractCheck : KotlinCheck, KtVisitor<Unit, KotlinFileContext>() {

    companion object {
        private const val THROWS_FQN = "kotlin.jvm.Throws"
    }

    lateinit var ruleKey: RuleKey
        private set

    override fun initialize(ruleKey: RuleKey) {
        this.ruleKey = ruleKey
    }

    /**
     * @param textRange `null` when on file
     */
    internal fun KotlinFileContext.reportIssue(
        textRange: SonarTextRange? = null,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = inputFileContext.reportIssue(ruleKey, textRange, message, secondaryLocations, gap)

    internal fun KotlinFileContext.locationListOf(vararg nodesForSecondaryLocations: Pair<PsiElement, String>) =
        ktFile.viewProvider.document?.let { document ->
            nodesForSecondaryLocations.map { (psiElement, msg) ->
                SecondaryLocation(KotlinTextRanges.textRange(document, psiElement), msg)
            }
        } ?: emptyList()

    internal fun KotlinFileContext.reportIssue(
        psiElement: PsiElement,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = ktFile.viewProvider.document?.let { document ->
        reportIssue(KotlinTextRanges.textRange(document, psiElement), message, secondaryLocations, gap)
    }

    internal fun KtParameter.typeAsString(bindingContext: BindingContext) =
        bindingContext.get(BindingContext.VALUE_PARAMETER, this)?.type.toString()

    internal fun KtExpression.throwsException(bindingContext: BindingContext) =
        when (this) {
            is KtThrowExpression -> true
            is KtDotQualifiedExpression ->
                selectorExpression?.hasAnnotation(THROWS_FQN, bindingContext)
            is KtCallExpression ->
                hasAnnotation(THROWS_FQN, bindingContext)
            else -> false
        } ?: false

    internal fun KtExpression.hasAnnotation(annotation: String, bindingContext: BindingContext) =
        getFunctionResolvedCallWithAssert(bindingContext).resultingDescriptor.annotations.hasAnnotation(FqName(annotation))

    internal fun KtNamedFunction.listStatements(): List<KtExpression> =
        bodyBlockExpression?.statements ?: (bodyExpression?.let { listOf(it) } ?: emptyList())

    internal fun KtCallExpression.getResolvedCall(bindingContext: BindingContext) =
        getCall(bindingContext)?.let { bindingContext.get(RESOLVED_CALL, it) }

    internal fun ClassDescriptor?.getAllSuperTypesInterfaces() =
        this?.let { getAllSuperTypesInterfaces(getSuperInterfaces() + superClassAsList()) } ?: emptyList()

    private fun getAllSuperTypesInterfaces(classes: List<ClassDescriptor>): List<ClassDescriptor> =
        classes + classes.flatMap { getAllSuperTypesInterfaces(it.getSuperInterfaces() + it.superClassAsList()) }

    private fun ClassDescriptor.superClassAsList(): List<ClassDescriptor> =
        getSuperClassNotAny()?.let { listOf(it) } ?: emptyList()

    internal fun KtExpression.skipParentheses(): KtExpression {
        var expr = this
        while (expr is KtParenthesizedExpression) {
            expr = expr.expression!!
        }
        return expr
    }

    internal fun PsiElement.hasComment(): Boolean {
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

    /**
     * Replacement for [org.sonarsource.slang.impl.TreeMetaDataProvider.TreeMetaDataImpl.computeLinesOfCode]
     */
    internal fun PsiElement.numberOfLinesOfCode(): Int {
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

    internal fun KotlinFileContext.textRange(element: PsiElement) =
        KotlinTextRanges.textRange(ktFile.viewProvider.document!!, element)

    fun KtStringTemplateExpression.asConstant() = entries.joinToString { it.text }
}
