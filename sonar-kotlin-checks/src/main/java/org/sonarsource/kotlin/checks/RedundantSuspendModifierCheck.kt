/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.checks.suspendModifier
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.visiting.analyze

@Rule(key = "S6318")
class RedundantSuspendModifierCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) = analyze {
        val suspendModifier = function.suspendModifier() ?: return
        with(function) {
            if (hasBody() && !overrides()) {
                findDescendantOfType<KtNameReferenceExpression> {
                    (it.resolveToCall()?.successfulFunctionCallOrNull()
                        ?.partiallyAppliedSymbol
                        ?.symbol as? KaNamedFunctionSymbol)?.isSuspend
                        ?: true
                } ?: context.reportIssue(suspendModifier, """Remove this unnecessary "suspend" modifier.""")
            }
        }
    }
}
