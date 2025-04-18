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

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.ArgumentMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.determineTypeAsString
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val EXPECTED_OVERRIDES = listOf(
    FunMatcher(name = "equals", returnType = "kotlin.Boolean") {
        withArguments(ArgumentMatcher("kotlin.Any"))
    },
    FunMatcher(name = "hashCode", returnType = "kotlin.Int") {
        withNoArguments()
    }
)

@Rule(key = "S6218")
class EqualsOverriddenWithArrayFieldCheck : AbstractCheck() {
    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        if (!klass.isData() || !klass.hasAnArrayProperty()) {
            return
        }
        klass.buildIssueMessage()?.let { context.reportIssue(klass.nameIdentifier!!, it) }
    }

    private fun KtClass.hasAnArrayProperty(): Boolean {
        // Because we only call this function on data classes, we can assume they have constructor
        val constructor = this.findDescendantOfType<KtPrimaryConstructor>()!!
        val oneParameterIsAnArray = constructor.valueParameters.any { it.isAnArray() }
        return if (oneParameterIsAnArray) {
            true
        } else {
            this.body?.properties?.any { it.isAnArray() } ?: false
        }
    }

    private fun KtClass.buildIssueMessage(): String? {
        val functions = this.collectOverridingFunctions()
        val missingFunctionNames = EXPECTED_OVERRIDES
            .filter { matcher -> !functions.any { function -> matcher.matches(function) } }
            .map { it.names.first() }
        return when (missingFunctionNames.size) {
            1 -> "Override ${missingFunctionNames[0]} to consider array content in the method."
            2 -> "Override ${missingFunctionNames[0]} and ${missingFunctionNames[1]} to consider array content in the method."
            else -> null
        }
    }

    private fun KtClass.collectOverridingFunctions(): List<KtNamedFunction> =
        this.collectDescendantsOfType<KtNamedFunction>()
            .filter { it.overrides() }

    private fun KtParameter.isAnArray(): Boolean =
        this.determineTypeAsString() == "kotlin.Array"

    private fun KtProperty.isAnArray(): Boolean =
        this.determineTypeAsString() == "kotlin.Array"
}
