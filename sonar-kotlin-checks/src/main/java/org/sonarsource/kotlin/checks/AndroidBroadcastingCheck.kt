/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val MESSAGE = "Make sure that broadcasting intents is safe here."

private val STICKY_BROADCAST_NAMES = setOf(
    "sendStickyBroadcast",
    "sendStickyBroadcastAsUser",
    "sendStickyOrderedBroadcast",
    "sendStickyOrderedBroadcastAsUser",
)

@Rule(key = "S5320")
class AndroidBroadcastingCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(FunMatcher(definingSupertype = "android.content.Context") {
        withNames(
            "sendBroadcast",
            "sendBroadcastAsUser",
            "sendOrderedBroadcast",
            "sendOrderedBroadcastAsUser",
            "sendStickyBroadcast",
            "sendStickyBroadcastAsUser",
            "sendStickyOrderedBroadcast",
            "sendStickyOrderedBroadcastAsUser",
        )
    })

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val arguments = resolvedCall.argumentMapping.keys.toList()
        val name = resolvedCall.partiallyAppliedSymbol.symbol.name?.asString() ?: return

        if (
            name in STICKY_BROADCAST_NAMES ||
            isSendBroadcast(name, arguments) ||
            isSendBroadcastAsUser(name, arguments) ||
            isSendOrderedBroadcast(name, arguments) ||
            isSendOrderedBroadcastAsUser(name, arguments)
        ) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        }
    }

    private fun isSendOrderedBroadcastAsUser(
        name: String,
        argumentsByIndex: List<KtExpression>
    ) = name == "sendOrderedBroadcastAsUser" && (argumentsByIndex.getOrNull(2)?.isNull() ?: false)

    private fun isSendOrderedBroadcast(name: String, argumentsByIndex: List<KtExpression>) =
        name == "sendOrderedBroadcast" && (argumentsByIndex.getOrNull(1)?.isNull() ?: false)

    private fun isSendBroadcastAsUser(name: String, argumentsByIndex: List<KtExpression>) =
        name == "sendBroadcast" && (argumentsByIndex.getOrNull(1)?.isNull() ?: true)

    private fun isSendBroadcast(name: String, argumentsByIndex: List<KtExpression>) =
        name == "sendBroadcastAsUser" && (argumentsByIndex.getOrNull(2)?.isNull() ?: true)

}
