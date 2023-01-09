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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.isNull
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext
    ) {
        val arguments = resolvedCall.valueArgumentsByIndex ?: return
        val name = resolvedCall.resultingDescriptor.name.asString()

        if (
            with(kotlinFileContext.bindingContext) {
                name in STICKY_BROADCAST_NAMES
                    || isSendBroadcast(name, arguments)
                    || isSendBroadcastAsUser(name, arguments)
                    || isSendOrderedBroadcast(name, arguments)
                    || isSendOrderedBroadcastAsUser(name, arguments)
            }
        ) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        }
    }

    private fun BindingContext.isSendOrderedBroadcastAsUser(
        name: String,
        argumentsByIndex: List<ResolvedValueArgument>
    ) = name == "sendOrderedBroadcastAsUser" && (argumentsByIndex.getOrNull(2)?.isNull(this) ?: false)

    private fun BindingContext.isSendOrderedBroadcast(name: String, argumentsByIndex: List<ResolvedValueArgument>) =
        name == "sendOrderedBroadcast" && (argumentsByIndex.getOrNull(1)?.isNull(this) ?: false)

    private fun BindingContext.isSendBroadcastAsUser(name: String, argumentsByIndex: List<ResolvedValueArgument>) =
        name == "sendBroadcast" && (argumentsByIndex.getOrNull(1)?.isNull(this) ?: true)

    private fun BindingContext.isSendBroadcast(name: String, argumentsByIndex: List<ResolvedValueArgument>) =
        name == "sendBroadcastAsUser" && (argumentsByIndex.getOrNull(2)?.isNull(this) ?: true)

}
