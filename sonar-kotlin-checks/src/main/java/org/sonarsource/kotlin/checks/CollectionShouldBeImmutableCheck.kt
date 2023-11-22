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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.determineTypeAsString
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6524")
class CollectionShouldBeImmutableCheck : AbstractCheck() {
    private val mutableCollections =
        setOf("kotlin.collections.MutableList", "kotlin.collections.MutableSet", "kotlin.collections.MutableMap")
    private val mutatorsOperations =
        setOf("add", "addAll", "remove", "removeAll", "retainAll", "clear", "set", "removeAt", "put", "putAll", "getOrPut")
    private val assignmentOperations = setOf(KtTokens.PLUSEQ, KtTokens.MINUSEQ)


    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        val bindingContext = context.bindingContext

        if (function.receiverTypeReference?.determineTypeAsString(bindingContext) in mutableCollections) {
            reportForExtensionFunction(function, context)
        }
        reportForParametersAndProperties(function, bindingContext, context)
    }

    private fun reportForParametersAndProperties(
        function: KtNamedFunction,
        bindingContext: BindingContext,
        context: KotlinFileContext,
    ) {
        val mutableProperties: List<KtNamedDeclaration> = function
            .collectDescendantsOfType<KtProperty> { it.isMutableCollection(bindingContext) }
        val mutableParameters: List<KtNamedDeclaration> = function.valueParameters
            .filter { it.isMutableCollection(bindingContext) }

        val mutableVariables = mutableProperties + mutableParameters


        val mutatedCollections: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtQualifiedExpression> { it.mutateCollection(bindingContext) }
            .mapNotNull { simpleReference(it.receiverExpression) }

        val arrayAccess: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtBinaryExpression> { it.isArrayAccess() }
            .mapNotNull { (it.left as KtArrayAccessExpression).arrayExpression }
            .filter { it.isMutableCollection(bindingContext) }
            .mapNotNull { simpleReference(it) }

        val assignments: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtBinaryExpression> { it.isAssignment() }
            .mapNotNull { simpleReference(it.left) }

        val mutablePropertiesInFunctionCalls: List<KtNameReferenceExpression> = function
            .collectDescendantsOfType<KtCallExpression>()
            .flatMap { it.valueArguments }
            .filter { it.isMutableCollection(bindingContext) }
            .mapNotNull { simpleReference(it.getArgumentExpression()) }


        val referencesToMutableCollections = mutatedCollections + mutablePropertiesInFunctionCalls + arrayAccess + assignments

        val mutatedNames = referencesToMutableCollections.mapNotNull { it.name }.toSet()
        val mutatedDeclarations =
            referencesToMutableCollections.mapNotNull { it.getResolvedCall(bindingContext)?.resultingDescriptor?.original }.toSet()


        mutableVariables
            .filter { it.isNotReferenced(mutatedDeclarations, mutatedNames, bindingContext) }
            .forEach { context.reportIssue(it, "Make this collection immutable.") }
    }

    private fun reportForExtensionFunction(
        function: KtNamedFunction,
        context: KotlinFileContext,
    ) {

        val functionsCalledOnThis = function
            .collectDescendantsOfType<KtCallExpression> { it.noReceiver() || it.thisIsReceiver() }

        if (functionsCalledOnThis.none { (it.calleeExpression as KtNameReferenceExpression).getReferencedName() in mutatorsOperations }) {
            context.reportIssue(function.receiverTypeReference!!.navigationElement, "Make this collection immutable.")
        }
    }
    

    private fun simpleReference(expression: KtExpression?): KtNameReferenceExpression? {
        return when (expression) {
            is KtNameReferenceExpression -> expression
            is KtParenthesizedExpression -> simpleReference(expression.expression!!)
            else -> null
        }
    }

    private fun KtNamedDeclaration.isNotReferenced(
        mutatedDeclarations: Set<CallableDescriptor>,
        mutatedNames: Set<String>,
        bindingContext: BindingContext,
    ): Boolean {
        val descriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, this]
        val name = this.nameAsSafeName.asString()

        return if (descriptor != null) {
            !mutatedDeclarations.contains(descriptor)
        } else {
            !mutatedNames.contains(name)
        }
    }

    private fun KtQualifiedExpression.mutateCollection(bindingContext: BindingContext): Boolean {
        return if (this.selectorExpression is KtCallExpression && (this.selectorExpression as KtCallExpression).calleeExpression is KtNameReferenceExpression) {
            val receiverType = this.receiverExpression.determineTypeAsString(bindingContext)
            val selector = ((this.selectorExpression as KtCallExpression).calleeExpression as KtNameReferenceExpression).getReferencedName()
            selector in mutatorsOperations && (receiverType == null || receiverType in mutableCollections)
        } else {
            false
        }
    }

    private fun KtCallExpression.noReceiver(): Boolean {
        return this.parent is KtBlockExpression
    }

    private fun KtCallExpression.thisIsReceiver(): Boolean {
        return if (this.parent is KtQualifiedExpression) {
            val parent = this.parent as KtQualifiedExpression
            simpleReference(parent.receiverExpression)?.name == "this"
        } else {
            false
        }
    }

    private fun KtExpression.isMutableCollection(bindingContext: BindingContext): Boolean {
        return this.determineTypeAsString(bindingContext) in mutableCollections
    }

    private fun KtValueArgument.isMutableCollection(bindingContext: BindingContext): Boolean {
        return this.getArgumentExpression()!!.isMutableCollection(bindingContext)
    }

    private fun KtNamedDeclaration.isMutableCollection(bindingContext: BindingContext): Boolean =
        this.determineTypeAsString(bindingContext) in mutableCollections

    private fun KtBinaryExpression.isArrayAccess(): Boolean {
        return this.operationToken == KtTokens.EQ && this.left is KtArrayAccessExpression

    }

    private fun KtBinaryExpression.isAssignment(): Boolean {
        return this.operationToken in assignmentOperations
    }

}