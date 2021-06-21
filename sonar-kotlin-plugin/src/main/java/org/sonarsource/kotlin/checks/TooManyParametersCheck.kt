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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.kotlin.checks.TooManyParametersKotlinCheck]
 */
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
        if (function.receiverTypeReference != null) {
            /** see [org.sonarsource.kotlin.converter.KotlinTreeVisitor.createFunctionDeclarationTree] */
            return
        }
        if (function.valueParameters.size > max
            && !function.hasModifier(KtTokens.OVERRIDE_KEYWORD)
            && function.annotationEntries.none { exceptionsList.contains(it.shortName?.asString()) }
        ) {
            val document = function.containingKtFile.viewProvider.document!!
            kotlinFileContext.reportIssue(
                /** see [org.sonarsource.slang.impl.FunctionDeclarationTreeImpl.rangeToHighlight] */
                function.nameIdentifier ?: function,
                "This function has ${function.valueParameters.size} parameters, which is greater than the $max authorized.",
                secondaryLocations = function.valueParameters.asSequence()
                    .drop(max)
                    .map { SecondaryLocation(KotlinTextRanges.textRange(document, it), null) }
                    .toList(),
            )
        }
    }

}
