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

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import java.util.stream.Collectors

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
        val nameIdentifier = function.nameIdentifier ?: return
        val complexity = CognitiveComplexity(function)
        val value = complexity.value()
        if (value > threshold) {
            context.reportIssue(
                nameIdentifier,
                "Refactor this method to reduce its Cognitive Complexity from $value to the $threshold allowed.",
                secondaryLocations = complexity.increments().stream()
                    .map { increment: CognitiveComplexity.Increment -> secondaryLocation(increment, context) }
                    .collect(Collectors.toList()),
                gap = value.toDouble() - threshold,
            )
        }
    }

    private fun secondaryLocation(increment: CognitiveComplexity.Increment, context: KotlinFileContext): SecondaryLocation {
        val nestingLevel = increment.nestingLevel
        var message = "+" + (nestingLevel + 1)
        if (nestingLevel > 0) {
            message += " (incl $nestingLevel for nesting)"
        }
        val textRange = context.textRange(increment.token)
        return SecondaryLocation(textRange, message)
    }
}
