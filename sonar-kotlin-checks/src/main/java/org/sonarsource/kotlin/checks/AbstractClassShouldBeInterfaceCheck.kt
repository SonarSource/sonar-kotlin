/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6526")
class AbstractClassShouldBeInterfaceCheck : AbstractCheck() {

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {

        if (!klass.isAbstract() || klass.isInterface() || klass.extendsClass() || klass.isExternal()) return

        val allMethods = klass.collectDescendantsOfType<KtFunction>()
        val allProperties: List<KtProperty> by lazy { klass.collectDescendantsOfType<KtProperty>() }

        if (allMethods.all { it.isAbstract() } && allProperties.all { it.isAbstract() }) {
            context.reportIssue(
                klass.nameIdentifier!!,
                "Replace this abstract class with an interface, or add function implementations or state properties to the class."
            )
        }
    }

    private fun KtClass.extendsClass(): Boolean {
        return superTypeListEntries.any { it is KtSuperTypeCallEntry }
    }
    private fun KtClass.isExternal() = hasModifier(KtTokens.EXTERNAL_KEYWORD)

    private fun KtDeclaration.isAbstract() = hasModifier(KtTokens.ABSTRACT_KEYWORD)

}
