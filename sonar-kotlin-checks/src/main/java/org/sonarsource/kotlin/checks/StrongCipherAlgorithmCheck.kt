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
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val weakCiphers = listOf(
    "DESEDE",
    "DESEDEWRAP",
    "DES",
    "ARC2",
    "ARC4",
    "ARCFOUR",
    "BLOWFISH",
    "RC2",
    "RC4",
)
private const val msg = "Use a strong cipher algorithm."
private val cipherGetInstanceMatcher = FunMatcher(qualifier = "javax.crypto.Cipher", name = "getInstance")
private val nullCipherConstructorMatcher = ConstructorMatcher("javax.crypto.NullCipher")

@Rule(key = "S5547")
class StrongCipherAlgorithmCheck : AbstractCheck() {
    override fun visitCallExpression(callExpr: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val (_, _, bindingContext) = kotlinFileContext
        if (nullCipherConstructorMatcher.matches(callExpr, bindingContext)) {
            kotlinFileContext.reportIssue(callExpr, msg)
        } else if (cipherGetInstanceMatcher.matches(callExpr, bindingContext)) {
            callExpr.valueArguments.firstOrNull()?.let { arg ->
                arg.getArgumentExpression()?.predictRuntimeStringValue(bindingContext)?.uppercase()?.let { candidateString ->
                    if (weakCiphers.any { cipher -> candidateString == cipher || candidateString.startsWith("$cipher/") }) {
                        kotlinFileContext.reportIssue(arg, msg)
                    }
                }
            }
        }
    }
}
