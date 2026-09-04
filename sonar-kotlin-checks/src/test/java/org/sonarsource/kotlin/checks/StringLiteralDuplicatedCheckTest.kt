/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier

class StringLiteralDuplicatedCheckTest : CheckTest(StringLiteralDuplicatedCheck()) {

    @Test
    fun `excludes logging occurrences from the threshold`() {
        KotlinVerifier(check) {
            fileName = "StringLiteralDuplicatedCheckLoggingSample.kt"
        }.verify()
    }

    @Test
    fun `handles exception message occurrences`() {
        KotlinVerifier(check) {
            fileName = "StringLiteralDuplicatedCheckExceptionMessageSample.kt"
        }.verify()
    }

    @Test
    fun `does not analyze test files`() {
        KotlinVerifier(check) {
            fileName = "StringLiteralDuplicatedCheckTestFileSample.kt"
            isTestFile = true
        }.verifyNoIssue()
    }

    @Test
    fun `excludes Compose preview function subtrees`() {
        KotlinVerifier(check) {
            fileName = "StringLiteralDuplicatedCheckComposePreviewSample.kt"
        }.verify()
    }

    @Test
    fun `does not fail with a zero threshold and only suppressed occurrences`() {
        KotlinVerifier(StringLiteralDuplicatedCheck().apply { threshold = 0 }) {
            fileName = "StringLiteralDuplicatedCheckZeroThresholdSample.kt"
        }.verifyNoIssue()
    }
}
