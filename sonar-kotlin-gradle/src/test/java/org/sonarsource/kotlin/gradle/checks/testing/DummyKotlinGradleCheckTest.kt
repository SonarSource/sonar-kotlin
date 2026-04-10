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
package org.sonarsource.kotlin.gradle.checks.testing

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.gradle.checks.CheckTest
import org.sonarsource.kotlin.gradle.checks.SAMPLES_BASE_DIR
import org.sonarsource.kotlin.testapi.KotlinVerifier

internal class DummyKotlinGradleCheckTest : CheckTest(DummyKotlinGradleCheck()) {

    @Test
    fun `no issues`() {
        KotlinVerifier(check) {
            this.fileName = "DummyKotlinGradleCheckNoIssuesSample.kts"
            this.baseDir = SAMPLES_BASE_DIR
        }.verifyNoIssue()
    }
}
