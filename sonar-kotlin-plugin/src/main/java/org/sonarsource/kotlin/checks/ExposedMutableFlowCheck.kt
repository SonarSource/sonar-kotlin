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

import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.determineTypeAsString
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val DISALLOWED_TYPES = listOf(
    "kotlinx.coroutines.flow.MutableSharedFlow",
    "kotlinx.coroutines.flow.MutableStateFlow",
)

private const val MESSAGE = "Don't expose mutable flow types."

@Rule(key = "S6305")
class ExposedMutableFlowCheck : AbstractCheck() {
    override fun visitProperty(property: KtProperty, kotlinFileContext: KotlinFileContext) {
        if (isEligible(property) && property.determineTypeAsString(kotlinFileContext.bindingContext) in DISALLOWED_TYPES) {
            kotlinFileContext.reportIssue(property, MESSAGE)
        }
    }

    override fun visitParameter(parameter: KtParameter, kotlinFileContext: KotlinFileContext) {
        if (isEligible(parameter) && parameter.determineTypeAsString(kotlinFileContext.bindingContext) in DISALLOWED_TYPES) {
            kotlinFileContext.reportIssue(parameter, MESSAGE)
        }
    }
}

private fun isEligible(declaration: KtDeclaration) = !declaration.isProtected() && !declaration.isPrivate() &&
    (declaration.isTopLevelKtOrJavaMember() || declaration.containingClassOrObject != null)
