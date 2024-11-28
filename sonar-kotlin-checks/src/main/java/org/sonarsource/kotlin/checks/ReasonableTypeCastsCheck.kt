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
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S6530")
class ReasonableTypeCastsCheck : AbstractCheck() {
    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        context.diagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factory) {
                    Errors.UNCHECKED_CAST -> "Remove this unchecked cast."
                    Errors.CAST_NEVER_SUCCEEDS -> "Remove this cast that can never succeed."
                    else -> null
                }?.let { diagnostic to it }
            }.forEach { (diagnostic, msg) ->
                context.reportIssue(diagnostic.psiElement, msg)
            }
    }
}
