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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.getVariableType
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

//@K1only
@Rule(key = "S1481")
class UnusedLocalVariableCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) = analyze(file) {
        // https://kotlin.github.io/analysis-api/diagnostics.html#diagnostics-in-a-ktfile
        // TODO this doesn't work anymore for K1 because we clear bindingContext.diagnostics
        file.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .filter {
                // TODO filter by type not needed for K2?
                it.factoryName == Errors.UNUSED_VARIABLE.name
            }
            .map { it.psi as KtNamedDeclaration }
            .forEach {
                context.reportIssue(it.nameIdentifier!!, """Remove this unused "${it.name}" local variable.""")
            }

        context.diagnostics
            .filter {
                it.factory == Errors.UNUSED_VARIABLE &&
                    it.psiElement.getVariableType(context.bindingContext)?.getKotlinTypeFqName(false) != "kotlin.Nothing"
            }
            .map { it.psiElement as KtNamedDeclaration }
            .forEach {
                context.reportIssue(it.nameIdentifier!!, """Remove this unused "${it.name}" local variable.""")
            }
    }
}
