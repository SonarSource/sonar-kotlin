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

import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6615")
class UselessAssignmentsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        context.kaDiagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factoryName) {
                    FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT.name ->
                        diagnostic.psi to "Remove this useless initializer."

                    FirErrors.VARIABLE_NEVER_READ.name ->
                        (diagnostic.psi as KtNamedDeclaration).identifyingElement!! to
                        "Remove this variable, which is assigned but never accessed."

                    FirErrors.ASSIGNED_VALUE_IS_NEVER_READ.name -> withKaSession {
                        if ((diagnostic.psi.parent as? KtPrefixExpression)?.isUsedAsExpression == true) {
                            // https://youtrack.jetbrains.com/issue/KT-75695/Bogus-Assigned-value-is-never-read-warning-for-prefix-operator
                            return@mapNotNull null
                        }
                        diagnostic.psi.parent to "The value assigned here is never used."
                    }

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }
    }
}
