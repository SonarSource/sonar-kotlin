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

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.suspendModifier
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6313")
class ViewModelSuspendingFunctionsCheck : AbstractCheck() {
    private val viewModelClassId = ClassId.fromString("androidx/lifecycle/ViewModel")

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        function.suspendModifier()?.let {
            if (!function.isPrivate()
                && function.extendsViewModel()
            ) {
                kotlinFileContext.reportIssue(it,
                    """Classes extending "ViewModel" should not expose suspending functions.""")
            }
        }
    }

    private fun KtNamedFunction.extendsViewModel(): Boolean = withKaSession {
        val containingSymbol = symbol.containingSymbol
        if (containingSymbol !is KaClassSymbol) return false
        return containingSymbol.superTypes.any { it.isClassType(viewModelClassId) }
    }
}
