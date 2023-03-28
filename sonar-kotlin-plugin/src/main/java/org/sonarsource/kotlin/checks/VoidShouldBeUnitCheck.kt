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

import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.determineType
import org.sonarsource.kotlin.api.isAbstract
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = """Replace this usage of "Void" type with "Unit"."""

@Rule(key = "S6508")
class VoidShouldBeUnitCheck : AbstractCheck() {

    override fun visitTypeReference(typeReference: KtTypeReference, kotlinFileContext: KotlinFileContext) {

        if (typeReference.isVoidTypeRef(kotlinFileContext.bindingContext) &&
            !typeReference.isInheritedType() &&
            !isATypeArgumentOfAnInheritableClass(typeReference)
        ) {
            kotlinFileContext.reportIssue(
                typeReference,
                MESSAGE
            )
        }
    }

    // As type aliases are resolved lazily, we can't rely on 'determineType()' function.
    // So we check argument types of a typealias separately
    override fun visitTypeAlias(typeAlias: KtTypeAlias, kotlinFileContext: KotlinFileContext) {

        val ktTypeReferences = typeAlias.collectDescendantsOfType<KtTypeReference>()

        kotlinFileContext.bindingContext.get(BindingContext.TYPE_ALIAS, typeAlias)
            ?.underlyingType
            ?.flattenTypeArguments()
            ?.forEachIndexed { i, type ->
                if (type.isJavaLangVoid()) {
                    kotlinFileContext.reportIssue(
                        ktTypeReferences[i],
                        MESSAGE
                    )
                }
            }
    }
}

private fun KotlinType.flattenTypeArguments(): Sequence<KotlinType> =
     arguments.asSequence().flatMap { it.type.flattenTypeArguments() } + sequenceOf(this)

private fun KtTypeReference.isVoidTypeRef(bindingContext: BindingContext) = determineType(bindingContext).isJavaLangVoid()

private fun KotlinType?.isJavaLangVoid() = this?.getJetTypeFqName(false) == "java.lang.Void"

private fun isATypeArgumentOfAnInheritableClass(typeReference: KtTypeReference) : Boolean {
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
