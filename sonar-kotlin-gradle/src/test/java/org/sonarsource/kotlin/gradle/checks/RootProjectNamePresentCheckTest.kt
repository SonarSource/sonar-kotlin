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
package org.sonarsource.kotlin.gradle.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier

internal class RootProjectNamePresentCheckTest : CheckTest(
    check = RootProjectNamePresentCheck(),
    sampleFileSemantics = "S6625/noncompliant/settings.gradle.kts"
) {

    @Test
    fun `no issues settings`() {
        KotlinVerifier(check) {
            this.fileName = "S6625/compliant/settings.gradle.kts"
            this.baseDir = SAMPLES_BASE_DIR
        }.verifyNoIssue()
    }

    @Test
    fun `no issues buildscript`() {
        KotlinVerifier(check) {
            this.fileName = "S6625/compliant/build.gradle.kts"
            this.baseDir = SAMPLES_BASE_DIR
        }.verifyNoIssue()
    }
}
