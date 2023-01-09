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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S107")
class TooManyParametersCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX = 7
    }

    @RuleProperty(
        key = "Max",
        description = "Maximum authorized number of parameters",
        defaultValue = "" + DEFAULT_MAX)
    var max = DEFAULT_MAX

    private val exceptionsList = listOf(
        "RequestMapping",
        "GetMapping",
        "PostMapping",
        "PutMapping",
        "DeleteMapping",
        "PatchMapping",
        "JsonCreator")

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        if (function.valueParameters.size > max
            && !function.hasModifier(KtTokens.OVERRIDE_KEYWORD)
            && function.annotationEntries.none { exceptionsList.contains(it.shortName?.asString()) }
        ) {
            report(
                /** see [org.sonarsource.slang.impl.FunctionDeclarationTreeImpl.rangeToHighlight] */
                function.nameIdentifier ?: function,
                function.valueParameters,
                kotlinFileContext,
            )
        }
    }

    override fun visitFunctionType(type: KtFunctionType, kotlinFileContext: KotlinFileContext) {
        if (type.parameters.size > max) {
            report(type, type.parameters, kotlinFileContext)
        }
    }

    private fun report(
        reportingLocation: PsiElement,
        parameters: List<KtParameter>,
        kotlinFileContext: KotlinFileContext,
    ) {
        kotlinFileContext.reportIssue(
            reportingLocation,
            "This function has ${parameters.size} parameters, which is greater than the $max authorized.",
            secondaryLocations = parameters.asSequence()
                .drop(max)
                .map {
                    SecondaryLocation(kotlinFileContext.textRange(it), null)
                }
                .toList(),
        )
    }
}
