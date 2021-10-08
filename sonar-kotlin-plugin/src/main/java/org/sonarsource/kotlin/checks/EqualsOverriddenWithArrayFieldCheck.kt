/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.sonarsource.kotlin.api.ANY
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.determineTypeAsString
import org.sonarsource.kotlin.api.overrides
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val EXPECTED_OVERRIDES = listOf(
    FunMatcher(name = "equals", returnType = "kotlin.Boolean", arguments = listOf(listOf(ANY))),
    FunMatcher(name = "hashCode", returnType = "kotlin.Int"),
    FunMatcher(name = "toString", returnType = "kotlin.String")
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
        if (oneParameterIsAnArray) {
            return true
        }
        val body = this.body ?: return false
        return body.properties.any { it.isAnArray(bindingContext) }
    }

    private fun KtClass.buildIssueMessage(bindingContext: BindingContext): String? {
        val functions = this.collectOverridingFunctions()
        val missingFunctionNames = mutableListOf<String>()
        for (matcher in EXPECTED_OVERRIDES) {
            if (!functions.any { matcher.matches(it, bindingContext) }) {
                missingFunctionNames.add(matcher.names.first())
            }
        }
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

    private fun KtParameter.isAnArray(bindingContext: BindingContext): Boolean {
        val type = this.determineTypeAsString(bindingContext, printTypeArguments = false) ?: return false
        return type == "kotlin.Array"
    }

    private fun KtProperty.isAnArray(bindingContext: BindingContext): Boolean {
        val type = this.determineTypeAsString(bindingContext, printTypeArguments = false) ?: return false
        return type == "kotlin.Array"
    }
}
