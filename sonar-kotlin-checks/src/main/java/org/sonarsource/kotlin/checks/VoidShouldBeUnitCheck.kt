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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.isAbstract
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message
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
        if (typeReference.type.isClassType(voidClassId) &&
            !isATypeArgumentOfAnInheritableClass(typeReference) &&
            !isInOverrideOrAbstractFunction(typeReference)
        ) {
            kotlinFileContext.reportIssue(
                typeReference,
                message
            )
        }
    }
}

private fun isATypeArgumentOfAnInheritableClass(typeReference: KtTypeReference): Boolean {
    // The idea is to filter out classes or interfaces, parametrized with <Void>,
    // that could be extended from Java. As usage of Void is justified due to this issue:
    // https://youtrack.jetbrains.com/issue/KT-15964
    return typeReference.getParentOfType<KtTypeArgumentList>(true)
        ?.let { _ -> typeReference.getParentOfType<KtClass>(true) }
        ?.run { isInterface() || hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.ABSTRACT_KEYWORD) }
        ?: false
}

/**
 * Check if the Void type reference is inside a signature of an override or abstract function.
 * This accounts for cases when interop with Java is needed and functions must match the parent's signature,
 * e.g. functions returning `Mono<Void>`.
 */
private fun isInOverrideOrAbstractFunction(typeReference: KtTypeReference) = typeReference
    // Get function declaration, stopping at block expressions to filter out function-local declarations with Void type
    .getParentOfType<KtNamedFunction>(strict = true, KtBlockExpression::class.java)
    ?.run { hasModifier(KtTokens.OVERRIDE_KEYWORD) || isAbstract() }
    ?: false
