/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.KotlinType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.determineTypeAsString
import org.sonarsource.kotlin.api.checks.isAbstract
import org.sonarsource.kotlin.api.checks.isActual
import org.sonarsource.kotlin.api.checks.isExpect
import org.sonarsource.kotlin.api.checks.isOpen
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only("easy?")
@Rule(key = "S6524")
class CollectionShouldBeImmutableCheck : AbstractCheck() {
    private val mutableCollections =
        setOf(
            "kotlin.collections.MutableList",
            "kotlin.collections.MutableSet",
            "kotlin.collections.MutableMap",
            "kotlin.collections.MutableCollection"
        )

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {

        file
            .collectDescendantsOfType<KtNamedFunction>(::notAnInterface) { true }
            .forEach { reportNamedFunction(it, context) }
    }

    private fun notAnInterface(element: PsiElement): Boolean {
        return if (element is KtClass) {
            !element.isInterface()
        } else {
            true
        }
    }

    private fun reportNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        if (function.isAbstract() || function.overrides() || function.isOpen() || function.isExpect() || function.isActual()) return

        val bindingContext = context.bindingContext

        val referencesToMutableCollections = collectReferenceToMutatedCollections(function, bindingContext)
        val mutatedNames = referencesToMutableCollections.map { it.getReferencedName() }.toSet()
        val mutatedDeclarations =
            referencesToMutableCollections.mapNotNull { it.getResolvedCall(bindingContext)?.resultingDescriptor?.original }.toSet()

        reportPropertiesAndParameters(function, mutatedDeclarations, bindingContext, context)

        if (function.receiverTypeReference?.determineTypeAsString(bindingContext) in mutableCollections) {
            reportForExtensionFunction(function, bindingContext, mutatedNames, context)
        }
    }

    private fun reportPropertiesAndParameters(
        function: KtNamedFunction,
        mutatedDeclarations: Set<CallableDescriptor>,
        bindingContext: BindingContext,
        context: KotlinFileContext,
    ) {
        function.valueParameters
            .filter { it.isMutableCollection(bindingContext) }
            .filter { it.isNotReferenced(mutatedDeclarations, bindingContext) }
            .forEach { context.reportIssue(it, "Make this collection immutable.") }
    }

    private fun KtNamedDeclaration.isMutableCollection(bindingContext: BindingContext): Boolean =
        this.determineTypeAsString(bindingContext) in mutableCollections

    private fun KtNamedDeclaration.isNotReferenced(
        mutatedDeclarations: Set<CallableDescriptor>,
        bindingContext: BindingContext,
    ): Boolean {
        val descriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, this]
        //descriptor is never null because we need the descriptor to know if we will consider the declaration
        //no need to return false if descriptor is null
        return !mutatedDeclarations.contains(descriptor)
    }

    private fun reportForExtensionFunction(
        function: KtNamedFunction,
        bindingContext: BindingContext,
        mutatedNames: Set<String>,
        context: KotlinFileContext,
    ) {
        val functionsCalledOnThis = function
            .collectDescendantsOfType<KtCallExpression> { it.noReceiver() }

        if (functionsCalledOnThis.none { it.mutatesCollection(bindingContext) } && "this" !in mutatedNames) {
            context.reportIssue(function.receiverTypeReference!!.navigationElement, "Make this collection immutable.")
        }
    }

    private fun KtCallExpression.noReceiver(): Boolean {
        return this.parent !is KtQualifiedExpression
    }

    private fun collectReferenceToMutatedCollections(
        function: KtNamedFunction,
        bindingContext: BindingContext,
    ): List<KtNameReferenceExpression> {
        val mutablePropertiesReceiver: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtQualifiedExpression> { it.mutatesCollection(bindingContext) }
            .flatMap { findReferenceExpressions(it.receiverExpression) }

        val mutablePropertiesInBinaryExpression: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtBinaryExpression> { it.mutatesCollection(bindingContext) }
            .flatMap { findReferenceExpressions(it.left!!) }

        val mutablePropertiesInFunctionCalls: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtCallExpression>()
            .flatMap { call ->
                val functionDescriptor = call.getResolvedCall(bindingContext)?.resultingDescriptor
                val parameterTypes = functionDescriptor?.valueParameters
                if (parameterTypes != null) {
                    val fullyQualifiedTypes = parameterTypes.map {
                        if (it.type.constructor.declarationDescriptor == null) null
                        else it.type.getKotlinTypeFqName(false)
                    }
                    call.valueArguments.zip(fullyQualifiedTypes).filter { it.second in mutableCollections }.map { it.first }
                } else {
                    call.valueArguments
                }
            }
            .flatMap { findReferenceExpressions(it.getArgumentExpression()!!) }

        return mutablePropertiesReceiver + mutablePropertiesInBinaryExpression + mutablePropertiesInFunctionCalls
    }

    private fun findReferenceExpressions(
        expression: KtExpression,
    ): List<KtNameReferenceExpression> {
        return expression.collectDescendantsOfType<KtNameReferenceExpression>(::considerElement) { true }
    }

    private fun considerElement(element: PsiElement): Boolean {
        return when (element) {
            is KtCallExpression -> false
            is KtDotQualifiedExpression -> false
            else -> true
        }
    }

    private fun KtQualifiedExpression.mutatesCollection(bindingContext: BindingContext): Boolean {
        return when(val selector = this.selectorExpression){
            null -> true
            is KtCallExpression -> selector.mutatesCollection(bindingContext)
            is KtReferenceExpression -> selector.mutatesCollection(bindingContext)
            else -> false
        }
    }

    private fun KtElement.mutatesCollection(bindingContext: BindingContext): Boolean {
        val functionDescriptor = this.getResolvedCall(bindingContext)?.resultingDescriptor
        val receiverType = functionDescriptor?.receiverType()
        return if (receiverType?.constructor?.declarationDescriptor != null) {
            receiverType.getKotlinTypeFqName(false) in mutableCollections
        } else {
            true
        }
    }

    private fun CallableDescriptor.receiverType(): KotlinType? {
        return if (this.isExtension) {
            this.extensionReceiverParameter?.type
        } else {
            this.dispatchReceiverParameter?.type
        }
    }

}