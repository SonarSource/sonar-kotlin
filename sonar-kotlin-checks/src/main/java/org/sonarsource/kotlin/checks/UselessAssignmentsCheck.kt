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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6615")
class UselessAssignmentsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        context.kaDiagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factoryName) {
                    /** TODO replace [Errors] by [org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors] during switch to K2 */
                    "VARIABLE_INITIALIZER_IS_REDUNDANT",
                    Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.name ->
                        diagnostic.psi to "Remove this useless initializer."

                    "VARIABLE_NEVER_READ",
                    Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.name ->
                        (diagnostic.psi as KtNamedDeclaration).identifyingElement!! to
                        "Remove this variable, which is assigned but never accessed."

                    "ASSIGNED_VALUE_IS_NEVER_READ" ->
                        diagnostic.psi.parent to "The value assigned here is never used."
                    Errors.UNUSED_VALUE.name ->
                        diagnostic.psi to "The value assigned here is never used."

                    Errors.UNUSED_CHANGED_VALUE.name ->
                        diagnostic.psi to "The value changed here is never used."

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }
    }
}
