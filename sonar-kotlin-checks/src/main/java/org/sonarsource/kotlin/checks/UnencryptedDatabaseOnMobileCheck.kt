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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

private val PROBLEMATIC_SIMPLE_CALLS = listOf(
    FunMatcher(definingSupertype = "android.app.Activity", name = "getPreferences"),
    FunMatcher(definingSupertype = "android.content.Context", name = "getSharedPreferences"),
    FunMatcher(definingSupertype = "android.preference.PreferenceManager", name = "getDefaultSharedPreferences"),
    FunMatcher(definingSupertype = "android.content.Context", name = "openOrCreateDatabase")
)

private val PROBLEMATIC_REALM_CALL = FunMatcher(definingSupertype = "io.realm.RealmConfiguration.Builder", name = "build")
private val REALM_ENC_KEY_FUN = FunMatcher(definingSupertype = "io.realm.RealmConfiguration.Builder", name = "encryptionKey")

private const val MESSAGE = "Make sure using an unencrypted database is safe here."

@Rule(key = "S6291")
class UnencryptedDatabaseOnMobileCheck : AbstractCheck() {
    override fun visitCallExpression(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val resolvedCall = analyze { callExpression.resolveToCall()?.successfulFunctionCallOrNull() }
        if (PROBLEMATIC_SIMPLE_CALLS.any { resolvedCall matches it }) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        } else if (
            resolvedCall matches PROBLEMATIC_REALM_CALL &&
            callExpression.findCallInPrecedingCallChain(REALM_ENC_KEY_FUN) == null
        ) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        }
    }
}

private fun KtExpression.findCallInPrecedingCallChain(
    matcher: FunMatcherImpl,
): Pair<KtExpression, KaFunctionCall<*>>? = analyze {
    var receiver = this@findCallInPrecedingCallChain
    var receiverResolved = receiver.resolveToCall()?.successfulFunctionCallOrNull() ?: return null
    while (!matcher.matches(receiverResolved)) {
        receiver = receiver.predictReceiverExpression() ?: return null
        receiverResolved = receiver.resolveToCall()?.singleFunctionCallOrNull() ?: return null
    }
    return receiver to receiverResolved
}
