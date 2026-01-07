/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
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
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.psi2ir.deparenthesize
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

fun KtExpression.predictRuntimeStringValue() =
    predictRuntimeValueExpression().stringValue()

fun KtExpression.predictRuntimeStringValueWithSecondaries() = withKaSession {
    mutableListOf<PsiElement>().let {
        predictRuntimeValueExpression(it)
            .stringValue(it) to it
    }
}

fun KtExpression.predictRuntimeIntValue(): Int? = withKaSession {
    val valueExpression = predictRuntimeValueExpression()
    if (valueExpression.expressionType?.isIntType == true) {
        valueExpression.evaluate()?.value as? Int
    } else null
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

private fun KaFunctionCall<*>.predictValueExpression(): KtExpression? =
    if (GET_PROP_WITH_DEFAULT_MATCHER.matches(this)) {
        argumentMapping.keys.elementAt(1)
    } else null

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
private fun KtReferenceExpression.extractLetAlsoTargetExpression() =
    findReceiverScopeFunctionLiteral()?.findLetAlsoRunWithTargetExpression()

/**
 * Will try to resolve what `this` is an alias for inside a `with`, `run` or `apply` scope.
 */
private fun KaImplicitReceiverValue.extractWithRunApplyTargetExpression(
    startNode: PsiElement
) =
    findReceiverScopeFunctionLiteral(startNode)?.findLetAlsoRunWithTargetExpression()

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

private fun KtReferenceExpression.findReceiverScopeFunctionLiteral(): KtFunctionLiteral? = withKaSession {
    when (val resolvedSymbol = this@findReceiverScopeFunctionLiteral.mainReference.resolveToSymbol()) {
        is KaValueParameterSymbol -> resolvedSymbol.containingSymbol
            ?.findFunctionLiteral(this@findReceiverScopeFunctionLiteral)
        else -> null
    }
}

private fun KaImplicitReceiverValue.findReceiverScopeFunctionLiteral(
    startNode: PsiElement,
): KtFunctionLiteral? =
    symbol.findFunctionLiteral(startNode)

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

private fun KtProperty.determineType(): KaType? = withKaSession {
    typeReference?.type ?: initializer?.expressionType
}

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

private fun KtParameter.determineType(): KaType? = withKaSession {
    typeReference?.type
}

fun KtParameter.determineTypeAsString(): String? = withKaSession {
    determineType()?.asFqNameString()
}

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

fun KtExpression.isPredictedNull() =
   predictRuntimeValueExpression().isNull()

/**
 * Checks whether the expression is a call, matches the FunMatchers in [STRING_TO_BYTE_FUNS] and is called on a constant string value.
 */
fun KtExpression.isBytesInitializedFromString(): Boolean = withKaSession {
    val call = this@isBytesInitializedFromString.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
        ?: return@withKaSession false
    STRING_TO_BYTE_FUNS.any { it.matches(call) } &&
            ((call.partiallyAppliedSymbol.extensionReceiver ?: call.partiallyAppliedSymbol.dispatchReceiver)
                    as? KaExplicitReceiverValue)
                ?.expression
                ?.predictRuntimeStringValue() != null
}

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
fun KtExpression?.isLocalVariable(): Boolean = withKaSession {
    if (this@isLocalVariable !is KtNameReferenceExpression) return false
    return mainReference.resolveToSymbol() is KaLocalVariableSymbol
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

fun KtClassOrObject.hasExactlyOneFunctionAndNoProperties(): Boolean {
    var functionCount = 0
    return declarations.all {
        it !is KtProperty && (it !is KtNamedFunction || functionCount++ == 0)
    } && functionCount > 0
}

fun KotlinFileContext.merge(firstElement: PsiElement, lastElement: PsiElement) =
    merge(listOf(textRange(firstElement), textRange(lastElement)))

/**
 * See examples of [org.jetbrains.kotlin.analysis.api.types.KaFlexibleType]
 * (aka [platform type](https://kotlin.github.io/analysis-api/kaflexibletype.html)) in
 * [KtNamedFunction.returnTypeAsString] and [KtProperty.determineTypeAsString].
 */
fun KaType.simpleName(): String? = withKaSession {
    return lowerBoundIfFlexible().symbol?.name?.asString()
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
