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
package org.sonarsource.kotlin.verifier

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.api.Tree
import org.sonarsource.slang.plugin.InputFileContext
import org.sonarsource.slang.visitors.TreeVisitor

class KtTestChecksVisitor(private val checks: List<AbstractCheck>) : TreeVisitor<InputFileContext>() {
    init {
        checks.forEach { it.initialize(RuleKey.of("Kotlin", "Dummy")) }
    }

    override fun scan(fileContext: InputFileContext, root: Tree?) {
        if (root is KotlinTree) {
            visit(KotlinFileContext(fileContext, root.psiFile, root.bindingContext))
        }
    }

    private fun visit(kotlinFileContext: KotlinFileContext) {
        flattenNodes(listOf(kotlinFileContext.ktFile)).forEach { psiElement ->
            // Note: we only visit KtElements. If we need to visit PsiElement, add a
            // visitPsiElement function in KotlinCheck and call it here in the else branch.
            when (psiElement) {
                is KtElement -> checks.forEach { check -> psiElement.accept(check, kotlinFileContext) }
            }
        }
    }

    private fun flattenNodes(root: List<PsiElement>): List<PsiElement> =
        root + root.flatMap { flattenNodes(it.children.toList()) }
}
