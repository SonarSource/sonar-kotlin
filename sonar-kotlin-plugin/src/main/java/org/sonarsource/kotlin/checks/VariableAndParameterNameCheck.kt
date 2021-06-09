/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.VariableAndParameterNameCheck]
 */
@Rule(key = "S117")
class VariableAndParameterNameCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_FORMAT = "^`?[_a-z][a-zA-Z0-9]*`?$"
    }

    @RuleProperty(
        key = "format",
        description = "Regular expression used to check the names against.",
        defaultValue = DEFAULT_FORMAT)
    var format: String = DEFAULT_FORMAT

    private lateinit var formatRegex: Regex

    override fun initialize(ruleKey: RuleKey) {
        super.initialize(ruleKey)
        formatRegex = Regex(format)
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, kotlinFileContext: KotlinFileContext) {
        constructor.valueParameters.forEach { check("parameter", it, kotlinFileContext) }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, kotlinFileContext: KotlinFileContext) {
        constructor.valueParameters.forEach { check("parameter", it, kotlinFileContext) }
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        if (function.receiverTypeReference != null) {
            /** see [org.sonarsource.kotlin.converter.KotlinTreeVisitor.createFunctionDeclarationTree] */
            return
        }
        function.valueParameters.forEach { check("parameter", it, kotlinFileContext) }
    }

    override fun visitProperty(property: KtProperty, kotlinFileContext: KotlinFileContext) {
        val containingDeclarationForPseudocode = property.containingDeclarationForPseudocode
        if (containingDeclarationForPseudocode is KtFunction && containingDeclarationForPseudocode.receiverTypeReference != null) {
            /** see [org.sonarsource.kotlin.converter.KotlinTreeVisitor.createFunctionDeclarationTree] */
            return
        }
        /** see `hasDelegate` in [org.sonarsource.kotlin.converter.KotlinTreeVisitor.createVariableDeclaration] */
        if (property.isLocal && !property.hasDelegate()) {
            check("local variable", property, kotlinFileContext)
        }
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        expression.valueParameters.forEach { check("parameter", it, kotlinFileContext) }
    }

    private fun check(kind: String, declaration: KtNamedDeclaration, kotlinFileContext: KotlinFileContext) {
        val name = declaration.nameIdentifier?.text ?: return
        if (!name.matches(formatRegex)) {
            kotlinFileContext.reportIssue(
                declaration.nameIdentifier!!,
                "Rename this $kind to match the regular expression \"$format\".")
        }
    }

}
