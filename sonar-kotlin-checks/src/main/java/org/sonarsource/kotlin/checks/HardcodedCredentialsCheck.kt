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

import java.net.URI
import java.net.URISyntaxException
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S2068")
class HardcodedCredentialsCheck : AbstractHardcodedVisitor() {
    @RuleProperty(key = "credentialWords",
        description = "Comma separated list of words identifying potential credentials",
        defaultValue = DEFAULT_VALUE)
    var credentialWords = DEFAULT_VALUE
    override val sensitiveVariableKind: String
        get() = "credential"
    override val sensitiveWords: String
        get() = credentialWords

    companion object {
        private const val DEFAULT_VALUE = "password,passwd,pwd,passphrase"
        private val URI_PREFIX = Regex("^\\w{1,8}://")

        private fun isURIWithCredentials(stringLiteral: String): Boolean {
            if (URI_PREFIX.containsMatchIn(stringLiteral)) {
                try {
                    val userInfo = URI(stringLiteral).userInfo
                    if (userInfo != null) {
                        val parts = userInfo.split(":").toTypedArray()
                        return (parts.size > 1 && parts[0] != parts[1]) && !(parts.size == 2 && parts[1].isEmpty())
                    }
                } catch (_: URISyntaxException) {
                    // ignore, stringLiteral is not a valid URI
                }
            }
            return false
        }
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, context: KotlinFileContext) {
        val content = if (!expression.hasInterpolation()) expression.asConstant() else ""
        if (isURIWithCredentials(content)) {
            context.reportIssue(expression, "Review this hard-coded URL, which may contain a credential.")
        } else {
            super.visitStringTemplateExpression(expression, context)
        }
    }
}
