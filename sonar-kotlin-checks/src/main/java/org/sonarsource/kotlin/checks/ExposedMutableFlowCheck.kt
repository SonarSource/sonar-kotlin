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

import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.determineTypeAsString
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val DISALLOWED_TYPES = listOf(
    "kotlinx.coroutines.flow.MutableSharedFlow",
    "kotlinx.coroutines.flow.MutableStateFlow",
)

private const val MESSAGE = "Don't expose mutable flow types."

@Rule(key = "S6305")
class ExposedMutableFlowCheck : AbstractCheck() {
    override fun visitProperty(property: KtProperty, kotlinFileContext: KotlinFileContext) {
        if (isEligible(property) && property.determineTypeAsString() in DISALLOWED_TYPES) {
            kotlinFileContext.reportIssue(property, MESSAGE)
        }
    }

    override fun visitParameter(parameter: KtParameter, kotlinFileContext: KotlinFileContext) {
        if (isEligible(parameter) && parameter.determineTypeAsString() in DISALLOWED_TYPES) {
            kotlinFileContext.reportIssue(parameter, MESSAGE)
        }
    }
}

private fun isEligible(declaration: KtDeclaration) = !declaration.isProtected() && !declaration.isPrivate() &&
    (declaration.isTopLevelKtOrJavaMember() || declaration.containingClassOrObject != null)
