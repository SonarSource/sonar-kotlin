/*
 * SonarSource SLang
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

import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.UnusedFunctionParameterCheck]
 */
@Rule(key = "S1172")
class UnusedFunctionParameterCheck : AbstractCheck() {

    companion object {
        private fun KtNamedFunction.getUnusedParameters(): List<KtParameter> =
            valueParameters.asSequence()
                .filter { ktParameter ->
                    !anyDescendantOfType<KtNameReferenceExpression> {
                        it.text == ktParameter.nameIdentifier?.text
                    }
                }.toList()
    }

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        if (!shouldBeChecked(function)) return
        val unusedParameters = function.getUnusedParameters()
        if (unusedParameters.isNotEmpty()) reportUnusedParameters(context, unusedParameters)
    }

    private fun reportUnusedParameters(context: KotlinFileContext, unusedParameters: List<KtParameter>) {
        val secondaryLocations = unusedParameters.asSequence()
            .map { unusedParameter: KtParameter ->
                SecondaryLocation(context.textRange(unusedParameter.nameIdentifier!!),
                    "Remove this unused method parameter ${unusedParameter.name}\".")
            }
            .toList()
        val firstUnused = unusedParameters[0]
        val msg = if (unusedParameters.size > 1) {
            "Remove these unused function parameters."
        } else {
            "Remove this unused function parameter \"${firstUnused.name}\"."
        }
        context.reportIssue(firstUnused.nameIdentifier!!, msg, secondaryLocations)
    }

    private fun shouldBeChecked(function: KtNamedFunction) =
        function.hasBody()
            && (function.isTopLevel || function.isPrivate())

}
