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

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.ArgumentMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.determineTypeAsString
import org.sonarsource.kotlin.api.overrides
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val EXPECTED_OVERRIDES = listOf(
    FunMatcher(name = "equals", returnType = "kotlin.Boolean", arguments = listOf(listOf(ArgumentMatcher("kotlin.Any")))),
    FunMatcher(name = "hashCode", returnType = "kotlin.Int", arguments = listOf(emptyList())),
    FunMatcher(name = "toString", returnType = "kotlin.String", arguments = listOf(emptyList()))
)

@Rule(key = "S6218")
class EqualsOverriddenWithArrayFieldCheck : AbstractCheck() {
    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        if (!klass.isData() || !klass.hasAnArrayProperty(context.bindingContext)) {
            return
        }
        klass.buildIssueMessage(context.bindingContext)?.let { context.reportIssue(klass.nameIdentifier!!, it) }
    }

    private fun KtClass.hasAnArrayProperty(bindingContext: BindingContext): Boolean {
        // Because we only call this function on data classes, we can assume they have constructor
        val constructor = this.findDescendantOfType<KtPrimaryConstructor>()!!
        val oneParameterIsAnArray = constructor.valueParameters.any { it.isAnArray(bindingContext) }
        return if (oneParameterIsAnArray) {
            true
        } else {
            this.body?.properties?.any { it.isAnArray(bindingContext) } ?: false
        }
    }

    private fun KtClass.buildIssueMessage(bindingContext: BindingContext): String? {
        val functions = this.collectOverridingFunctions()
        val missingFunctionNames = EXPECTED_OVERRIDES
            .filter { matcher -> !functions.any { function -> matcher.matches(function, bindingContext) } }
            .map { it.names.first() }
        return when (missingFunctionNames.size) {
            1 -> "Override ${missingFunctionNames[0]} to consider array content in the method."
            2 -> "Override ${missingFunctionNames[0]} and ${missingFunctionNames[1]} to consider array content in the method."
            3 -> "Override ${missingFunctionNames[0]}, ${missingFunctionNames[1]} and ${missingFunctionNames[2]} to consider array content in the method."
            else -> null
        }
    }

    private fun KtClass.collectOverridingFunctions(): List<KtNamedFunction> =
        this.collectDescendantsOfType<KtNamedFunction>()
            .filter { it.overrides() }

    private fun KtParameter.isAnArray(bindingContext: BindingContext): Boolean =
        this.determineTypeAsString(bindingContext, printTypeArguments = false) == "kotlin.Array"

    private fun KtProperty.isAnArray(bindingContext: BindingContext): Boolean =
        this.determineTypeAsString(bindingContext, printTypeArguments = false) == "kotlin.Array"
}
