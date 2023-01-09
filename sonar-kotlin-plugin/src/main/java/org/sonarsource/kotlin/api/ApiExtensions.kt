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
package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.coroutines.hasSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.unwrappedGetMethod
import org.jetbrains.kotlin.psi2ir.unwrappedSetMethod
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.smartcasts.getKotlinTypeForComparison
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.util.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.util.getParentCall
import org.jetbrains.kotlin.resolve.calls.util.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure.findCorrespondingSupertype
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.sonarsource.kotlin.checks.EmptyCommentCheck

private val GET_PROP_WITH_DEFAULT_MATCHER = FunMatcher {
    qualifier = "java.util.Properties"
    name = "getProperty"
    withArguments("kotlin.String", "kotlin.String")
}

private val KOTLIN_CHAIN_CALL_CONSTRUCTS = listOf("let", "also", "run", "apply")
private const val MAX_AST_PARENT_TRAVERSALS = 25

private val STRING_TO_BYTE_FUNS = listOf(
    FunMatcher(qualifier = "kotlin.text", name = "toByteArray"),
    FunMatcher(qualifier = "java.lang.String", name = "getBytes")
)

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

internal fun KtExpression.predictRuntimeBooleanValue(bindingContext: BindingContext) =
    predictRuntimeValueExpression(bindingContext).let { runtimeValueExpression ->
        runtimeValueExpression.getType(bindingContext)?.let {
            bindingContext.get(BindingContext.COMPILE_TIME_VALUE, runtimeValueExpression)?.getValue(it) as? Boolean
        }
    }

/**
 * In Kotlin, we may often be dealing with expressions that can already statically be resolved to prior and more accurate expressions that
 * they will alias at runtime. A good example of this are `it` within `let` and `also` scopes, as well as `this` within `with`, `apply`
 * and `run` scopes. Other examples include constants assigned to a property elsewhere.
 *
 * This function will try to resolve the current expression as far as it statically can, including deparenthesizing the expression.
 */
internal fun KtExpression.predictRuntimeValueExpression(
    bindingContext: BindingContext,
    declarations: MutableList<PsiElement> = mutableListOf(),
): KtExpression = this.deparenthesize().let { deparenthesized ->
    when (deparenthesized) {
        is KtReferenceExpression -> run {
            val referenceTarget = deparenthesized.extractLetAlsoTargetExpression(bindingContext)
                ?: deparenthesized.extractFromInitializer(bindingContext, declarations)

            referenceTarget?.predictRuntimeValueExpression(bindingContext, declarations)
        }

        is KtParenthesizedExpression -> deparenthesized.expression?.predictRuntimeValueExpression(bindingContext, declarations)
        is KtBinaryExpressionWithTypeRHS -> deparenthesized.left.predictRuntimeValueExpression(bindingContext, declarations)
        is KtThisExpression -> bindingContext.get(BindingContext.REFERENCE_TARGET, deparenthesized.instanceReference)
            ?.findFunctionLiteral(deparenthesized, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

        else -> deparenthesized.getCall(bindingContext)?.predictValueExpression(bindingContext)
    } ?: deparenthesized as? KtExpression
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
    return resolvedCall?.getImplicitReceiverValue()?.extractWithRunApplyTargetExpression(this, bindingContext)
}

internal fun KtStringTemplateExpression.asString() = entries.joinToString("") { it.text }

internal fun PsiElement.linesOfCode(): Set<Int> {
    val lines = mutableSetOf<Int>()
    val document = this.containingFile.viewProvider.document!!
    this.accept(object : KtTreeVisitorVoid() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            if (element is LeafPsiElement && element !is PsiWhiteSpace && element !is PsiComment) {
                lines.add(document.getLineNumber(element.textRange.startOffset) + 1)
            }
        }
    })
    return lines
}

/**
 * Replacement for [org.sonarsource.kotlin.converter.CommentAnnotationsAndTokenVisitor.createComment]
 * TODO unify with similar code in [EmptyCommentCheck]
 */
internal fun PsiComment.getContent() =
    when (tokenType) {
        KtTokens.BLOCK_COMMENT -> text.substring(2, textLength - 2)
        KtTokens.EOL_COMMENT -> text.substring(2, textLength)
        else -> text
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
            if (it.expression != null) it.expression!!.stringValue(bindingContext, declarations) else it.text
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

/**
 * Will try to resolve what `it` is an alias for inside of a `let` or `also` scope.
 */
private fun KtReferenceExpression.extractLetAlsoTargetExpression(bindingContext: BindingContext) =
    findReceiverScopeFunctionLiteral(bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

/**
 * Will try to resolve what `this` is an alias for inside a `with`, `run` or `apply` scope.
 */
private fun ImplicitReceiver.extractWithRunApplyTargetExpression(startNode: PsiElement, bindingContext: BindingContext) =
    findReceiverScopeFunctionLiteral(startNode, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

private fun KtFunctionLiteral.findLetAlsoRunWithTargetExpression(bindingContext: BindingContext): KtExpression? =
    getParentCall(bindingContext)?.let { larwCallCandidate ->
        when (larwCallCandidate.callElement.getCalleeExpressionIfAny()?.text) {
            in KOTLIN_CHAIN_CALL_CONSTRUCTS -> {
                (larwCallCandidate.explicitReceiver as? ExpressionReceiver)?.expression?.predictRuntimeValueExpression(bindingContext)
            }

            "with" -> {
                larwCallCandidate.getResolvedCall(bindingContext)?.getFirstArgumentExpression()
                    ?.predictRuntimeValueExpression(bindingContext)
            }

            else -> null
        }
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

fun KtNamedFunction.overrides() = modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

fun KtNamedFunction.isAbstract() = modifierList?.hasModifier(KtTokens.ABSTRACT_KEYWORD) ?: false

fun KtNamedFunction.suspendModifier() = modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD)

fun KtQualifiedExpression.resolveReferenceTarget(bindingContext: BindingContext) =
    this.selectorExpression?.referenceExpression()?.let { bindingContext.get(BindingContext.REFERENCE_TARGET, it) }

fun DeclarationDescriptor.scope() = fqNameSafe.asString().substringBeforeLast(".")

fun KtCallExpression.expressionTypeFqn(bindingContext: BindingContext) =
    bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, this)?.type?.getJetTypeFqName(false)

private fun KtProperty.determineType(bindingContext: BindingContext) =
    (typeReference?.let { bindingContext.get(BindingContext.TYPE, it) }
        ?: bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, initializer)?.type)

fun KtProperty.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getJetTypeFqName(printTypeArguments)

private fun KtParameter.determineType(bindingContext: BindingContext) =
    bindingContext.get(BindingContext.TYPE, typeReference)

fun KtParameter.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getJetTypeFqName(printTypeArguments)

private fun KtTypeReference.determineType(bindingContext: BindingContext) =
    bindingContext.get(BindingContext.TYPE, this)

fun KtTypeReference.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getJetTypeFqName(printTypeArguments)

fun KtNamedFunction.returnTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    returnType(bindingContext)?.getJetTypeFqName(printTypeArguments)

fun KtNamedFunction.returnType(bindingContext: BindingContext) =
    createTypeBindingForReturnType(bindingContext)?.type

fun KtLambdaArgument.isSuspending(
    bindingContext: BindingContext,
) = (parent as? KtCallExpression)
    ?.getResolvedCall(bindingContext)
    ?.resultingDescriptor
    ?.valueParameters
    // using .lastOrNull() instead of .last() due to possible NoSuchElementException in case of incomplete semantic
    ?.lastOrNull()
    ?.hasSuspendFunctionType
    ?: false

fun CallableDescriptor.throwsExceptions(exceptions: Collection<String>) =
    annotations.any {
        it.fqName?.asString() == THROWS_FQN &&
            (it.allValueArguments.asSequence()
                .find { (k, _) -> k.asString() == "exceptionClasses" }
                ?.value as? ArrayValue)
                ?.value
                ?.mapNotNull { it.value as? KClassValue.Value.NormalClass }
                ?.map { it.value.toString().replace("/", ".") }
                ?.any(exceptions::contains)
            ?: false
    }

fun KtNamedFunction.isInfix() = hasModifier(KtTokens.INFIX_KEYWORD)

fun KtNamedFunction.isAnonymous() = nameIdentifier == null

fun Call.findCallInPrecedingCallChain(matcher: FunMatcherImpl, bindingContext: BindingContext): Pair<Call, ResolvedCall<*>>? {
    var receiver = this
    var receiverResolved = receiver.getResolvedCall(bindingContext) ?: return null
    while (!matcher.matches(receiverResolved)) {
        val callElement = receiver.callElement as? KtCallExpression ?: return null
        receiver = callElement.predictReceiverExpression(bindingContext)?.getCall(bindingContext) ?: return null
        receiverResolved = receiver.getResolvedCall(bindingContext) ?: return null
    }
    return receiver to receiverResolved
}

fun ResolvedValueArgument.isNull(bindingContext: BindingContext) = (
    (this as? ExpressionValueArgument)
        ?.valueArgument
        ?.getArgumentExpression()
        ?.predictRuntimeValueExpression(bindingContext)
    )?.isNull() ?: false

fun KtExpression.getCalleeOrUnwrappedGetMethod(bindingContext: BindingContext) =
    (this as? KtDotQualifiedExpression)?.let { dotQualifiedExpression ->
        (dotQualifiedExpression.selectorExpression as? KtNameReferenceExpression)?.let { nameSelector ->
            (bindingContext.get(BindingContext.REFERENCE_TARGET, nameSelector) as? PropertyDescriptor)?.unwrappedGetMethod
        }
    } ?: this.getResolvedCall(bindingContext)?.resultingDescriptor

/**
 * Checks whether the expression is a call, matches the FunMatchers in [STRING_TO_BYTE_FUNS] and is called on a constant string value.
 */
fun KtExpression.isBytesInitializedFromString(bindingContext: BindingContext) =
    getCalleeOrUnwrappedGetMethod(bindingContext)?.let { callee ->
        STRING_TO_BYTE_FUNS.any { it.matches(callee) } &&
            (getCall(bindingContext)?.explicitReceiver as? ExpressionReceiver)?.expression?.predictRuntimeStringValue(bindingContext) != null
    } ?: false

fun ResolvedCall<*>.simpleArgExpressionOrNull(index: Int) =
    this.valueArgumentsByIndex
        ?.getOrNull(index)
        ?.arguments?.firstOrNull()
        ?.getArgumentExpression()

/**
 * Will try to find any usages of this reference. You can provide a searchStartNode to define the scope of the search (will
 * use the closest block statement going up the AST). You can also provide a predicate to filter findings.
 *
 * If allUsages parameter is set to true, it will return all the findings, otherwise only previous findings will be returned
 */
fun KtNameReferenceExpression.findUsages(
    searchStartNode: KtExpression = this,
    allUsages: Boolean = false,
    predicate: (KtNameReferenceExpression) -> Boolean = { _ -> true },
) =
    mutableListOf<KtNameReferenceExpression>().also { acc ->
        searchStartNode.getParentOfType<KtBlockExpression>(false)
            ?.collectDescendantsOfType<KtNameReferenceExpression> { it.getReferencedName() == this.getReferencedName() }
            ?.let { usages ->
                for (usage in usages) {
                    if (usage === this) {
                        if (allUsages) continue else break
                    } else if (predicate(usage)) {
                        acc.add(usage)
                    }
                }
            }
    }

/**
 * Will try to find all the usages of this property. You can provide a searchStartNode to define the scope of the search (will
 * use the closest block statement going up the AST). You can also provide a predicate to filter findings.
 */
fun KtProperty.findUsages(
    searchStartNode: KtExpression = this,
    predicate: (KtNameReferenceExpression) -> Boolean = { _ -> true },
) =
    mutableListOf<KtNameReferenceExpression>().also { acc ->
        searchStartNode.getParentOfType<KtBlockExpression>(false)
            ?.collectDescendantsOfType<KtNameReferenceExpression> { it.getReferencedName() == name }
            ?.let { usages ->
                for (usage in usages) {
                    if (predicate(usage)) {
                        acc.add(usage)
                    }
                }
            }
    }

/**
 * Checks whether the variable has been initialized with the help of a secure random function
 */
fun KtExpression.isInitializedPredictably(searchStartNode: KtExpression, bindingContext: BindingContext): Boolean {
    return this !is KtNameReferenceExpression || this.findUsages(searchStartNode) {
        it.getParentOfType<KtCallExpression>(false).getResolvedCall(bindingContext) matches SECURE_RANDOM_FUNS
    }.isEmpty()
}

/**
 * Checks if an expression is a function local variable
 */
fun KtExpression?.isLocalVariable(bindingContext: BindingContext) =
    (this is KtNameReferenceExpression) && (bindingContext.get(BindingContext.REFERENCE_TARGET, this) is LocalVariableDescriptor)

fun KtExpression?.setterMatches(bindingContext: BindingContext, propertyName: String, matcher: FunMatcherImpl): Boolean = when (this) {
    is KtNameReferenceExpression -> (getReferencedName() == propertyName) &&
        (matcher.matches((bindingContext.get(BindingContext.REFERENCE_TARGET, this) as? PropertyDescriptor)?.unwrappedSetMethod))

    is KtQualifiedExpression -> selectorExpression.setterMatches(bindingContext, propertyName, matcher)
    else -> false
}

fun KtExpression?.getterMatches(bindingContext: BindingContext, propertyName: String, matcher: FunMatcherImpl): Boolean = when (this) {
    is KtNameReferenceExpression -> (getReferencedName() == propertyName) &&
        (matcher.matches((bindingContext.get(BindingContext.REFERENCE_TARGET, this) as? PropertyDescriptor)?.unwrappedGetMethod))

    is KtQualifiedExpression -> selectorExpression.getterMatches(bindingContext, propertyName, matcher)
    else -> false
}

fun KtBinaryExpression.isPlus() =
    this.operationReference.operationSignTokenType?.let { OperatorConventions.BINARY_OPERATION_NAMES[it] }?.asString() == "plus"

fun PsiElement?.getType(bindingContext: BindingContext) =
    this?.let { bindingContext.get(BindingContext.VARIABLE, it)?.type }

fun KotlinType.simpleName(): String = getJetTypeFqName(false).substringAfterLast(".")

fun PsiElement?.determineType(bindingContext: BindingContext): KotlinType? =
    this?.let {
        when (this) {
            is KtCallExpression -> getResolvedCall(bindingContext)?.resultingDescriptor?.returnType
            is KtParameter -> determineType(bindingContext)
            is KtTypeReference -> determineType(bindingContext)
            is KtProperty -> determineType(bindingContext)
            is KtDotQualifiedExpression -> getResolvedCall(bindingContext)?.resultingDescriptor?.returnType
            is KtReferenceExpression -> bindingContext.get(BindingContext.REFERENCE_TARGET, this).determineType()
            is KtFunction -> bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this).determineType()
            is KtClass -> bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this).determineType()
            is KtExpression -> this.getKotlinTypeForComparison(bindingContext)
            is KtValueArgument -> this.getArgumentExpression()?.determineType(bindingContext)
            else -> null
        }

    }

fun KotlinType.isSupertypeOf(other: KotlinType): Boolean {
    return findCorrespondingSupertype(other, this).let { it != null && it != other }
}

fun DeclarationDescriptor?.determineType(): KotlinType? =
    when (this) {
        is FunctionDescriptor -> returnType
        is ClassDescriptor -> defaultType
        is PropertyDescriptor -> type
        is ValueDescriptor -> type
        else -> null
    }

fun KtQualifiedExpression?.determineSignature(bindingContext: BindingContext): DeclarationDescriptor? =
    when (val selectorExpr = this?.selectorExpression) {
        is KtCallExpression -> bindingContext.get(BindingContext.REFERENCE_TARGET, selectorExpr.getCallNameExpression())
        is KtSimpleNameExpression -> bindingContext.get(BindingContext.REFERENCE_TARGET, selectorExpr)
        else -> null
    }

fun KtAnnotationEntry.annotatedElement(): KtAnnotated {
    var annotated = parent
    while (annotated !is KtAnnotated) annotated = annotated.parent
    return annotated
}
