/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Make sure that broadcasting intents is safe here."

private val MATCHER = FunMatcher(qualifier = "android.content.Context") {
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
}

@Rule(key = "S5320")
class AndroidBroadcastingCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(MATCHER)


    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext
    ) {
        val argumentsByIndex = resolvedCall.valueArgumentsByIndex ?: return
        val name = resolvedCall.resultingDescriptor.name.asString()

        if (isSendBroadcast(name, argumentsByIndex) /*|| isSendBroadcastAsUser(mit) || isSendOrderedBroadcast(mit) ||
            isSendOrderedBroadcastAsUser(mit) || STICKY_BROADCAST.matches(mit)*/
        ) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        }
    }

    private fun isSendBroadcast(name: String, argumentsByIndex: List<ResolvedValueArgument>): Boolean {
        return name == "sendBroadcast" && (argumentsByIndex.size < 2 || argumentsByIndex[1] == null)
    }




/*
    private val SEND_BROADCAST: MethodMatchers = androidContext().names("sendBroadcast").withAnyParameters().build()
    private val SEND_BROADCAST_AS_USER: MethodMatchers =
        androidContext().names("sendBroadcastAsUser").withAnyParameters().build()
    private val SEND_ORDERED_BROADCAST: MethodMatchers =
        androidContext().names("sendOrderedBroadcast").withAnyParameters().build()
    private val SEND_ORDERED_BROADCAST_AS_USER: MethodMatchers =
        androidContext().names("sendOrderedBroadcastAsUser").withAnyParameters().build()
    private val SEND_STICKY_BROADCAST: MethodMatchers =
        androidContext().names("sendStickyBroadcast").withAnyParameters().build()
    private val SEND_STICKY_BROADCAST_AS_USER: MethodMatchers =
        androidContext().names("sendStickyBroadcastAsUser").withAnyParameters().build()
    private val SEND_STICKY_ORDERED_BROADCAST: MethodMatchers =
        androidContext().names("sendStickyOrderedBroadcast").withAnyParameters().build()
    private val SEND_STICKY_ORDERED_BROADCAST_AS_USER: MethodMatchers =
        androidContext().names("sendStickyOrderedBroadcastAsUser").withAnyParameters().build()
    private val STICKY_BROADCAST: MethodMatchers = MethodMatchers.or(
        SEND_STICKY_BROADCAST,
        SEND_STICKY_BROADCAST_AS_USER, SEND_STICKY_ORDERED_BROADCAST, SEND_STICKY_ORDERED_BROADCAST_AS_USER
    )

    private fun isSendBroadcast(mit: KtCallExpression): Boolean {
        return SEND_BROADCAST.matches(mit) && (mit.arguments().size() < 2 || mit.arguments().get(1).`is`(NULL_LITERAL))
    }

    private fun isSendBroadcastAsUser(mit: KtCallExpression): Boolean {
        return SEND_BROADCAST_AS_USER.matches(mit) && (mit.arguments().size() < 3 || mit.arguments().get(2)
            .`is`(NULL_LITERAL))
    }

    private fun isSendOrderedBroadcast(mit: KtCallExpression): Boolean {
        return SEND_ORDERED_BROADCAST.matches(mit) && mit.arguments().size() > 1 && mit.arguments().get(1)
            .`is`(NULL_LITERAL)
    }

    private fun isSendOrderedBroadcastAsUser(mit: KtCallExpression): Boolean {
        return SEND_ORDERED_BROADCAST_AS_USER.matches(mit) && mit.arguments().size() > 2 && mit.arguments().get(2)
            .`is`(NULL_LITERAL)
    }*/
}
