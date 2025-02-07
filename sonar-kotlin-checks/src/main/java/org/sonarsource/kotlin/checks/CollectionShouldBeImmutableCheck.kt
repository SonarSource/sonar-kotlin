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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val nonMutatingFunctions = FunMatcher {
    withDefiningSupertypes(
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.Map",
        // TODO all above seem useless because of below
        "kotlin.collections.Collection",
    )
    withNames(
        "size",
        "isEmpty",
        "contains",
        "containsAll",
        "get",
        "indexOf",
        "lastIndexOf",
        "containsKey",
        "containsValue",
    )
}

private val mutatingFunctions = FunMatcher {
    withDefiningSupertypes(
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableSet",
        "kotlin.collections.MutableMap",
        "kotlin.collections.MutableCollection",
    )
    withNames(
        "iterator",
        "add",
        "remove",
        "addAll",
        "removeAll",
        "retainAll",
        "clear",
        "listIterator",
        "removeAt",
        "set",
        "subList",
        "put",
        "putAll",
        "keys",
        "values",
        "entries",
    )
}

private val imMutableCollections =
    setOf(
        "kotlin.collections.Iterable",
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.Map",
        "kotlin.collections.Collection"
    )

private val mutatingTokes = setOf(KtTokens.PLUSEQ, KtTokens.MINUSEQ)

private val mutableCollections =
    setOf(
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableSet",
        "kotlin.collections.MutableMap",
        "kotlin.collections.MutableCollection"
    )

//@org.sonarsource.kotlin.api.frontend.K1only // "Rewritten, 1 FN due to the bug? in K2 analysis API implementation"
@Rule(key = "S6524")
class CollectionShouldBeImmutableCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) = withKaSession {
        if (
            function.isAbstract() ||
            function.overrides() ||
            function.isOpen() ||
            function.isExpect() ||
            function.isActual() ||
            !function.hasBody()
        ) return

        val mutableCollectionsParameters =
            function.valueParameters.filter {
                it.determineType()?.symbol?.classId?.asFqNameString() in mutableCollections
            }
        val mutableCollectionsVariables =
            function.collectDescendantsOfType<KtVariableDeclaration> {
                it.returnType.asFqNameString() in mutableCollections
            }

        mutableCollectionsParameters.filter { !it.isMutated(function) }.forEach { parameter ->
            kotlinFileContext.reportIssue(parameter, "Make this collection immutable.")
        }

        mutableCollectionsVariables.filter { !it.isMutated(function) }.forEach { parameter ->
            kotlinFileContext.reportIssue(parameter, "Make this collection immutable.")
        }

    }

    private fun KtCallableDeclaration.isMutated(function: KtNamedFunction): Boolean {
        val usages =
            function.collectDescendantsOfType<KtNameReferenceExpression> { ref -> ref.getReferencedName() == name }
        return usages.any {
            it.parent.skipParentParentheses().isMutatingUsage()
        }
    }

        private fun PsiElement?.isMutatingUsage(): Boolean {
            return when(this) {

                is KtDotQualifiedExpression -> withKaSession {

                    val resolveToCall = this@isMutatingUsage.resolveToCall()
                    val kaCallableMemberCall: KaCallableMemberCall<*,*> =
                        resolveToCall?.successfulFunctionCallOrNull() ?:
                        resolveToCall?.successfulVariableAccessCall() ?: return true

//                    if (mutatingFunctions.matches(kaCallableMemberCall)) return true
                    if (nonMutatingFunctions.matches(kaCallableMemberCall)) return false

                    val kaCallableSymbol = kaCallableMemberCall.partiallyAppliedSymbol.symbol
                    val receiverType = if (kaCallableSymbol.isExtension) {
                        kaCallableMemberCall.partiallyAppliedSymbol.signature.receiverType
                            ?.symbol?.classId?.asFqNameString()
                    } else {
//                        kaCallableSymbol.fakeOverrideOriginal
//                            .callableId?.classId?.asFqNameString()
                        null
                    }

                    return receiverType !in imMutableCollections
                }

                is KtValueArgument -> withKaSession {
                    val resolveToCall = parent.parentOfType<KtCallExpression>()?.resolveToCall()

                    val parameterIndex = (parent as? KtValueArgumentList)?.arguments?.indexOf(this@isMutatingUsage) ?: -1
                    if (parameterIndex < 0) {
                        false
                    } else {
                        val fqNameString = resolveToCall?.successfulFunctionCallOrNull()
                            ?.argumentMapping?.values?.toList()?.get(parameterIndex)
                            ?.returnType?.symbol?.classId?.asFqNameString()

                        fqNameString !in imMutableCollections
                    }
                }

                is KtArrayAccessExpression -> {
                    val preparent = parent.skipParentParentheses()
                    preparent is KtBinaryExpression && preparent.operationToken == KtTokens.EQ
                }

                is KtBinaryExpression -> {
                    operationToken in mutatingTokes
                }

                is KtUnaryExpression ->
                    baseExpression?.isMutatingUsage() ?: true

                is KtContainerNode ->
                    false

                else -> true
        }
    }
}
