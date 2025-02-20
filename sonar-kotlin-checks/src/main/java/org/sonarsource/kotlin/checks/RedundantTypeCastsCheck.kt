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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.psi.KtFile
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.kotlin.api.reporting.message
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S6531")
class RedundantTypeCastsCheck : AbstractCheck() {
    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        context.diagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factory.name) {
                    FirErrors.USELESS_CAST.name -> Message("Remove this useless cast.")
                    FirErrors.USELESS_IS_CHECK.name -> message {
                        +"Remove this useless "
                        code("is")
                        +" check."
                    }

                    else -> null
                }?.let { diagnostic to it }
            }.forEach { (diagnostic, msg) ->
                context.reportIssue(diagnostic.psiElement, msg)
            }
    }
}
