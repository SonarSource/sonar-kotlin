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
package org.sonarsource.kotlin.metrics

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.kotlin.api.checks.annotatedElement
import org.sonarsource.kotlin.api.checks.asString
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.api.visiting.KtTreeVisitor

private val SUPPRESSION_ANNOTATION_NAMES = listOf("Suppress", "SuppressWarnings")

// Common Suppress annotation parameter used by kotlin compiler.
private val COMPILER_KEY_TO_SONAR_KEYS = mapOf(
    "UNUSED_PARAMETER" to sequenceOf("kotlin:S1172"),
    "UNUSED_VARIABLE" to sequenceOf("kotlin:S1481"),
    "UNUSED" to sequenceOf("kotlin:S1172", "kotlin:S1481"),
    "TOO_MANY_ARGUMENTS" to sequenceOf("kotlin:S107"),
    "DEPRECATION" to sequenceOf("kotlin:S1874"),
    "UNCHECKED_CAST" to sequenceOf("kotlin:S6530"),
    "CAST_NEVER_SUCCEEDS" to sequenceOf("kotlin:S6530"),
    "USELESS_CAST" to sequenceOf("kotlin:S6531"),
    "USELESS_IS_CHECK" to sequenceOf("kotlin:S6531"),
    "VARIABLE_WITH_REDUNDANT_INITIALIZER" to sequenceOf("kotlin:S6615"),
    "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE" to sequenceOf("kotlin:S6615"),
    "UNUSED_VALUE" to sequenceOf("kotlin:S6615"),
    "UNUSED_CHANGED_VALUE" to sequenceOf("kotlin:S6615"),
)

class IssueSuppressionVisitor : KotlinFileVisitor() {
    override fun visit(kotlinFileContext: KotlinFileContext) {
        with(IssueSuppressionTreeVisitor(kotlinFileContext, mutableMapOf())) {
            visitTree(kotlinFileContext.ktFile)
            kotlinFileContext.inputFileContext.filteredRules = acc
        }
    }
}

private class IssueSuppressionTreeVisitor(
    val kotlinFileContext: KotlinFileContext,
    val acc: MutableMap<String, Set<TextRange>>,
) : KtTreeVisitor() {
    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) =
        detectSuppressedRules(annotationEntry.annotatedElement())

    private fun detectSuppressedRules(node: KtAnnotated) {
        val suppressedRules = detectSuppressedRulesInAnnotation(node.annotationEntries.asSequence())
        val textRange by lazy(mode = LazyThreadSafetyMode.NONE) { kotlinFileContext.textRange(node) }

        suppressedRules.forEach { suppressedRuleKey ->
            acc.compute(suppressedRuleKey) { _, value ->
                if (value == null) {
                    setOf(textRange)
                } else {
                    value + textRange
                }
            }
        }
    }

    private fun detectSuppressedRulesInAnnotation(annotations: Sequence<KtAnnotationEntry>) =
        annotations
            .filter { it.shortName?.asString() in SUPPRESSION_ANNOTATION_NAMES }
            .flatMap { getArgumentsText(it.valueArguments.asSequence()) }
            .flatMap { ruleKey ->
                COMPILER_KEY_TO_SONAR_KEYS[ruleKey.uppercase()] ?: sequenceOf(ruleKey)
            }

    private fun getArgumentsText(args: Sequence<ValueArgument>) =
        args.flatMap { valueArgument ->
            when (val argExpr = valueArgument.getArgumentExpression()) {
                is KtStringTemplateExpression -> sequenceOf(argExpr.asString())
                is KtCollectionLiteralExpression -> argExpr.getInnerExpressions().asSequence()
                    .filterIsInstance<KtStringTemplateExpression>()
                    .map { it.asString() }
                else -> emptySequence()
            }
        }
}
