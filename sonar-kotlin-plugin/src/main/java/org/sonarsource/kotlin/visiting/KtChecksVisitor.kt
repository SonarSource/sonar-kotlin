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
package org.sonarsource.kotlin.visiting

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.sonar.api.batch.rule.Checks
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.plugin.measureDuration

class KtChecksVisitor(val checks: Checks<out AbstractCheck>) : KotlinFileVisitor() {

    override fun visit(kotlinFileContext: KotlinFileContext) {
        flattenNodes(listOf(kotlinFileContext.ktFile)).let { flatNodes ->
            checks.all().forEach { check ->
                measureDuration(check.javaClass.simpleName) {
                    flatNodes.forEach { node ->
                        // Note: we only visit KtElements. If we need to visit PsiElement, add a
                        // visitPsiElement function in KotlinCheck and call it here in the else branch.
                        when (node) {
                            is KtElement -> node.accept(check, kotlinFileContext)
                        }
                    }
                }
            }
        }
    }

    private tailrec fun flattenNodes(childNodes: List<PsiElement>, acc: MutableList<PsiElement> = mutableListOf()): List<PsiElement> =
        if (childNodes.none()) acc
        else flattenNodes(childNodes = childNodes.flatMap { it.children.asList() }, acc = acc.apply { addAll(childNodes) })
}
