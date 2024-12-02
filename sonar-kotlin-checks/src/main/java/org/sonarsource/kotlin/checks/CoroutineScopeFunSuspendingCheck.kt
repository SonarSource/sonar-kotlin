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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.checks.suspendModifier
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = "Extension functions on CoroutineScope should not be suspending."

@Rule(key = "S6312")
class CoroutineScopeFunSuspendingCheck : AbstractCheck() {
    private val coroutineScopeClassId = ClassId.fromString("kotlinx/coroutines/CoroutineScope")

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) = withKaSession {
        // Only applicable for suspending extension functions
        val suspendModifier = function.suspendModifier() ?: return
        val receiverType = function.receiverTypeReference ?: return
        if (receiverType.type.isClassType(coroutineScopeClassId) ||
            receiverType.type.allSupertypes.any { it.isClassType(coroutineScopeClassId) }
        ) {
            val secondaries = listOf(SecondaryLocation(kotlinFileContext.textRange(receiverType)))
            kotlinFileContext.reportIssue(suspendModifier, MESSAGE, secondaries)
        }
    }
}
