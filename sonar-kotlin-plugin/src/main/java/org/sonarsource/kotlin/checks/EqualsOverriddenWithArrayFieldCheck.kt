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

private val EQUALS_MATCHER = FunMatcher(name = "equals", returnType = "kotlin.Boolean", arguments = listOf(listOf(ANY)))
private val HASH_CODE_MATCHER = FunMatcher(name = "hashCode", returnType = "kotlin.Int")
private val TO_STRING_MATCHER = FunMatcher(name = "toString", returnType = "kotlin.String")

@Rule(key = "S6218")
class EqualsOverriddenWithArrayFieldCheck : AbstractCheck() {
    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        if (!klass.isData() || !klass.hasAnArrayProperty(context.bindingContext)) {
            return
        }
        val functions = klass.collectOverridingFunctions()
        val missingFunctionNames = mutableListOf<String>()
        if (!functions.any { EQUALS_MATCHER.matches(it, context.bindingContext) }) {
            missingFunctionNames.add("equals")
        }
        if (!functions.any { HASH_CODE_MATCHER.matches(it, context.bindingContext) }) {
            missingFunctionNames.add("hashCode")
        }
        if (!functions.any { TO_STRING_MATCHER.matches(it, context.bindingContext) }) {
            missingFunctionNames.add("toString")
        }
        val message = when (missingFunctionNames.size) {
            1 -> "Override ${missingFunctionNames[0]} to consider array content in the method."
            2 -> "Override ${missingFunctionNames[0]} and ${missingFunctionNames[1]} to consider array content in the method."
            3 -> "Override ${missingFunctionNames[0]}, ${missingFunctionNames[1]} and ${missingFunctionNames[2]} to consider array content in the method."
            else -> return
        }
        context.reportIssue(klass.nameIdentifier!!, message)
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
