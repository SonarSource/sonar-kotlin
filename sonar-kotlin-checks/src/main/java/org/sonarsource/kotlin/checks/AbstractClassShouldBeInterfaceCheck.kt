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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6526")
class AbstractClassShouldBeInterfaceCheck : AbstractCheck() {

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        super.visitClass(klass, context)

        if (!klass.isAbstract() || klass.isInterface()) return
        val allMethods = klass.collectDescendantsOfType<KtFunction>()
        val allProperties = klass.collectDescendantsOfType<KtProperty>()

        if (allMethods.none { it.isNotAbstract() } && allProperties.none { it.isNotAbstract() }) {
            context.reportIssue(
                klass.nameIdentifier!!,
                "Either replace the class declaration with an interface declaration, or add actual function implementations or state properties to the abstract class."
            )
        }
    }

    private fun KtFunction.isNotAbstract() = !hasModifier(KtTokens.ABSTRACT_KEYWORD)
    private fun KtProperty.isNotAbstract() = !hasModifier(KtTokens.ABSTRACT_KEYWORD)
}
