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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver

private val GET_PROP_WITH_DEFAULT_MATCHER = FunMatcher {
    qualifier = "java.util.Properties"
    name = "getProperty"
    withArguments("kotlin.String", "kotlin.String")
}

private val KOTLIN_CHAIN_CALL_CONSTRUCTS = listOf("let", "also", "run")
private const val MAX_AST_PARENT_TRAVERSALS = 25

internal fun KtExpression.predictRuntimeStringValue(bindingContext: BindingContext) =
    predictRuntimeValueExpression(bindingContext).stringValue(bindingContext)

internal fun KtExpression.predictRuntimeStringValueWithSecondaries(bindingContext: BindingContext) =
    mutableListOf<PsiElement>().let {
        predictRuntimeValueExpression(bindingContext, it)
            .stringValue(bindingContext, it) to it
    }

internal fun KtExpression.predictRuntimeIntValue(bindingContext: BindingContext) =
    predictRuntimeValueExpression(bindingContext).let { runtimeValueExpression ->
        runtimeValueExpression.getType(bindingContext)?.let {
            bindingContext.get(BindingContext.COMPILE_TIME_VALUE, runtimeValueExpression)?.getValue(it) as? Int
        }
    }

internal fun KtExpression.predictRuntimeValueExpression(
    bindingContext: BindingContext,
    declarations: MutableList<PsiElement> = mutableListOf(),
): KtExpression =
    if (this is KtReferenceExpression) {
        val referenceTarget = extractLetAlsoTargetExpression(bindingContext)
            ?: extractFromInitializer(bindingContext, declarations)

        referenceTarget?.predictRuntimeValueExpression(bindingContext, declarations)
    } else {
        getCall(bindingContext)?.predictValueExpression(bindingContext)
    } ?: this

internal fun KtCallExpression.predictReceiverExpression(
    bindingContext: BindingContext,
    precomputedResolvedCall: ResolvedCall<*>? = null,
): KtExpression? {
    val resolvedCall = precomputedResolvedCall ?: getResolvedCall(bindingContext)

    // For calls of the format `foo.bar()` (i.e. with explicit receiver)
    resolvedCall?.getReceiverExpression()?.let { explicitReceiver ->
        return explicitReceiver.predictRuntimeValueExpression(bindingContext)
    }

    // For calls of the format `foo()` (i.e. where `this` is the implicit receiver)
    resolvedCall?.getImplicitReceiverValue()?.let { implicitReceiver ->
        return implicitReceiver.extractWithRunTargetExpression(this, bindingContext)
    }

    return null
}

/**
 * @param declarations is used to collect all visited declaration for reporting secondary locations
 */
private fun KtExpression.stringValue(
    bindingContext: BindingContext,
    declarations: MutableList<PsiElement> = mutableListOf(),
): String? = when (this) {
    is KtStringTemplateExpression -> {
        val entries = entries.map {
            if (it.expression != null) it.expression!!.stringValue(bindingContext,
                declarations) else it.text
        }
        if (entries.all { it != null }) entries.joinToString("") else null
    }
    is KtNameReferenceExpression -> {
        val descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, this)
        descriptor?.let {
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
            if (declaration is KtProperty && !declaration.isVar) {
                declarations.add(declaration)
                declaration.delegateExpressionOrInitializer?.stringValue(bindingContext, declarations)
            } else null
        }
    }
    is KtDotQualifiedExpression -> selectorExpression?.stringValue(bindingContext, declarations)
    is KtBinaryExpression ->
        if (operationToken == KtTokens.PLUS)
            left?.stringValue(bindingContext, declarations)?.plus(right?.stringValue(bindingContext, declarations))
        else null
    else -> null
}

private fun Call.predictValueExpression(bindingContext: BindingContext) =
    if (GET_PROP_WITH_DEFAULT_MATCHER.matches(this, bindingContext)) {
        valueArguments[1].getArgumentExpression()
    } else null

private fun KtReferenceExpression.extractFromInitializer(
    bindingContext: BindingContext,
    declarations: MutableList<PsiElement> = mutableListOf(),
) =
    bindingContext.get(BindingContext.REFERENCE_TARGET, this)?.let {
        DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtProperty
    }?.let { declaration ->
        if (!declaration.isVar) {
            declarations.add(declaration)
            declaration.delegateExpressionOrInitializer?.predictRuntimeValueExpression(bindingContext)
        } else null
    }

private fun KtReferenceExpression.extractLetAlsoTargetExpression(bindingContext: BindingContext) =
    findReceiverScopeFunctionLiteral(bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

private fun ImplicitReceiver.extractWithRunTargetExpression(startNode: PsiElement, bindingContext: BindingContext) =
    findReceiverScopeFunctionLiteral(startNode, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

private fun KtFunctionLiteral.findLetAlsoRunWithTargetExpression(bindingContext: BindingContext): KtExpression? {
    getParentCall(bindingContext)?.let { larwCallCandidate ->
        val callName = larwCallCandidate.callElement.getCalleeExpressionIfAny()?.text
        if (callName in KOTLIN_CHAIN_CALL_CONSTRUCTS) {
            return (larwCallCandidate.explicitReceiver as? ExpressionReceiver)?.expression?.predictRuntimeValueExpression(bindingContext)
        } else if (callName == "with") {
            return larwCallCandidate.getResolvedCall(bindingContext)?.getFirstArgumentExpression()
                ?.predictRuntimeValueExpression(bindingContext)
        }
    }
    return null
}

private fun KtReferenceExpression.findReceiverScopeFunctionLiteral(bindingContext: BindingContext): KtFunctionLiteral? =
    (bindingContext.get(BindingContext.REFERENCE_TARGET, this) as? ValueParameterDescriptor)?.containingDeclaration
        ?.findFunctionLiteral(this, bindingContext)

private fun ImplicitReceiver.findReceiverScopeFunctionLiteral(startNode: PsiElement, bindingContext: BindingContext): KtFunctionLiteral? =
    declarationDescriptor.findFunctionLiteral(startNode, bindingContext)

private fun DeclarationDescriptor.findFunctionLiteral(
    startNode: PsiElement,
    bindingContext: BindingContext,
): KtFunctionLiteral? {
    var curNode: PsiElement? = startNode
    for (i in 0 until MAX_AST_PARENT_TRAVERSALS) {
        curNode = curNode?.parent ?: break
        if (curNode is KtFunctionLiteral && bindingContext.get(BindingContext.FUNCTION, curNode) === this) {
            return curNode
        }
    }
    return null
}
