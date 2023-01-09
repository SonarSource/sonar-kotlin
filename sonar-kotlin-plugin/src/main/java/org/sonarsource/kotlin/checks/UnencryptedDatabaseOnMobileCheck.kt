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
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.findCallInPrecedingCallChain
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        val bindingContext = kotlinFileContext.bindingContext
        val resolvedCall = callExpression.getResolvedCall(bindingContext)
        if (PROBLEMATIC_SIMPLE_CALLS.any { resolvedCall matches it }) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        } else if (
            resolvedCall matches PROBLEMATIC_REALM_CALL &&
            callExpression.getCall(bindingContext)?.findCallInPrecedingCallChain(REALM_ENC_KEY_FUN, bindingContext) == null
        ) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        }
    }
}
