/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S1481")
class UnusedLocalVariableCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        context.kaDiagnostics
            .filter { it.factoryName == FirErrors.UNUSED_VARIABLE.name }
            .map { it.psi as KtNamedDeclaration }
            .filterNot { it.hasUnresolvedInitializer() }
            .forEach {
                context.reportIssue(it.nameIdentifier!!, """Remove this unused "${it.name}" local variable.""")
            }
    }
}

/**
 * K2's UNUSED_VARIABLE diagnostic is not reliable when semantic errors prevent it from determining
 * the property's initializer type.
 */
private fun KtNamedDeclaration.hasUnresolvedInitializer(): Boolean = withKaSession {
    return (this@hasUnresolvedInitializer as? KtProperty)
        ?.let { it.initializer ?: it.delegateExpression }
        ?.expressionType
        ?.containsErrorType() == true
}

private fun KaType.containsErrorType(): Boolean =
    this is KaErrorType || (this is KaClassType && typeArguments.any { it.type?.containsErrorType() == true })
