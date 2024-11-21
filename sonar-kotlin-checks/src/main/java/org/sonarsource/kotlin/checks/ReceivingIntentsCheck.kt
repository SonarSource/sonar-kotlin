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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.isNull
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S5322")
class ReceivingIntentsCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        FunMatcher(definingSupertype = "android.content.Context", name = "registerReceiver")
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext
    ) {
        val arguments = resolvedCall.valueArgumentsByIndex ?: return
        if (arguments.size < 4 || arguments[2].isNull(kotlinFileContext.bindingContext)) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, "Make sure that intents are received safely here.")
        }
    }
}
