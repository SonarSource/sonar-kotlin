/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.sonarsource.kotlin.testapi.KotlinVerifier
import java.nio.file.Paths

internal class VoidShouldBeUnitCheckTest : CheckTestWithNoSemantics(
    VoidShouldBeUnitCheck(), shouldReport = true, sampleFileK2 = "VoidShouldBeUnitCheckK2Sample.kt" ) {

    @Test
    fun `should not report on custom Void type`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileSemantics ?: "${checkName}SampleCustomVoid.kt"
        }.verify()
    }

    @Test
    fun `ruling`() {
        KotlinVerifier(check) {

            // sources/kotlin/corda/testing/test-utils/src/test/kotlin/net/corda/testing/internal/RigorousMockTest.kt
            this.baseDir = Paths.get("..", "its", "sources", "kotlin", "corda", "testing", "test-utils",
                "src", "test", "kotlin", "net", "corda", "testing", "internal")
            this.fileName = "RigorousMockTest.kt"
            useK2 = true
        }.verifyNoIssue()
    }

}
