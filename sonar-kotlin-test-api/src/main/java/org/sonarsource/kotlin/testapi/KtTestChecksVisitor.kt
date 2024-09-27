/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.testapi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.runtime.structure.ReflectJavaLiteralAnnotationArgument
import org.jetbrains.kotlin.descriptors.runtime.structure.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.sonar.api.rule.RuleKey
// TODO: testapi should not depend on api module.
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor

class KtTestChecksVisitor(private val checks: List<AbstractCheck>) : KotlinFileVisitor() {
    init {
        checks.forEach { check ->
            val key = (check.javaClass.annotations.findAnnotation(FqName("org.sonar.check.Rule"))
                ?.arguments
                ?.find { it.name?.asString() == "key" } as? ReflectJavaLiteralAnnotationArgument)
                ?.value as? String

            requireNotNull(key) { "Rule key is missing for ${check.javaClass.canonicalName}." }

            check.initialize(RuleKey.of("kotlin", key))
        }
    }

    override fun visit(kotlinFileContext: KotlinFileContext) {
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
