/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.kotlin.gradle.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier

internal class RootProjectNamePresentCheckTest : CheckTest(
    check = RootProjectNamePresentCheck(),
    sampleFileSemantics = "S6625/noncompliant/settings.gradle.kts"
) {

    @Test
    fun `no issues settings`() = verifyNoIssue("S6625/compliant/settings.gradle.kts")

    @Test
    fun `no issues buildscript`() = verifyNoIssue("S6625/compliant/build.gradle.kts")

    @Test
    fun `no issues settings with setter`() = verifyNoIssue("S6625/compliant-setter/settings.gradle.kts")

    private fun verifyNoIssue(fileName: String) {
        KotlinVerifier(check) {
            this.fileName = fileName
            this.baseDir = SAMPLES_BASE_DIR
        }.verifyNoIssue()
    }
}
