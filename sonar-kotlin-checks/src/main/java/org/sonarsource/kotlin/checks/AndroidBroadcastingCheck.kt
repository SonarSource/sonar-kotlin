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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.isPredictedNull
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
    ) = name == "sendOrderedBroadcastAsUser" && (argumentsByIndex.getOrNull(2)?.isPredictedNull() ?: false)

    private fun isSendOrderedBroadcast(name: String, argumentsByIndex: List<KtExpression>) =
        name == "sendOrderedBroadcast" && (argumentsByIndex.getOrNull(1)?.isPredictedNull() ?: false)

    private fun isSendBroadcastAsUser(name: String, argumentsByIndex: List<KtExpression>) =
        name == "sendBroadcast" && (argumentsByIndex.getOrNull(1)?.isPredictedNull() ?: true)

    private fun isSendBroadcast(name: String, argumentsByIndex: List<KtExpression>) =
        name == "sendBroadcastAsUser" && (argumentsByIndex.getOrNull(2)?.isPredictedNull() ?: true)

}
