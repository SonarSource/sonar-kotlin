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

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6615")
class UselessAssignmentsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        context.diagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factory) {
                    Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER ->
                        diagnostic.psiElement to "Remove this useless initializer."

                    Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE ->
                        (diagnostic.psiElement as KtNamedDeclaration).identifyingElement!! to
                        "Remove this variable, which is assigned but never accessed."

                    Errors.UNUSED_VALUE ->
                        diagnostic.psiElement to "The value assigned here is never used."

                    Errors.UNUSED_CHANGED_VALUE ->
                        diagnostic.psiElement to "The value changed here is never used."

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }
    }
}
