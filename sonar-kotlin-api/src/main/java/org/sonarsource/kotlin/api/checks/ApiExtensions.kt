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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SyntheticPropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
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
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi2ir.deparenthesize
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure.findCorrespondingSupertype
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.sonar.api.SonarProduct
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.utils.Version
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.merge
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.visiting.withKaSession

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

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.predictRuntimeStringValue()"))
fun KtExpression.predictRuntimeStringValue(bindingContext: BindingContext) =
    predictRuntimeValueExpression(bindingContext).stringValue(bindingContext)

fun KtExpression.predictRuntimeStringValue() =
    predictRuntimeValueExpression().stringValue()

fun KtExpression.predictRuntimeStringValueWithSecondaries() = withKaSession {
    mutableListOf<PsiElement>().let {
        predictRuntimeValueExpression(it)
            .stringValue(it) to it
    }
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.predictRuntimeIntValue()"))
fun KtExpression.predictRuntimeIntValue(bindingContext: BindingContext) =
    predictRuntimeValueExpression(bindingContext).let { runtimeValueExpression ->
        runtimeValueExpression.getType(bindingContext)?.let {
            bindingContext[BindingContext.COMPILE_TIME_VALUE, runtimeValueExpression]?.getValue(it) as? Int
        }
    }

fun KtExpression.predictRuntimeIntValue(): Int? = withKaSession {
    val valueExpression = predictRuntimeValueExpression()
    if (valueExpression.expressionType?.isIntType == true) {
        valueExpression.evaluate()?.value as? Int
    } else null
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.predictRuntimeBooleanValue()"))
fun KtExpression.predictRuntimeBooleanValue(bindingContext: BindingContext) =
    predictRuntimeValueExpression(bindingContext).let { runtimeValueExpression ->
        runtimeValueExpression.getType(bindingContext)?.let {
            bindingContext[BindingContext.COMPILE_TIME_VALUE, runtimeValueExpression]?.getValue(it) as? Boolean
        }
    }

fun KtExpression.predictRuntimeBooleanValue() = withKaSession {
    val valueExpression = predictRuntimeValueExpression()
    if (valueExpression.expressionType?.isBooleanType == true) {
        valueExpression.evaluate()?.value as? Boolean
    } else null
}

/**
 * In Kotlin, we may often be dealing with expressions that can already statically be resolved to prior and more accurate expressions that
 * they will alias at runtime. A good example of this are `it` within `let` and `also` scopes, as well as `this` within `with`, `apply`
 * and `run` scopes. Other examples include constants assigned to a property elsewhere.
 *
 * This function will try to resolve the current expression as far as it statically can, including deparenthesizing the expression.
 */
@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.predictRuntimeValueExpression(declarations)"))
fun KtExpression.predictRuntimeValueExpression(
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
        is KtThisExpression -> bindingContext[BindingContext.REFERENCE_TARGET, deparenthesized.instanceReference]
            ?.findFunctionLiteral(deparenthesized, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

        else -> deparenthesized.getCall(bindingContext)?.predictValueExpression(bindingContext)
    } ?: deparenthesized as? KtExpression
} ?: this

fun KtExpression.predictRuntimeValueExpression(
    declarations: MutableList<PsiElement> = mutableListOf(),
): KtExpression = this.deparenthesize().let { deparenthesized ->
    when (deparenthesized) {
        is KtReferenceExpression -> run {
            val referenceTarget = deparenthesized.extractLetAlsoTargetExpression()
                ?: deparenthesized.extractFromInitializer(declarations)

            referenceTarget?.predictRuntimeValueExpression(declarations)
        }

        is KtParenthesizedExpression -> deparenthesized.expression?.predictRuntimeValueExpression(declarations)

        is KtBinaryExpressionWithTypeRHS -> deparenthesized.left.predictRuntimeValueExpression(declarations)

        is KtThisExpression -> withKaSession {
            var symbol = deparenthesized.instanceReference.mainReference.resolveToSymbol()
            // TODO investigate: in K1 the symbol is anonymous function symbol, in K2 it is a parameter
            if (symbol !is KaAnonymousFunctionSymbol) symbol = symbol?.containingSymbol
            symbol?.findFunctionLiteral(deparenthesized)?.findLetAlsoRunWithTargetExpression()
        }

        else -> withKaSession {
            deparenthesized.resolveToCall()?.successfulFunctionCallOrNull()?.predictValueExpression()
        }
    } ?: deparenthesized as? KtExpression
} ?: this

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.predictReceiverExpression()"))
fun KtCallExpression.predictReceiverExpression(
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

fun KtExpression.predictReceiverExpression(): KtExpression? = withKaSession {
    val resolvedCall = this@predictReceiverExpression.resolveToCall()?.successfulFunctionCallOrNull()
    val symbol = resolvedCall?.partiallyAppliedSymbol
    val receiver = symbol?.extensionReceiver ?: symbol?.dispatchReceiver
    when (receiver) {
        is KaExplicitReceiverValue -> receiver.expression.predictRuntimeValueExpression()
        is KaImplicitReceiverValue -> receiver.symbol.containingSymbol
            ?.findFunctionLiteral(this@predictReceiverExpression)?.findLetAlsoRunWithTargetExpression()
        else -> null
    }
}

fun KtStringTemplateExpression.asString() = entries.joinToString("") { it.text }

fun PsiElement.linesOfCode(): Set<Int> {
    val lines = mutableSetOf<Int>()
    val document = this.containingFile.viewProvider.document!!
    this.accept(object : KtTreeVisitorVoid() {
        override fun visitElement(element: PsiElement) {
            if (element !is PsiComment) {
                super.visitElement(element)
            }
            if (element is LeafPsiElement && element !is PsiWhiteSpace && element !is PsiComment) {
                lines.add(document.getLineNumber(element.textRange.startOffset) + 1)
            }
        }
    })
    return lines
}

// TODO unify with similar code in `org.sonarsource.kotlin.checks.EmptyCommentCheck`
fun PsiComment.getContent() =
    when (tokenType) {
        KtTokens.BLOCK_COMMENT -> text.substring(2, textLength - 2)
        KtTokens.EOL_COMMENT -> text.substring(2, textLength)
        else -> text
    }

/**
 * @param declarations is used to collect all visited declaration for reporting secondary locations
 */
@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.stringValue(declarations)"))
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
        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, this]
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

fun KtExpression.stringValue(
    declarations: MutableList<PsiElement> = mutableListOf(),
): String? = withKaSession {
    when (this@stringValue) {
        is KtStringTemplateExpression -> {
            val entries = entries.map {
                if (it.expression != null) it.expression!!.stringValue(declarations) else it.text
            }
            if (entries.all { it != null }) entries.joinToString("") else null
        }

        is KtNameReferenceExpression -> {
            (this@stringValue.mainReference.resolveToSymbol() as? KaVariableSymbol)
                ?.let {
                    if (it.isVal) {
                        (it.psi as? KtProperty)
                            ?.apply { declarations.add(this) }
                            ?.delegateExpressionOrInitializer?.stringValue(declarations)
                    } else null
                }
        }

        is KtDotQualifiedExpression -> selectorExpression?.stringValue(declarations)
        is KtBinaryExpression ->
            if (operationToken == KtTokens.PLUS)
                left?.stringValue(declarations)?.plus(right?.stringValue(declarations))
            else null

        else -> null
    }
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.predictValueExpression()"))
private fun Call.predictValueExpression(bindingContext: BindingContext) =
    if (GET_PROP_WITH_DEFAULT_MATCHER.matches(this, bindingContext)) {
        valueArguments[1].getArgumentExpression()
    } else null

private fun KaFunctionCall<*>.predictValueExpression(): KtExpression? =
    if (GET_PROP_WITH_DEFAULT_MATCHER.matches(this)) {
        argumentMapping.keys.elementAt(1)
    } else null

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.extractFromInitializer()"))
private fun KtReferenceExpression.extractFromInitializer(
    bindingContext: BindingContext,
    declarations: MutableList<PsiElement> = mutableListOf(),
) =
    bindingContext[BindingContext.REFERENCE_TARGET, this]?.let {
        DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtProperty
    }?.let { declaration ->
        if (!declaration.isVar) {
            declarations.add(declaration)
            declaration.delegateExpressionOrInitializer?.predictRuntimeValueExpression(bindingContext)
        } else null
    }


private fun KtReferenceExpression.extractFromInitializer(
    declarations: MutableList<PsiElement> = mutableListOf(),
) = withKaSession {
    (this@extractFromInitializer.mainReference.resolveToSymbol() as? KaVariableSymbol)
        ?.let {
            if (it.isVal) {
                (it.psi as? KtProperty)
                    ?.apply { declarations.add(this) }
                    ?.delegateExpressionOrInitializer?.predictRuntimeValueExpression(declarations)
            } else null
        }
}

/**
 * Will try to resolve what `it` is an alias for inside of a `let` or `also` scope.
 */
@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.extractLetAlsoTargetExpression()"))
private fun KtReferenceExpression.extractLetAlsoTargetExpression(bindingContext: BindingContext) =
    findReceiverScopeFunctionLiteral(bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

private fun KtReferenceExpression.extractLetAlsoTargetExpression() =
    findReceiverScopeFunctionLiteral()?.findLetAlsoRunWithTargetExpression()

/**
 * Will try to resolve what `this` is an alias for inside a `with`, `run` or `apply` scope.
 */
@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.extractWithRunApplyTargetExpression()"))
private fun ImplicitReceiver.extractWithRunApplyTargetExpression(
    startNode: PsiElement,
    bindingContext: BindingContext
) =
    findReceiverScopeFunctionLiteral(startNode, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

private fun KaImplicitReceiverValue.extractWithRunApplyTargetExpression(
    startNode: PsiElement
) =
    findReceiverScopeFunctionLiteral(startNode)?.findLetAlsoRunWithTargetExpression()

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.findLetAlsoRunWithTargetExpression()"))
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

private fun KtFunctionLiteral.findLetAlsoRunWithTargetExpression(): KtExpression? = withKaSession {
    getParentCall()?.let { larwCallCandidate ->
        withKaSession {
            when (larwCallCandidate.partiallyAppliedSymbol.symbol.name?.asString()) {
                in KOTLIN_CHAIN_CALL_CONSTRUCTS -> {
                    (larwCallCandidate.partiallyAppliedSymbol.extensionReceiver as? KaExplicitReceiverValue)?.expression?.predictRuntimeValueExpression()
                }

                "with" -> {
                    larwCallCandidate.getFirstArgumentExpression()
                        ?.predictRuntimeValueExpression()
                }

                else -> null
            }
        }
    }
}

fun KtElement.getParentCallExpr(): KtElement? {
    val callExpressionTypes = arrayOf(
        KtSimpleNameExpression::class.java,
        KtCallElement::class.java,
        KtBinaryExpression::class.java,
        KtUnaryExpression::class.java,
        KtArrayAccessExpression::class.java
    )
    return PsiTreeUtil.getParentOfType(this, *callExpressionTypes)
}

fun KtElement.getParentCall(): KaFunctionCall<*>? {
    return withKaSession { getParentCallExpr()?.resolveToCall()?.successfulFunctionCallOrNull() }
}

fun KaFunctionCall<*>.getFirstArgumentExpression() =
    argumentMapping.keys.elementAtOrNull(0)

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.findReceiverScopeFunctionLiteral()"))
private fun KtReferenceExpression.findReceiverScopeFunctionLiteral(bindingContext: BindingContext): KtFunctionLiteral? =
    (bindingContext[BindingContext.REFERENCE_TARGET, this] as? ValueParameterDescriptor)?.containingDeclaration
        ?.findFunctionLiteral(this, bindingContext)

private fun KtReferenceExpression.findReceiverScopeFunctionLiteral(): KtFunctionLiteral? = withKaSession {
    when (val resolvedSymbol = this@findReceiverScopeFunctionLiteral.mainReference.resolveToSymbol()) {
        is KaValueParameterSymbol -> resolvedSymbol.containingSymbol
            ?.findFunctionLiteral(this@findReceiverScopeFunctionLiteral)
        else -> null
    }
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.findReceiverScopeFunctionLiteral(startNode)"))
private fun ImplicitReceiver.findReceiverScopeFunctionLiteral(
    startNode: PsiElement,
    bindingContext: BindingContext
): KtFunctionLiteral? =
    declarationDescriptor.findFunctionLiteral(startNode, bindingContext)

private fun KaImplicitReceiverValue.findReceiverScopeFunctionLiteral(
    startNode: PsiElement,
): KtFunctionLiteral? =
    symbol.findFunctionLiteral(startNode)

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.findFunctionLiteral(startNode)"))
private fun DeclarationDescriptor.findFunctionLiteral(
    startNode: PsiElement,
    bindingContext: BindingContext,
): KtFunctionLiteral? {
    var curNode: PsiElement? = startNode
    for (i in 0 until MAX_AST_PARENT_TRAVERSALS) {
        curNode = curNode?.parent ?: break
        if (curNode is KtFunctionLiteral && bindingContext[BindingContext.FUNCTION, curNode] === this) {
            return curNode
        }
    }
    return null
}

private fun KaSymbol.findFunctionLiteral(
    startNode: PsiElement,
): KtFunctionLiteral? = withKaSession {
    var curNode: PsiElement? = startNode
    for (i in 0 until MAX_AST_PARENT_TRAVERSALS) {
        curNode = curNode?.parent ?: break
        if (curNode is KtFunctionLiteral && curNode.symbol == this@findFunctionLiteral)
            return curNode
    }
    return null
}

fun KtNamedFunction.overrides() = modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

fun KtNamedFunction.isAbstract() = modifierList?.hasModifier(KtTokens.ABSTRACT_KEYWORD) ?: false

fun KtNamedFunction.isOpen(): Boolean {
    return modifierList?.hasModifier(KtTokens.OPEN_KEYWORD) ?: false
}

fun KtNamedFunction.isActual(): Boolean {
    return modifierList?.hasModifier(KtTokens.ACTUAL_KEYWORD) ?: false
}

fun KtNamedFunction.isExpect(): Boolean {
    return modifierList?.hasModifier(KtTokens.EXPECT_KEYWORD) ?: false
}

fun KtNamedFunction.suspendModifier() = modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD)

fun KtQualifiedExpression.resolveReferenceTarget(bindingContext: BindingContext) =
    this.selectorExpression?.referenceExpression()?.let { bindingContext[BindingContext.REFERENCE_TARGET, it] }

fun DeclarationDescriptor.scope() = fqNameSafe.asString().substringBeforeLast(".")

fun KtCallExpression.expressionTypeFqn(bindingContext: BindingContext): String? {
    val type = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, this]?.type
    if (/* intersection type */ type?.constructor?.declarationDescriptor == null) return null
    // Note that for intersection type next method throws IllegalArgumentException
    return type.getKotlinTypeFqName(false)
}

/**
 * See examples of [org.jetbrains.kotlin.analysis.api.types.KaFlexibleType]
 * (aka [platform type](https://kotlin.github.io/analysis-api/kaflexibletype.html)) in
 * [KtNamedFunction.returnTypeAsString] and [KtProperty.determineTypeAsString].
 *
 * TODO according to [Kotlin Analysis API Documentation](https://kotlin.github.io/analysis-api/types.html#example)
 * > Avoid using [org.jetbrains.kotlin.name.FqName]s or raw strings for type comparison.
 * > Use [org.jetbrains.kotlin.name.ClassId]s instead
 */
fun KaType.asFqNameString(): String? = withKaSession {
    (lowerBoundIfFlexible() as? KaClassType)?.classId?.asFqNameString()
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.determineType()"))
private fun KtProperty.determineType(bindingContext: BindingContext) =
    (typeReference?.let { bindingContext[BindingContext.TYPE, it] }
        ?: bindingContext[BindingContext.EXPRESSION_TYPE_INFO, initializer]?.type)

private fun KtProperty.determineType(): KaType? = withKaSession {
    typeReference?.getType() ?: initializer?.expressionType
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.determineTypeAsString()"))
fun KtProperty.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

/**
 * In the following example for the platform type `java.lang.String!`
 * ```
 * val flexibleAkaPlatformType = java.lang.String.valueOf(1)
 * ```
 * this function returns its lower bound `"java.lang.String"`.
 * @see asFqNameString
 */
fun KtProperty.determineTypeAsString(): String? = withKaSession {
    determineType()?.asFqNameString()
}

fun KtExpression.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.let {
        if (it.constructor.declarationDescriptor != null) it.getKotlinTypeFqName(printTypeArguments)
        else null
    }

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.determineType()"))
private fun KtParameter.determineType(bindingContext: BindingContext) =
    bindingContext[BindingContext.TYPE, typeReference]

private fun KtParameter.determineType(): KaType? = withKaSession {
    typeReference?.getType()
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.determineTypeAsString()"))
fun KtParameter.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

fun KtParameter.determineTypeAsString(): String? = withKaSession {
    determineType()?.asFqNameString()
}

private fun KtTypeReference.determineType(bindingContext: BindingContext) =
    bindingContext[BindingContext.TYPE, this]

fun KtTypeReference.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.returnTypeAsString()"))
fun KtNamedFunction.returnTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    returnType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

@Deprecated("use kotlin-analysis-api", ReplaceWith("this.returnType"))
fun KtNamedFunction.returnType(bindingContext: BindingContext) =
    createTypeBindingForReturnType(bindingContext)?.type

/**
 * In the following example for the platform type `java.lang.String!`
 * ```
 * fun flexibleAkaPlatformType() = java.lang.String.valueOf(1)
 * ```
 * this function returns its lower bound `"java.lang.String"`.
 * @see asFqNameString
 */
fun KtNamedFunction.returnTypeAsString(): String? = withKaSession {
    returnType.asFqNameString()
}

fun KtLambdaArgument.isSuspending() = withKaSession {
    (parent as? KtCallExpression)
        ?.resolveToCall()
        ?.successfulFunctionCallOrNull()
        ?.argumentMapping
        ?.values
        ?.lastOrNull()
        ?.returnType
        ?.isSuspendFunctionType
        ?: return false
}

fun KaFunctionSymbol.throwsExceptions(exceptions: Collection<String>) =
    annotations.any { annotation ->
        annotation.classId?.asFqNameString() == THROWS_FQN &&
            (annotation.arguments
                .find { arg -> arg.name.asString() == "exceptionClasses" }
                ?.expression as? KaAnnotationValue.ArrayValue)
                ?.values
                ?.mapNotNull { it as? KaAnnotationValue.ClassLiteralValue }
                ?.map { it.classId?.asFqNameString() }
                ?.any(exceptions::contains)
            ?: false
    }

fun KtNamedFunction.isInfix() = hasModifier(KtTokens.INFIX_KEYWORD)

fun KtExpression.findCallInPrecedingCallChain(
    matcher: FunMatcherImpl,
): Pair<KtExpression, KaFunctionCall<*>>? = withKaSession {
    var receiver = this@findCallInPrecedingCallChain
    var receiverResolved = receiver.resolveToCall()?.successfulFunctionCallOrNull() ?: return null
    while (!matcher.matches(receiverResolved)) {
        receiver = receiver.predictReceiverExpression() ?: return null
        receiverResolved = receiver.resolveToCall()?.singleFunctionCallOrNull() ?: return null
    }
    return receiver to receiverResolved
}

@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.isPredictedNull()"))
fun ResolvedValueArgument.isNull(bindingContext: BindingContext) = (
    (this as? ExpressionValueArgument)
        ?.valueArgument
        ?.getArgumentExpression()
        ?.predictRuntimeValueExpression(bindingContext)
    )?.isNull() ?: false

fun KtExpression.isPredictedNull() =
   predictRuntimeValueExpression().isNull()

@Deprecated("use kotlin-analysis-api instead")
fun KtExpression.getCalleeOrUnwrappedGetMethod(bindingContext: BindingContext) =
    (this as? KtDotQualifiedExpression)?.let { dotQualifiedExpression ->
        (dotQualifiedExpression.selectorExpression as? KtNameReferenceExpression)?.let { nameSelector ->
            (bindingContext[BindingContext.REFERENCE_TARGET, nameSelector] as? PropertyDescriptor)?.unwrappedGetMethod
        }
    } ?: this.getResolvedCall(bindingContext)?.resultingDescriptor

/**
 * Checks whether the expression is a call, matches the FunMatchers in [STRING_TO_BYTE_FUNS] and is called on a constant string value.
 */
@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.isBytesInitializedFromString()"))
fun KtExpression.isBytesInitializedFromString(bindingContext: BindingContext) =
    getCalleeOrUnwrappedGetMethod(bindingContext)?.let { callee ->
        STRING_TO_BYTE_FUNS.any { it.matches(callee) } &&
            (getCall(bindingContext)?.explicitReceiver as? ExpressionReceiver)?.expression?.predictRuntimeStringValue(bindingContext) != null
    } ?: false

fun KtExpression.isBytesInitializedFromString(): Boolean = withKaSession {
    val call = this@isBytesInitializedFromString.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
        ?: return@withKaSession false
    STRING_TO_BYTE_FUNS.any { it.matches(call) } &&
            ((call.partiallyAppliedSymbol.extensionReceiver ?: call.partiallyAppliedSymbol.dispatchReceiver)
                    as? KaExplicitReceiverValue)
                ?.expression
                ?.predictRuntimeStringValue() != null
}

@Deprecated("use kotlin-analysis-api instead")
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

fun KtReferenceExpression.getContainingDeclaration(bindingContext: BindingContext) =
    bindingContext[BindingContext.REFERENCE_TARGET, this]?.containingDeclaration

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
@Deprecated("use kotlin-analysis-api instead", ReplaceWith("this.isInitializedPredictably(searchStartNode)"))
fun KtExpression.isInitializedPredictably(searchStartNode: KtExpression, bindingContext: BindingContext): Boolean {
    return this !is KtNameReferenceExpression || this.findUsages(searchStartNode) {
        it.getParentOfType<KtCallExpression>(false).getResolvedCall(bindingContext) matches SECURE_RANDOM_FUNS
    }.isEmpty()
}

fun KtExpression.isInitializedPredictably(searchStartNode: KtExpression): Boolean {
    return this !is KtNameReferenceExpression || this.findUsages(searchStartNode) {
        withKaSession {
            it.getParentOfType<KtCallExpression>(false)?.resolveToCall()
                ?.successfulFunctionCallOrNull() matches SECURE_RANDOM_FUNS
        }
    }.isEmpty()
}

/**
 * Checks if an expression is a function local variable
 */
@Deprecated("use kotlin-analysis-api instead", replaceWith = ReplaceWith("isLocalVariable()"))
fun KtExpression?.isLocalVariable(bindingContext: BindingContext) =
    (this is KtNameReferenceExpression) && (bindingContext[BindingContext.REFERENCE_TARGET, this] is LocalVariableDescriptor)

fun KtExpression?.isLocalVariable(): Boolean = withKaSession {
    if (this@isLocalVariable !is KtNameReferenceExpression) return false
    return mainReference.resolveToSymbol() is KaLocalVariableSymbol
}

@Deprecated("use kotlin-analysis-api instead")
fun KtExpression?.setterMatches(bindingContext: BindingContext, propertyName: String, matcher: FunMatcherImpl): Boolean = when (this) {
    is KtNameReferenceExpression -> (getReferencedName() == propertyName) &&
        (matcher.matches((bindingContext[BindingContext.REFERENCE_TARGET, this] as? PropertyDescriptor)?.unwrappedSetMethod))

    is KtQualifiedExpression -> selectorExpression.setterMatches(bindingContext, propertyName, matcher)
    else -> false
}

fun KtExpression?.getterMatches(bindingContext: BindingContext, propertyName: String, matcher: FunMatcherImpl): Boolean = when (this) {
    is KtNameReferenceExpression -> (getReferencedName() == propertyName) &&
        (matcher.matches((bindingContext[BindingContext.REFERENCE_TARGET, this] as? PropertyDescriptor)?.unwrappedGetMethod))

    is KtQualifiedExpression -> selectorExpression.getterMatches(bindingContext, propertyName, matcher)
    else -> false
}

/**
 * Finds the closest ancestor of type T within the scope of a {@param stopCondition}.
 * By default, there is no stop condition so the search continues up till the AST root is reached.
 */
inline fun <reified T : KtExpression> KtExpression.findClosestAncestorOfType(stopCondition: (PsiElement) -> Boolean = { false }): T? {
    var parent = this.parent

    while (parent != null && !stopCondition.invoke(parent) && parent !is T) {
        parent = parent.parent
    }

    return parent as? T
}

fun PsiElement.findClosestAncestor(predicate: (PsiElement) -> Boolean): PsiElement? {
    var parent = this.parent

    while (parent != null && !predicate(parent)) {
        parent = parent.parent
    }

    return parent
}

fun KtBinaryExpression.isPlus() =
    this.operationReference.operationSignTokenType?.let { OperatorConventions.BINARY_OPERATION_NAMES[it] }?.asString() == "plus"

fun PsiElement?.getVariableType(bindingContext: BindingContext) =
    this?.let { bindingContext[BindingContext.VARIABLE, it]?.type }

/** Use [org.jetbrains.kotlin.analysis.api.components.KaTypeProvider.type] instead. */
@Deprecated("use kotlin-analysis-api instead")
fun KtTypeReference?.getType(bindingContext: BindingContext): KotlinType? =
    this?.let { bindingContext[BindingContext.TYPE, it] }

/**
 * Workaround for
 * [exceptions from KtTypeReference.type](https://github.com/JetBrains/kotlin/blob/v2.1.10/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaTypeProvider.kt#L81-L86)
 *
 * > org.jetbrains.kotlin.analysis.low.level.api.fir.api.InvalidFirElementTypeException: For TYPE_REFERENCE with text `Any`, the element of type interface org.jetbrains.kotlin.fir.FirElement expected, but no element found
 */
fun KtTypeReference.getType(): KaType = withKaSession {
    try {
        // TODO forbid calls of original method
        this@getType.type
    } catch (e: Exception) {
        buildClassType(ClassId.fromString("Error"))
    }
}

fun KtClassOrObject.hasExactlyOneFunctionAndNoProperties(): Boolean {
    var functionCount = 0
    return declarations.all {
        it !is KtProperty && (it !is KtNamedFunction || functionCount++ == 0)
    } && functionCount > 0
}

fun KotlinFileContext.merge(firstElement: PsiElement, lastElement: PsiElement) =
    merge(listOf(textRange(firstElement), textRange(lastElement)))

@Deprecated("use kotlin-analysis-api instead")
fun KotlinType.simpleName(): String = getKotlinTypeFqName(false).substringAfterLast(".")

/**
 * See examples of [org.jetbrains.kotlin.analysis.api.types.KaFlexibleType]
 * (aka [platform type](https://kotlin.github.io/analysis-api/kaflexibletype.html)) in
 * [KtNamedFunction.returnTypeAsString] and [KtProperty.determineTypeAsString].
 */
fun KaType.simpleName(): String? = withKaSession {
    return lowerBoundIfFlexible().symbol?.name?.asString()
}

fun PsiElement?.determineType(bindingContext: BindingContext): KotlinType? =
    this?.let {
        when (this) {
            is KtCallExpression -> getResolvedCall(bindingContext)?.resultingDescriptor?.returnType
            is KtParameter -> determineType(bindingContext)
            is KtTypeReference -> determineType(bindingContext)
            is KtProperty -> determineType(bindingContext)
            is KtDotQualifiedExpression -> getResolvedCall(bindingContext)?.resultingDescriptor?.returnType
            is KtReferenceExpression -> bindingContext[BindingContext.REFERENCE_TARGET, this].determineType()
            is KtFunction -> bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this].determineType()
            is KtClass -> bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this].determineType()
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

fun KtQualifiedExpression?.determineSignature(): KaSymbol? = withKaSession {
    when (val selectorExpr = this@determineSignature?.selectorExpression) {
        is KtCallExpression ->
            selectorExpr.getCallNameExpression()?.mainReference?.resolveToSymbol()
        is KtSimpleNameExpression ->
            selectorExpr.mainReference.resolveToSymbol()
        else -> null
    }
}

fun KtAnnotationEntry.annotatedElement(): KtAnnotated {
    var annotated = parent
    while (annotated !is KtAnnotated) annotated = annotated.parent
    return annotated
}

fun SensorContext.hasCacheEnabled(): Boolean {
    val runtime = runtime()
    return runtime.product != SonarProduct.SONARLINT &&
        runtime.apiVersion.isGreaterThanOrEqual(Version.create(9, 4)) &&
        isCacheEnabled
}

@OptIn(KaIdeApi::class)
fun KtWhenExpression.isExhaustive(): Boolean = withKaSession {
    return entries.any { it.isElse } || computeMissingCases().isEmpty()
}

val PropertyDescriptor.unwrappedGetMethod: FunctionDescriptor?
    get() = if (this is SyntheticPropertyDescriptor) this.getMethod else getter

val PropertyDescriptor.unwrappedSetMethod: FunctionDescriptor?
    get() = if (this is SyntheticPropertyDescriptor) this.setMethod else setter
