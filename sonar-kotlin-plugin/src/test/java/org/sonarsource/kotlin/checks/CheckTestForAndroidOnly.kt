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

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.verifier.KotlinVerifier

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
