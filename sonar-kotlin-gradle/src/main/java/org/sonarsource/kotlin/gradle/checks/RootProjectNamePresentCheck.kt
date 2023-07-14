/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message

private const val SETTINGS_FILE_NAME = "settings.gradle.kts"
private const val IDENTIFIER_ROOT_PROJECT = "rootProject"
private const val IDENTIFIER_NAME = "name"
private const val REF_NAME_SET_NAME = "setName"

@Rule(key = "S6625")
class RootProjectNamePresentCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        if (!file.name.endsWith(SETTINGS_FILE_NAME)) return

        val visitor = ScanRootProjectNameAssign()
        file.acceptChildren(visitor)
        if (!visitor.isRootProjectNameAssigned) {

            kotlinFileContext.reportIssue(
                null,
                message {
                    +"Assign "
                    code("$IDENTIFIER_ROOT_PROJECT.$IDENTIFIER_NAME")
                    +" in "
                    code(SETTINGS_FILE_NAME)
                }
            )
        }
    }

    private class ScanRootProjectNameAssign : KtTreeVisitorVoid() {

        var isRootProjectNameAssigned = false

        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            if (expression.operationToken != KtTokens.EQ) return
            val qualifier = expression.left as? KtDotQualifiedExpression ?: return
            val receiver = qualifier.receiverExpression as? KtNameReferenceExpression ?: return
            val selector = qualifier.selectorExpression as? KtNameReferenceExpression ?: return

            if (receiver.getReferencedName() == IDENTIFIER_ROOT_PROJECT && selector.getReferencedName() == IDENTIFIER_NAME) {
                isRootProjectNameAssigned = true
            }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            val qualifier = expression.parent as? KtDotQualifiedExpression ?: return

            if (expression.getCalleeExpressionIfAny()?.getReferencedName() == REF_NAME_SET_NAME
                && qualifier.receiverExpression.getReferencedName() == IDENTIFIER_ROOT_PROJECT
            ) {
                isRootProjectNameAssigned = true
            }
        }
    }
}

private fun KtExpression.getReferencedName() = (this as? KtNameReferenceExpression)?.getReferencedName()
