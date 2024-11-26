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
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.getVariableType
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1481")
class UnusedLocalVariableCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
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
