/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val GUAVA_OPTIONAL = "com.google.common.base.Optional"

private val REPLACEMENT_FUNCTIONS = mapOf(
    FunMatcher(qualifier = "com.google.common.base.Joiner", name = "join") {
        withArguments("kotlin.collections.MutableIterable")
    } to "Iterable<T>.joinToString",
    FunMatcher(qualifier = "com.google.common.base.Joiner", name = "join") { withArguments("kotlin.Array") }
        to "Array<T>.joinToString",
    FunMatcher(qualifier = "com.google.common.io.Files", name = "createTempDir") { withNoArguments() }
        to "kotlin.io.path.createTempDirectory",
    FunMatcher(qualifier = "com.google.common.collect.ImmutableSet", name = "of")
        to "kotlin.collections.setOf",
    FunMatcher(qualifier = "com.google.common.collect.ImmutableList", name = "of")
        to "kotlin.collections.listOf",
    FunMatcher(qualifier = "com.google.common.collect.ImmutableMap", name = "of")
        to "kotlin.collections.mapOf",
    FunMatcher(qualifier = "com.google.common.io.BaseEncoding") {
        withNames("base64", "base64Url")
        withNoArguments()
    } to "java.util.Base64",
    FunMatcher(qualifier = GUAVA_OPTIONAL, name = "of")
        to "java.util.Optional.of",
    FunMatcher(qualifier = GUAVA_OPTIONAL, name = "absent")
        to "java.util.Optional.empty",
    FunMatcher(qualifier = GUAVA_OPTIONAL, name = "fromNullable")
        to "java.util.Optional.ofNullable",
)

private val REPLACEMENT_TYPES = mapOf(
    "com.google.common.base.Predicate" to """Use "(T) -> Boolean" instead.""",
    "com.google.common.base.Function" to """Use "(T) -> R" instead.""",
    "com.google.common.base.Supplier" to """Use "() -> T" instead.""",
    GUAVA_OPTIONAL to """Use "java.util.Optional" instead.""",
)

@Rule(key = "S4738")
class ReplaceGuavaWithKotlinCheck : CallAbstractCheck() {

    override val functionsToVisit = REPLACEMENT_FUNCTIONS.keys

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val calleeExpression = callExpression.calleeExpression ?: return
        REPLACEMENT_FUNCTIONS
            .filter { it.key.matches(resolvedCall) }
            .forEach { kotlinFileContext.reportIssue(calleeExpression, """Use "${it.value}" instead.""") }
    }

    override fun visitParameter(parameter: KtParameter, ctx: KotlinFileContext) {
        parameter.typeReference?.let { typeReference ->
            typeReference.ifTypeReplacement { replacement ->
                if (typeReference.isInNonOverrideContext(ctx, parameter)) {
                    ctx.reportIssue(typeReference, replacement)
                }
            }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction, ctx: KotlinFileContext) {
        function.typeReference?.let { typeReference ->
            typeReference.ifTypeReplacement { replacement ->
                if (typeReference.isInNonOverrideContext(ctx)) {
                    ctx.reportIssue(typeReference, replacement)
                }
            }
        }
    }

    override fun visitProperty(property: KtProperty, ctx: KotlinFileContext) {
        property.typeReference?.let { typeReference ->
            typeReference.ifTypeReplacement { replacement ->
                ctx.reportIssue(typeReference, replacement)
            }
        }
    }

    private fun PsiElement?.isInNonOverrideContext(ctx: KotlinFileContext, parameter: KtParameter? = null): Boolean = when (this) {
        is KtNamedFunction -> this.hasBody() && !this.overrides()
        is KtClass -> !(parameter?.hasReferencesIn(this.getSuperTypeList()) ?: false)
        is KtTypeReference -> this.parent.isInNonOverrideContext(ctx, parameter)
        is KtParameter -> this.parent.isInNonOverrideContext(ctx, parameter)
        is KtParameterList -> this.parent.isInNonOverrideContext(ctx, parameter)
        is KtPrimaryConstructor -> this.parent.isInNonOverrideContext(ctx, parameter)
        else -> false
    }

    private fun KtParameter.hasReferencesIn(elementToVisit: KtElement?): Boolean = withKaSession {
        val variableSymbol = this@hasReferencesIn.symbol

        return elementToVisit?.collectDescendantsOfType<KtReferenceExpression>() { expression ->
            expression.mainReference.resolveToSymbol() == variableSymbol
        }?.isNotEmpty() ?: false
    }

    private fun KtTypeReference.ifTypeReplacement(action: (String) -> Unit) = withKaSession {
        this@ifTypeReplacement.type.symbol?.classId?.asFqNameString()
            ?.let { REPLACEMENT_TYPES[it]?.let(action) }
    }

}
