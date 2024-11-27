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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message

private val corePluginMatcherRegex = """org\.gradle\.[^.]+""".toRegex()
private const val PREFIX_LENGTH = "org.gradle.".length

@Rule(key = "S6634")
class CorePluginsShortcutUsageCheck : AbstractCheck() {
    override fun visitCallExpression(callExpr: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val calleeExpr = callExpr.calleeExpression ?: return
        val referencedName = (calleeExpr.referenceExpression() as? KtNameReferenceExpression)?.getReferencedName() ?: return
        if (callExpr.valueArguments.size != 1 || referencedName != "id") return

        val argAsString = callExpr.valueArguments.first().getArgumentExpression()
            ?.stringValue() ?: return
        if (argAsString.matches(corePluginMatcherRegex)) {
            val canonicalName = argAsString.substring(PREFIX_LENGTH).let {
                if (it.contains('-')) "`$it`"
                else it
            }
            kotlinFileContext.reportIssue(callExpr, message(canonicalName))
        }
    }
}

private fun KtExpression?.stringValue(): String? {
    return if (this is KtStringTemplateExpression) {
        val entries = entries.map {
            if (it.expression != null) it.expression!!.stringValue() else it.text
        }
        if (entries.all { it != null }) entries.joinToString("") else null
    } else null
}

private fun message(canonicalName: String) =
    message {
        +"Replace this with the core plugin short name "
        code(canonicalName)
    }
