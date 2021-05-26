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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.DuplicatedFunctionImplementationCheck]
 */
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
        val textRange = KotlinTextRanges.textRange(
            ctx.ktFile.viewProvider.document!!,
            original.nameIdentifier ?: original,
        )
        val line = textRange.start().line()
        val message = original.name?.let { "$BASE_MESSAGE \"$it\" on line $line." }
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
        if (function !is KtConstructor<*>) functions.computeIfAbsent(function.parent) { mutableListOf() }
            .add(function)
    }
}
