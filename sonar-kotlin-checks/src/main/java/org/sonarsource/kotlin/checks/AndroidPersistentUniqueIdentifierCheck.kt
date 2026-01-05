/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.STRING_TYPE
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val NON_RESETTABLE_PERSISTENT_ID_MESSAGE = "Using a non-resettable persistent identifier puts user privacy at risk. Make sure it is safe here."
private const val HARDWARE_ID_MESSAGE = "Using a hardware identifier puts user privacy at risk. Make sure it is safe here."
private const val PHONE_NUMBER_MESSAGE = "Using a phone number puts user privacy at risk. Make sure it is safe here."
private const val ADVERTISING_ID_MESSAGE = "Using Advertising ID puts user privacy at risk. Make sure it is safe here."

private val staticSettingsSecureGetStringFunMatcher = FunMatcher {
    qualifier = "android.provider.Settings.Secure"
    name = "getString"
    withArguments("android.content.ContentResolver", STRING_TYPE)
}
private val settingsSecureClassId = ClassId.fromString("android/provider/Settings.Secure")
private val staticSettingsSecureAndroidIdName = Name.identifier("ANDROID_ID")

private val instanceMatches = mapOf(
    FunMatcher {
        qualifier = "android.bluetooth.BluetoothAdapter"
        name = "getAddress"
    } to HARDWARE_ID_MESSAGE,
    FunMatcher {
        definingSupertype = "android.net.wifi.WifiInfo"
        name = "getMacAddress"
    } to HARDWARE_ID_MESSAGE,
    FunMatcher {
        definingSupertype = "android.telephony.TelephonyManager"
        withNames("getSimSerialNumber", "getDeviceId", "getImei", "getMeid")
    } to HARDWARE_ID_MESSAGE,
    FunMatcher {
        definingSupertype = "android.telephony.TelephonyManager"
        withNames("getLine1Number")
    } to PHONE_NUMBER_MESSAGE,
    FunMatcher {
        definingSupertype = "android.telephony.SubscriptionManager"
        name = "getPhoneNumber"
    } to PHONE_NUMBER_MESSAGE,
    FunMatcher {
        withDefiningSupertypes(
            "com.google.android.gms.ads.identifier.AdvertisingIdClient.Info",
            "androidx.ads.identifier.AdvertisingIdInfo",
            "com.huawei.hms.ads.identifier.AdvertisingIdClient.Info",
        )
        name = "getId"
    } to ADVERTISING_ID_MESSAGE,
)

@Rule(key = "S7435")
class AndroidPersistentUniqueIdentifierCheck : CallAbstractCheck() {

    override val functionsToVisit = instanceMatches.keys

    override fun visitReferenceExpression(expression: KtReferenceExpression, data: KotlinFileContext) = withKaSession {
        val resolvedCall = expression.resolveToCall()
        val call = resolvedCall?.successfulCallOrNull<KaCallableMemberCall<*, *>>() ?: return@withKaSession
        if (staticSettingsSecureGetStringFunMatcher.matches(call)) {
            val nameValueArgument = resolvedCall.successfulFunctionCallOrNull()?.argumentMapping?.keys?.toList()?.getOrNull(1)
                ?: return@withKaSession
            if (isSettingsSecureAndroidId(nameValueArgument) || nameValueArgument.predictRuntimeStringValue() == "android_id") {
                data.reportIssue(expression, NON_RESETTABLE_PERSISTENT_ID_MESSAGE)
            }
        } else {
            val matcher = instanceMatches.keys.firstOrNull { it.matches(call) } ?: return@withKaSession
            data.reportIssue(expression, instanceMatches[matcher]!!)
        }
    }

    private fun isSettingsSecureAndroidId(expression: KtExpression): Boolean = withKaSession {
        val fieldSymbol = expression.predictRuntimeValueExpression().resolveToCall()?.successfulVariableAccessCall()?.symbol
            ?: return@withKaSession false
        val classSymbol = fieldSymbol.containingSymbol as? KaClassSymbol ?: return@withKaSession false
        classSymbol.classId == settingsSecureClassId && fieldSymbol.name == staticSettingsSecureAndroidIdName
    }
}
