/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.getType
import org.sonarsource.kotlin.api.checks.hasExactlyOneFunctionAndNoProperties
import org.sonarsource.kotlin.api.checks.isFunctionalInterface
import org.sonarsource.kotlin.api.checks.merge
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S6516")
class SamConversionCheck : AbstractCheck() {

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, context: KotlinFileContext) {
        val superTypeEntry = declaration.superTypeListEntries.singleOrNull() ?: return
        val superType = superTypeEntry.typeReference?.getType(context.bindingContext) ?: return

        if (superType.isFunctionalInterface() && declaration.hasExactlyOneFunctionAndNoProperties()) {
            val textRange = context.merge(declaration.getDeclarationKeyword()!!, superTypeEntry)
            context.reportIssue(textRange, "Replace explicit functional interface implementation with lambda expression.")
        }
    }
}
