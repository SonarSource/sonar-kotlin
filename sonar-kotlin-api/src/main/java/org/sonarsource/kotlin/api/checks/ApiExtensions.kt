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
@file:OptIn(KaExperimentalApi::class, KaExperimentalApi::class)

package org.sonarsource.kotlin.api.checks

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
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
import org.jetbrains.kotlin.psi.*
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
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
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
import org.sonarsource.kotlin.api.visiting.analyze

annotation class Rewritten

private val GET_PROP_WITH_DEFAULT_MATCHER = FunMatcher {
    qualifier = "java.util.Properties"
    name = "getProperty"
    withArguments("kotlin.String", "kotlin.String")
}

private val KOTLIN_CHAIN_CALL_CONSTRUCTS = listOf("let", "also", "run", "apply")
private const val MAX_AST_PARENT_TRAVERSALS = 25

private val STRING_TO_BYTE_FUNS = listOf(
    FunMatcher(qualifier = "kotlin.text", name = "toByteArray"),
    FunMatcher(qualifier = "java.lang.String", name = "getBytes"),
)

private val STRING_BYTES = FunMatcher(qualifier = "java.lang.String", name = "bytes")

@Rewritten
fun KtExpression.predictRuntimeStringValue() =
    predictRuntimeValueExpression().stringValue()

@Rewritten
fun KtExpression.predictRuntimeIntValue(): Int? = analyze {
    predictRuntimeValueExpression()?.evaluate()?.value as? Int
}

@Rewritten
fun KtExpression.predictRuntimeBooleanValue() = analyze {
    predictRuntimeValueExpression().let { runtimeValueExpression ->
        runtimeValueExpression.evaluate()?.value as? Boolean
    }
}

/**
 * In Kotlin, we may often be dealing with expressions that can already statically be resolved to prior and more accurate expressions that
 * they will alias at runtime. A good example of this are `it` within `let` and `also` scopes, as well as `this` within `with`, `apply`
 * and `run` scopes. Other examples include constants assigned to a property elsewhere.
 *
 * This function will try to resolve the current expression as far as it statically can, including deparenthesizing the expression.
 */
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

        is KtParenthesizedExpression -> deparenthesized.expression?.predictRuntimeValueExpression(
            bindingContext,
            declarations
        )

        is KtBinaryExpressionWithTypeRHS -> deparenthesized.left.predictRuntimeValueExpression(
            bindingContext,
            declarations
        )

        is KtThisExpression -> bindingContext[BindingContext.REFERENCE_TARGET, deparenthesized.instanceReference]
            ?.findFunctionLiteral(deparenthesized, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)

        else -> deparenthesized.getCall(bindingContext)?.predictValueExpression(bindingContext)
    } ?: deparenthesized as? KtExpression
} ?: this

@Rewritten
fun KtExpression.predictRuntimeValueExpression(
    declarations: MutableList<PsiElement> = mutableListOf(),
): KtExpression = this.deparenthesize().let { deparenthesized ->
    when (deparenthesized) {
        is KtReferenceExpression -> run {
            val referenceTarget = deparenthesized.extractLetAlsoTargetExpression()
                ?: deparenthesized.extractFromInitializer(declarations)

            referenceTarget?.predictRuntimeValueExpression(declarations)
        }

        is KtParenthesizedExpression -> deparenthesized.expression?.predictRuntimeValueExpression(
            declarations
        )

        is KtBinaryExpressionWithTypeRHS -> deparenthesized.left.predictRuntimeValueExpression(
            declarations
        )

        is KtThisExpression -> analyze {
            var symbol = deparenthesized.instanceReference.mainReference.resolveToSymbol()
            // In K1 the symbol is anonymous function symbol, in K2 it is a parameter
            if (symbol !is KaAnonymousFunctionSymbol) symbol = symbol?.containingSymbol
            symbol?.findFunctionLiteral(deparenthesized)
                ?.findLetAlsoRunWithTargetExpression()
        }

        else -> analyze {
            deparenthesized.resolveToCall()?.successfulFunctionCallOrNull()?.predictValueExpression()
        }
    } ?: deparenthesized as? KtExpression
} ?: this

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


@Rewritten
fun KtCallExpression.predictReceiverExpression(
): KtExpression? =
    analyze {
        val call = this@predictReceiverExpression.resolveToCall()?.successfulFunctionCallOrNull()
        val symbol = call?.partiallyAppliedSymbol
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

@Rewritten
fun KtExpression.stringValue(
    declarations: MutableList<PsiElement> = mutableListOf(),
): String? = analyze {
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
                        (it.psi as? KtProperty)?.delegateExpressionOrInitializer?.stringValue(declarations)
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

private fun Call.predictValueExpression(bindingContext: BindingContext) =
    if (GET_PROP_WITH_DEFAULT_MATCHER.matches(this, bindingContext)) {
        valueArguments[1].getArgumentExpression()
    } else null

private fun KaFunctionCall<*>.predictValueExpression(): KtExpression? =
    if (GET_PROP_WITH_DEFAULT_MATCHER.matches(this)) {
        argumentMapping.keys.toList()[1]
    } else null

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


@Rewritten
private fun KtReferenceExpression.extractFromInitializer(
    declarations: MutableList<PsiElement> = mutableListOf(),
) = analyze {
    (this@extractFromInitializer.mainReference.resolveToSymbol() as? KaVariableSymbol)
        ?.let {
            if (it.isVal) {
                (it.psi as? KtProperty)
                    ?.apply { declarations.add(this) }
                    ?.delegateExpressionOrInitializer
                    ?.predictRuntimeValueExpression(declarations)
            } else null
        }
}

/**
 * Will try to resolve what `it` is an alias for inside of a `let` or `also` scope.
 */
private fun KtReferenceExpression.extractLetAlsoTargetExpression(bindingContext: BindingContext) =
    findReceiverScopeFunctionLiteral(bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)


@Rewritten
private fun KtReferenceExpression.extractLetAlsoTargetExpression() =
    findReceiverScopeFunctionLiteral()?.findLetAlsoRunWithTargetExpression()

/**
 * Will try to resolve what `this` is an alias for inside a `with`, `run` or `apply` scope.
 */
private fun ImplicitReceiver.extractWithRunApplyTargetExpression(
    startNode: PsiElement,
    bindingContext: BindingContext
) =
    findReceiverScopeFunctionLiteral(startNode, bindingContext)?.findLetAlsoRunWithTargetExpression(bindingContext)


@Rewritten
private fun KaImplicitReceiverValue.extractWithRunApplyTargetExpression(
    startNode: PsiElement
) =
    findReceiverScopeFunctionLiteral(startNode)?.findLetAlsoRunWithTargetExpression()

@Rewritten
private fun KaImplicitReceiverValue.findReceiverScopeFunctionLiteral(
    startNode: PsiElement,
): KtFunctionLiteral? =
    symbol.findFunctionLiteral(startNode)

@Rewritten
private fun KtReferenceExpression.findReceiverScopeFunctionLiteral(): KtFunctionLiteral? =
    analyze {
        this@findReceiverScopeFunctionLiteral.mainReference.resolveToSymbol()?.containingSymbol?.findFunctionLiteral(
            this@findReceiverScopeFunctionLiteral
        )
    }


private fun KtFunctionLiteral.findLetAlsoRunWithTargetExpression(bindingContext: BindingContext): KtExpression? =
    getParentCall(bindingContext)?.let { larwCallCandidate ->
        when (larwCallCandidate.callElement.getCalleeExpressionIfAny()?.text) {
            in KOTLIN_CHAIN_CALL_CONSTRUCTS -> {
                (larwCallCandidate.explicitReceiver as? ExpressionReceiver)?.expression?.predictRuntimeValueExpression(
                    bindingContext
                )
            }

            "with" -> {
                larwCallCandidate.getResolvedCall(bindingContext)?.getFirstArgumentExpression()
                    ?.predictRuntimeValueExpression(bindingContext)
            }

            else -> null
        }
    }


@Rewritten
private fun KtFunctionLiteral.findLetAlsoRunWithTargetExpression(): KtExpression? =
    analyze {
        (getParentCall() as? KaFunctionCall<*>)?.let { larwCallCandidate ->
            analyze {
                when (larwCallCandidate.partiallyAppliedSymbol.symbol.name?.asString()) {
                    in KOTLIN_CHAIN_CALL_CONSTRUCTS -> {
                        (larwCallCandidate.partiallyAppliedSymbol.extensionReceiver as? KaExplicitReceiverValue)?.expression?.predictRuntimeValueExpression()
                    }

                    "with" -> {
                        val argumentMapping = larwCallCandidate.argumentMapping
                        val argument = if (argumentMapping.size >= 1) argumentMapping.keys.toList()[0] else null
                        return argument?.predictRuntimeValueExpression()
                    }

                    else -> null
                }
            }
        }
    }

fun KaFunctionCall<*>.getFirstArgumentExpression() =
    argumentMapping.keys.toList().firstOrNull()

fun KtElement.getParentCall(): KaCall? {
    val callExpressionTypes = arrayOf(
        KtSimpleNameExpression::class.java, KtCallElement::class.java, KtBinaryExpression::class.java,
        KtUnaryExpression::class.java, KtArrayAccessExpression::class.java
    )
    val parentOfType = PsiTreeUtil.getParentOfType(this, *callExpressionTypes)
    return analyze { parentOfType?.resolveToCall()?.successfulCallOrNull() }
}


private fun KtReferenceExpression.findReceiverScopeFunctionLiteral(bindingContext: BindingContext): KtFunctionLiteral? =
    (bindingContext[BindingContext.REFERENCE_TARGET, this] as? ValueParameterDescriptor)?.containingDeclaration
        ?.findFunctionLiteral(this, bindingContext)

private fun ImplicitReceiver.findReceiverScopeFunctionLiteral(
    startNode: PsiElement,
    bindingContext: BindingContext
): KtFunctionLiteral? =
    declarationDescriptor.findFunctionLiteral(startNode, bindingContext)

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

@Rewritten
private fun KaSymbol.findFunctionLiteral(
    startNode: PsiElement,
): KtFunctionLiteral? = analyze {
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
    if (type?.constructor?.declarationDescriptor == null) return null
    return type.getKotlinTypeFqName(false)
}

private fun KtProperty.determineType(bindingContext: BindingContext) =
    (typeReference?.let { bindingContext[BindingContext.TYPE, it] }
        ?: bindingContext[BindingContext.EXPRESSION_TYPE_INFO, initializer]?.type)

@Deprecated("")
fun KtProperty.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

fun KtExpression.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

private fun KtParameter.determineType(bindingContext: BindingContext) =
    bindingContext[BindingContext.TYPE, typeReference]

@Deprecated("")
fun KtParameter.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

/**
 * TODO see also [FunMatcherImpl.checkReturnType]
 */
fun KtDeclaration.determineTypeAsString() = analyze {
    (this@determineTypeAsString.returnType as? KaClassType)?.classId?.asFqNameString()
}

private fun KtTypeReference.determineType(bindingContext: BindingContext) =
    bindingContext[BindingContext.TYPE, this]

fun KtTypeReference.determineTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    determineType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

fun KtNamedFunction.returnTypeAsString(): String? {
    val namedFunction = this
    analyze {
        when (val returnType = namedFunction.returnType) {
            is KaClassType -> return returnType.classId.asFqNameString()
            else -> return null
        }
    }
}

@Deprecated("use kotlin-analysis-api instead", replaceWith = ReplaceWith("returnTypeAsString()"))
fun KtNamedFunction.returnTypeAsString(bindingContext: BindingContext, printTypeArguments: Boolean = false) =
    returnType(bindingContext)?.getKotlinTypeFqName(printTypeArguments)

fun KtNamedFunction.returnType(bindingContext: BindingContext) =
    createTypeBindingForReturnType(bindingContext)?.type

fun CallableDescriptor.throwsExceptions(exceptions: Collection<String>) =
    annotations.any { annotation ->
        annotation.fqName?.asString() == THROWS_FQN &&
                (annotation.allValueArguments.asSequence()
                    .find { (k, _) -> k.asString() == "exceptionClasses" }
                    ?.value as? ArrayValue)
                    ?.value
                    ?.mapNotNull { it.value as? KClassValue.Value.NormalClass }
                    ?.map { it.value.toString().replace("/", ".") }
                    ?.any(exceptions::contains)
                ?: false
    }

fun KtNamedFunction.isInfix() = hasModifier(KtTokens.INFIX_KEYWORD)

fun Call.findCallInPrecedingCallChain(
    matcher: FunMatcherImpl,
    bindingContext: BindingContext
): Pair<Call, ResolvedCall<*>>? {
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
            (bindingContext[BindingContext.REFERENCE_TARGET, nameSelector] as? PropertyDescriptor)?.unwrappedGetMethod
        }
    } ?: this.getResolvedCall(bindingContext)?.resultingDescriptor

@Rewritten
fun KtExpression.getCalleeOrUnwrappedGetMethod(): KaFunctionSymbol? = analyze {
    (this@getCalleeOrUnwrappedGetMethod as? KtDotQualifiedExpression)?.let { dotQualifiedExpression ->
        (dotQualifiedExpression.selectorExpression as? KtNameReferenceExpression)?.let { nameSelector ->
            (nameSelector.mainReference.resolveToSymbol() as? KaPropertySymbol)?.getter
        }
    } ?: this@getCalleeOrUnwrappedGetMethod.resolveToCall()?.successfulFunctionCallOrNull()
        ?.partiallyAppliedSymbol?.symbol
}

/**
 * Checks whether the expression is a call, matches the FunMatchers in [STRING_TO_BYTE_FUNS] and is called on a constant string value.
 */
fun KtExpression.isBytesInitializedFromString(bindingContext: BindingContext) =
    getCalleeOrUnwrappedGetMethod(bindingContext)?.let { callee ->
        STRING_TO_BYTE_FUNS.any { it.matches(callee) } &&
                (getCall(bindingContext)?.explicitReceiver as? ExpressionReceiver)?.expression
                    ?.predictRuntimeStringValue() != null
    } ?: false

@Rewritten
fun KtExpression.isBytesInitializedFromString() = analyze {
    val resolveToCall = this@isBytesInitializedFromString.resolveToCall()

    val functionCall = resolveToCall?.successfulFunctionCallOrNull()
    if (functionCall != null) {
        return STRING_TO_BYTE_FUNS.any { it.matches(functionCall) }
                && ((functionCall.partiallyAppliedSymbol.extensionReceiver
            ?: functionCall.partiallyAppliedSymbol.dispatchReceiver) as? KaExplicitReceiverValue)?.expression
            ?.predictRuntimeStringValue() != null
    }
    resolveToCall?.singleVariableAccessCall()?.let { callee ->
        STRING_BYTES.matches(callee) &&
                ((callee.partiallyAppliedSymbol.extensionReceiver
                    ?: callee.partiallyAppliedSymbol.dispatchReceiver)
                as? KaExplicitReceiverValue)?.expression
            ?.predictRuntimeStringValue() != null
    } ?: false
}

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
fun KtExpression.isInitializedPredictably(searchStartNode: KtExpression, bindingContext: BindingContext): Boolean {
    return this !is KtNameReferenceExpression || this.findUsages(searchStartNode) {
        it.getParentOfType<KtCallExpression>(false).getResolvedCall(bindingContext) matches SECURE_RANDOM_FUNS
    }.isEmpty()
}

@Rewritten
fun KtExpression.isInitializedPredictably(searchStartNode: KtExpression): Boolean {
    return this !is KtNameReferenceExpression || this.findUsages(searchStartNode) {
        analyze {
            it.getParentOfType<KtCallExpression>(false)?.resolveToCall()
                ?.successfulFunctionCallOrNull() matches SECURE_RANDOM_FUNS
        }
    }.isEmpty()
}

/**
 * Checks if an expression is a function local variable
 */
@Deprecated("", replaceWith = ReplaceWith("isLocalVariable()"))
fun KtExpression?.isLocalVariable(bindingContext: BindingContext) =
    (this is KtNameReferenceExpression) && (bindingContext[BindingContext.REFERENCE_TARGET, this] is LocalVariableDescriptor)

// https://googlesamples.github.io/android-custom-lint-rules/api-guide.html#astanalysis/kotlinanalysisapi
// https://kotlin.github.io/analysis-api/migrating-from-k1.html#-6zwegf_221
fun KtExpression?.isLocalVariable(): Boolean {
    if (this !is KtNameReferenceExpression) return false
    val expression = this
    analyze {
        return expression.mainReference.resolveToSymbol() is KaLocalVariableSymbol
    }
}

fun KtExpression?.setterMatches(
    bindingContext: BindingContext,
    propertyName: String,
    matcher: FunMatcherImpl
): Boolean = when (this) {
    is KtNameReferenceExpression -> (getReferencedName() == propertyName) &&
            (matcher.matches((bindingContext[BindingContext.REFERENCE_TARGET, this] as? PropertyDescriptor)?.unwrappedSetMethod))

    is KtQualifiedExpression -> selectorExpression.setterMatches(bindingContext, propertyName, matcher)
    else -> false
}

@Rewritten
fun KtExpression?.setterMatches(
    propertyName: String,
    matcher: FunMatcherImpl
): Boolean = analyze {
    when (this@setterMatches) {
        is KtNameReferenceExpression -> (getReferencedName() == propertyName) &&
                (matcher.matches(this@setterMatches.resolveToCall()?.successfulVariableAccessCall() ?: return false))

        is KtQualifiedExpression -> selectorExpression.setterMatches(propertyName, matcher)
        else -> false
    }
}


fun KtExpression?.getterMatches(
    bindingContext: BindingContext,
    propertyName: String,
    matcher: FunMatcherImpl
): Boolean = when (this) {
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
    this.operationReference.operationSignTokenType?.let { OperatorConventions.BINARY_OPERATION_NAMES[it] }
        ?.asString() == "plus"

fun PsiElement?.getVariableType(bindingContext: BindingContext) =
    this?.let { bindingContext[BindingContext.VARIABLE, it]?.type }

fun KtTypeReference?.getType(bindingContext: BindingContext): KotlinType? =
    this?.let { bindingContext[BindingContext.TYPE, it] }

fun KtClassOrObject.hasExactlyOneFunctionAndNoProperties(): Boolean {
    var functionCount = 0
    return declarations.all {
        it !is KtProperty && (it !is KtNamedFunction || functionCount++ == 0)
    } && functionCount > 0
}

fun KotlinType.isFunctionalInterface(): Boolean =
    (constructor.declarationDescriptor as? ClassDescriptor)?.let(::getSingleAbstractMethodOrNull) != null

fun KotlinFileContext.merge(firstElement: PsiElement, lastElement: PsiElement) =
    merge(listOf(textRange(firstElement), textRange(lastElement)))

fun KotlinType.simpleName(): String = getKotlinTypeFqName(false).substringAfterLast(".")

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

fun KtQualifiedExpression?.determineSignature(bindingContext: BindingContext): DeclarationDescriptor? =
    when (val selectorExpr = this?.selectorExpression) {
        is KtCallExpression -> bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr.getCallNameExpression()]
        is KtSimpleNameExpression -> bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr]
        else -> null
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
fun KtWhenExpression.isExhaustive(context: KotlinFileContext): Boolean = analyze {
    return entries.any { it.isElse } || this@isExhaustive.computeMissingCases().isEmpty()
//    return entries.any { it.isElse } || context.bindingContext[BindingContext.EXHAUSTIVE_WHEN, this] == true
}

val PropertyDescriptor.unwrappedGetMethod: FunctionDescriptor?
    get() = if (this is SyntheticPropertyDescriptor) this.getMethod else getter

val PropertyDescriptor.unwrappedSetMethod: FunctionDescriptor?
    get() = if (this is SyntheticPropertyDescriptor) this.setMethod else setter
