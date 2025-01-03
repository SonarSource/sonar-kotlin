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
package org.sonarsource.kotlin.api.visiting

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.sonar.api.batch.rule.Checks
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.common.measureDuration
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
