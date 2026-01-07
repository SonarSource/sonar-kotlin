/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.testapi.KotlinVerifier

private const val NON_ANDROID_TEST_FILE_POSTFIX = "SampleNonAndroid.kt"

abstract class CheckTestForAndroidOnly(
    check: AbstractCheck,
    sampleFileSemantics: String? = null,
    val sampleFileNonAndroid: String? = null,
    classpath: List<String>? = null,
    dependencies: List<String>? = null,
) : CheckTest(
    check = check,
    sampleFileSemantics = sampleFileSemantics,
    classpath = classpath,
    dependencies = dependencies,
    isAndroid = true,
) {
    @Test
    fun `non Android`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileNonAndroid ?: "$checkName$NON_ANDROID_TEST_FILE_POSTFIX"
            this.isAndroid = false
        }.verifyNoIssue()
    }
}
