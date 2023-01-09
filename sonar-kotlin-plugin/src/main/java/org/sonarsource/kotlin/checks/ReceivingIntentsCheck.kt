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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.isNull
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
