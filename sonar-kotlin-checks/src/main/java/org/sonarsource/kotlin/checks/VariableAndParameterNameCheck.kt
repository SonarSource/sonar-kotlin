/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
        function.valueParameters.forEach { check("parameter", it, kotlinFileContext) }
    }

    override fun visitProperty(property: KtProperty, kotlinFileContext: KotlinFileContext) {
        if (property.isLocal) {
            check("local variable", property, kotlinFileContext)
        }
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        expression.valueParameters.forEach { check("parameter", it, kotlinFileContext) }
    }

    private fun check(kind: String, declaration: KtNamedDeclaration, kotlinFileContext: KotlinFileContext) {
        val name = declaration.nameIdentifier?.text ?: return
        if (!name.matches(formatRegex)) {
            kotlinFileContext.reportIssue(declaration.nameIdentifier!!, """Rename this $kind to match the regular expression $format""")
        }
    }

}
