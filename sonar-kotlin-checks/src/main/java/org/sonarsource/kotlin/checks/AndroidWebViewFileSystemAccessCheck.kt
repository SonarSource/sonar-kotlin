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
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.BOOLEAN_TYPE
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.getFirstArgumentExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val message = "Make sure exposing the Android file system is safe here."

@Rule(key = "S7201")
class AndroidWebViewFileSystemAccessCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher {
            definingSupertype = "android.webkit.WebSettings"
            withNames(
                "setAllowFileAccess",
                "setAllowFileAccessFromFileURLs",
                "setAllowUniversalAccessFromFileURLs",
                "setAllowContentAccess",
            )
            withArguments(BOOLEAN_TYPE)
        },
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext
    ) = withKaSession {
        resolvedCall.getFirstArgumentExpression()?.predictRuntimeBooleanValue()?.ifTrue {
            kotlinFileContext.reportIssue(callExpression, message)
        } ?: return
    }

    override fun visitBinaryExpression(
        binaryExpression: KtBinaryExpression,
        kotlinFileContext: KotlinFileContext
    ) = withKaSession {
        if (!KtPsiUtil.isAssignment(binaryExpression)) return
        val call = binaryExpression.resolveToCall()?.successfulVariableAccessCall()?: return
        if (binaryExpression.right?.predictRuntimeBooleanValue() == true &&
            functionsToVisit.any { it.matches(call) }
        ) {
            kotlinFileContext.reportIssue(binaryExpression, message)
        }
    }
}
