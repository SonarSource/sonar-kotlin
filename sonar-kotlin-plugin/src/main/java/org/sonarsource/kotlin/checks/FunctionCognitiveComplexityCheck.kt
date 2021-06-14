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

import java.util.stream.Collectors
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.FunctionCognitiveComplexityCheck]
 */
@Rule(key = "S3776")
class FunctionCognitiveComplexityCheck : AbstractCheck() {
    companion object {
        private const val DEFAULT_THRESHOLD = 15
    }

    @RuleProperty(key = "threshold",
        description = "The maximum authorized complexity.",
        defaultValue = "" + DEFAULT_THRESHOLD)
    var threshold = DEFAULT_THRESHOLD

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        if (function.receiverTypeReference != null) {
            /** see [org.sonarsource.kotlin.converter.KotlinTreeVisitor.createFunctionDeclarationTree] */
            return
        }
        val nameIdentifier = function.nameIdentifier ?: return
        val complexity = CognitiveComplexity(function)
        val value = complexity.value()
        if (value > threshold) {
            val document = context.ktFile.viewProvider.document!!
            context.reportIssue(
                nameIdentifier,
                "Refactor this method to reduce its Cognitive Complexity from $value to the $threshold allowed.",
                secondaryLocations = complexity.increments().stream()
                    .map { increment: CognitiveComplexity.Increment -> secondaryLocation(increment, document) }
                    .collect(Collectors.toList()),
                gap = value.toDouble() - threshold,
            )
        }
    }

    private fun secondaryLocation(increment: CognitiveComplexity.Increment, document: Document): SecondaryLocation {
        val nestingLevel = increment.nestingLevel
        var message = "+" + (nestingLevel + 1)
        if (nestingLevel > 0) {
            message += " (incl $nestingLevel for nesting)"
        }
        val textRange = KotlinTextRanges.textRange(document, increment.token)
        return SecondaryLocation(textRange, message)
    }
}
