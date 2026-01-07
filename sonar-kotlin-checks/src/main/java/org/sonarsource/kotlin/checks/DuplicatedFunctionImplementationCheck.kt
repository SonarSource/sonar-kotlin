/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S4144")
class DuplicatedFunctionImplementationCheck : AbstractCheck() {

    companion object {
        private const val BASE_MESSAGE = "Update this function so that its implementation is not identical to"
        private const val MINIMUM_STATEMENTS_COUNT = 2

        private fun hasMinimumSize(function: KtNamedFunction): Boolean {
            val statements = function.bodyBlockExpression?.statements ?: return false
            return statements.size >= MINIMUM_STATEMENTS_COUNT
        }

        private fun areDuplicatedImplementation(
            original: KtNamedFunction,
            possibleDuplicate: KtNamedFunction,
        ): Boolean {
            return SyntacticEquivalence.areEquivalent(original, possibleDuplicate)
        }
    }

    override fun visitKtFile(file: KtFile, ctx: KotlinFileContext) {
        val functionsFinder = KtNamedFunctionsFinder()
        file.accept(functionsFinder)
        functionsFinder.functions.forEach { (_, functions) -> check(ctx, functions) }
    }

    private fun check(ctx: KotlinFileContext, functionDeclarations: List<KtNamedFunction>) {
        val reportedDuplicates: MutableSet<KtNamedFunction> = HashSet()
        functionDeclarations.indices.forEach { i ->
            val original = functionDeclarations[i]
            functionDeclarations.asSequence()
                .drop(i + 1)
                .filter { f -> !reportedDuplicates.contains(f) }
                .filter { f -> hasMinimumSize(f) }
                .filter { f -> areDuplicatedImplementation(original, f) }
                .forEach { duplicate ->
                    reportDuplicate(ctx, original, duplicate)
                    reportedDuplicates.add(duplicate)
                }
        }
    }

    private fun reportDuplicate(
        ctx: KotlinFileContext,
        original: KtNamedFunction,
        duplicate: KtNamedFunction,
    ) {
        val textRange = ctx.textRange(original.nameIdentifier ?: original)
        val line = textRange.start().line()
        val message = original.name?.let { """$BASE_MESSAGE "$it" on line $line.""" }
            ?: "$BASE_MESSAGE the one on line $line."
        ctx.reportIssue(
            duplicate.nameIdentifier ?: duplicate,
            message,
            listOf(SecondaryLocation(textRange, "original implementation")),
        )
    }
}

private class KtNamedFunctionsFinder : KtTreeVisitorVoid() {
    val functions: MutableMap<PsiElement, MutableList<KtNamedFunction>> = HashMap()

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        functions.computeIfAbsent(function.parent) { mutableListOf() }.add(function)
    }
}
