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

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.isAnonymous
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S1172")
class UnusedFunctionParameterCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        if (!shouldBeChecked(function)) return
        val unusedParameters = function.getUnusedParameters()
        val singleMessage =
            if (function.isAnonymous()) """Use "_" instead of this unused function parameter"""
            else "Remove this unused function parameter"
        val pluralMessage =
            if (function.isAnonymous()) """Use "_" instead of these unused function parameters."""
            else "Remove these unused function parameters."
        if (unusedParameters.isNotEmpty()) reportUnusedParameters(context, unusedParameters, singleMessage, pluralMessage)
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, context: KotlinFileContext) {
        val unusedParameters = expression.getUnusedParameters()
        val singleMessage = """Use "_" instead of this unused lambda parameter"""
        val pluralMessage = """Use "_" instead of these unused lambda parameters."""
        if (unusedParameters.isNotEmpty()) reportUnusedParameters(context, unusedParameters, singleMessage, pluralMessage)
    }

    private fun reportUnusedParameters(
        context: KotlinFileContext,
        unusedParameters: List<KtNamedDeclaration>,
        singleMessage: String,
        pluralMessage: String,
    ) {
        val firstUnused = unusedParameters[0]
        val secondaryLocations = unusedParameters.asSequence()
            .map { unusedParameter: KtNamedDeclaration ->
                SecondaryLocation(context.textRange(unusedParameter.nameIdentifier!!),
                    """$singleMessage "${unusedParameter.name}".""")
            }
            .toList()
        if (unusedParameters.size > 1) context.reportIssue(firstUnused.nameIdentifier!!, pluralMessage, secondaryLocations)
        else  context.reportIssue(firstUnused.nameIdentifier!!, """$singleMessage "${firstUnused.name}".""")
    }
}

private fun shouldBeChecked(function: KtNamedFunction) =
    function.hasBody()
        && (function.isTopLevel || function.isPrivate() || function.isAnonymous())

private fun KtNamedFunction.getUnusedParameters(): List<KtParameter> =
    valueParameters.asSequence()
        .filter { it.notUsedIn(this) }
        .toList()

private fun KtLambdaExpression.getUnusedParameters(): List<KtNamedDeclaration> =
    valueParameters.asSequence()
        .flatMap { it.unusedParametersList(this) }
        .toList()

private fun KtNamedDeclaration.notUsedIn(ktElement: KtElement) =
    name != "_"
        && !ktElement.anyDescendantOfType<KtNameReferenceExpression> { it.getReferencedName() == name }

private fun KtParameter.unusedParametersList(ktElement: KtElement) =
    when {
        destructuringDeclaration != null -> destructuringDeclaration!!.entries.filter { it.notUsedIn(ktElement) }
        notUsedIn(ktElement) -> listOf(this)
        else -> emptyList<KtNamedDeclaration>()
    }
