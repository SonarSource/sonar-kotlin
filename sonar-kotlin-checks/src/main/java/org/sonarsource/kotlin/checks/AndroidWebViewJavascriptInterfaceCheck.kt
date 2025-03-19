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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ANY_TYPE
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.STRING_TYPE
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = "Exposing a Javascript interface can expose sensitive information to attackers. Make sure it is safe here."

@Rule(key = "S7409")
class AndroidWebViewJavascriptInterfaceCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher {
            definingSupertype = "android.webkit.WebView"
            withNames("addJavascriptInterface")
            withArguments(ANY_TYPE, STRING_TYPE)
        },
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext
    ) = withKaSession {
        kotlinFileContext.reportIssue(callExpression, MESSAGE)
    }
}
