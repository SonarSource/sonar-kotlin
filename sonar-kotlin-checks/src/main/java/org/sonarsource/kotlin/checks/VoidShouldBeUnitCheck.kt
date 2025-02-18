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

import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.getType
import org.sonarsource.kotlin.api.checks.isAbstract
import org.sonarsource.kotlin.api.reporting.message
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val message = message {
    +"Replace this usage of "
    code("Void")
    +" type with "
    code("Unit")
    +"."
}

@Rule(key = "S6508")
class VoidShouldBeUnitCheck : AbstractCheck() {
    private val voidClassId = ClassId.fromString("java/lang/Void")

    override fun visitTypeReference(typeReference: KtTypeReference, kotlinFileContext: KotlinFileContext) = withKaSession {
        if (typeReference.getType().isClassType(voidClassId) &&
            !typeReference.isInheritedType() &&
            !isATypeArgumentOfAnInheritableClass(typeReference)
        ) {
            kotlinFileContext.reportIssue(
                typeReference,
                message
            )
        }
    }

    // As type aliases are resolved lazily, we can't rely on 'determineType()' function.
    // So we check argument types of a typealias separately
    /**
     * TODO remove after switch to K2 where this case is handled by [visitTypeReference],
     * in K1 for some reason type of [KtTypeAlias.getTypeReference] is [org.jetbrains.kotlin.analysis.api.types.KaErrorType]
     */
    @Deprecated("use K2 instead")
    override fun visitTypeAlias(typeAlias: KtTypeAlias, kotlinFileContext: KotlinFileContext) {

        val ktTypeReferences =
            flattenTypeRefs(typeAlias.getTypeReference().nonNullTypeArguments() ?: return)

        kotlinFileContext.bindingContext[BindingContext.TYPE_ALIAS, typeAlias]
            ?.underlyingType
            ?.flattenTypeArguments()
            ?.forEachIndexed { i, type ->
                if (type.isJavaLangVoid()) {
                    kotlinFileContext.reportIssue(
                        ktTypeReferences[i],
                        message
                    )
                }
            }
    }
}

private tailrec fun flattenTypeRefs(
    typeRefs: List<KtTypeReference?>,
    acc: MutableList<KtTypeReference> = mutableListOf()
): List<KtTypeReference> =
    if (typeRefs.isEmpty()) acc
    else flattenTypeRefs(
        typeRefs = typeRefs.flatMap { it.nonNullTypeArguments() ?: emptyList() },
        acc = acc.apply { addAll(typeRefs.filterNotNull()) })

private fun KtTypeReference?.nonNullTypeArguments() = this?.typeElement?.typeArgumentsAsTypes?.filterNotNull()

private tailrec fun flattenTypeProjections(
    typeProjections: List<TypeProjection>,
    acc: MutableList<TypeProjection> = mutableListOf()
): List<TypeProjection> =
    if (typeProjections.isEmpty()) acc
    else flattenTypeProjections(
        typeProjections = typeProjections.flatMap { it.type.arguments.withoutStarProjection() },
        acc = acc.apply { addAll(typeProjections.withoutStarProjection()) })

private fun List<TypeProjection>.withoutStarProjection() = filter { projection -> projection !is StarProjectionImpl }

private fun KotlinType.flattenTypeArguments(): List<KotlinType> = flattenTypeProjections(arguments).map { it.type }

private fun KotlinType?.isJavaLangVoid() = this?.getKotlinTypeFqName(false) == "java.lang.Void"

private fun isATypeArgumentOfAnInheritableClass(typeReference: KtTypeReference): Boolean {
    // The idea is to filter out classes or interfaces, parametrized with <Void>,
    // that could be extended from Java. As usage of Void is justified due to this issue:
    // https://youtrack.jetbrains.com/issue/KT-15964
    return typeReference.getParentOfType<KtTypeArgumentList>(true)?.let { _ ->
        typeReference.getParentOfType<KtClass>(true)?.run {
            isInterface() || hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.ABSTRACT_KEYWORD)
        }
    } ?: false
}

private fun KtTypeReference.isInheritedType(): Boolean =
    with(parent) {
        this is KtNamedFunction && (hasModifier(KtTokens.OVERRIDE_KEYWORD) || isAbstract())
    }
