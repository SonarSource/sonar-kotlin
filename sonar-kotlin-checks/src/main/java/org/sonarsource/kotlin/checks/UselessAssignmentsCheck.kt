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

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

//@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S6615")
class UselessAssignmentsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) = analyze(file) {
        // https://kotlin.github.io/analysis-api/diagnostics.html#diagnostics-in-a-ktfile
        // TODO this doesn't work anymore for K1 because we clear bindingContext.diagnostics
        file.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .mapNotNull { diagnostic ->
                when (diagnostic.factoryName) {
                    FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT.name ->
//                    Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.name ->
                        diagnostic.psi to "Remove this useless initializer."

                    FirErrors.VARIABLE_NEVER_READ.name ->
//                    Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.name ->
                        (diagnostic.psi as KtNamedDeclaration).identifyingElement!! to
                            "Remove this variable, which is assigned but never accessed."

                    FirErrors.ASSIGNED_VALUE_IS_NEVER_READ.name ->
//                    Errors.UNUSED_VALUE.name ->
                        diagnostic.psi to "The value assigned here is never used."

//                    Errors.UNUSED_CHANGED_VALUE.name ->
//                        diagnostic.psi to "The value changed here is never used."

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }

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
                        diagnostic.psiElement to "The value assigned here is never used."

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }
    }
}
