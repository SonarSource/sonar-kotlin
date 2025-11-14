/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.analyzer.commons.EntropyDetector
import org.sonarsource.analyzer.commons.HumanLanguageDetector

private const val DEFAULT_SECRET_WORDS = "api[_.-]?key,auth,credential,secret,token"
private const val DEFAULT_RANDOMNESS_SENSIBILITY = "3.0"

private const val MAX_RANDOMNESS_SENSIBILITY = 10
private const val MINIMUM_CREDENTIAL_LENGTH = 17
private const val LANGUAGE_SCORE_INCREMENT = 0.3

@Rule(key = "S6418")
class HardcodedSecretsCheck : AbstractHardcodedVisitor() {

    @RuleProperty(
        key = "secretWords",
        description = "Comma separated list of words identifying potential secrets",
        defaultValue = DEFAULT_SECRET_WORDS
    )
    var secretWords: String = DEFAULT_SECRET_WORDS

    @RuleProperty(
        key = "randomnessSensibility",
        description = "Allows to tune the Randomness Sensibility (from 0 to 10)",
        defaultValue = DEFAULT_RANDOMNESS_SENSIBILITY
    )
    var randomnessSensibility: Double = DEFAULT_RANDOMNESS_SENSIBILITY.toDouble()

    private lateinit var entropyDetector: EntropyDetector
    private var maxLanguageScore = 0.0

    override val sensitiveVariableKind: String
        get() = "secret"
    override val sensitiveWords: String
        get() = secretWords

    private fun getEntropyDetector(): EntropyDetector {
        if (::entropyDetector.isInitialized.not()) {
            entropyDetector = EntropyDetector(randomnessSensibility)
        }
        return entropyDetector
    }

    override fun isSensitiveStringLiteral(value: String): Boolean {
        return value.isNotEmpty()
                && value.length >= MINIMUM_CREDENTIAL_LENGTH
                && getEntropyDetector().hasEnoughEntropy(value)
                && HumanLanguageDetector.humanLanguageScore(value) < maxLanguageScore()
    }

    private fun maxLanguageScore(): Double {
        if (maxLanguageScore == 0.0) {
            maxLanguageScore = (MAX_RANDOMNESS_SENSIBILITY - randomnessSensibility) * LANGUAGE_SCORE_INCREMENT
        }
        return maxLanguageScore
    }
}
