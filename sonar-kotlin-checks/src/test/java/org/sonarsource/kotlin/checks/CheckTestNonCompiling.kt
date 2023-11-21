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
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.testapi.KotlinVerifier
import java.nio.file.Paths

private const val NON_COMPILING_TEST_FILE_POSTFIX = "SampleNonCompiling.kt"
val NON_COMPILING_BASE_DIR = Paths.get("..", "kotlin-checks-test-sources", "src", "main", "files", "non-compiling", "checks")

abstract class CheckTestNonCompiling(
    check: AbstractCheck,
    sampleFileSemantics: String? = null,
    val sampleFileNonCompiling: String? = null,
    classpath: List<String>? = null,
    dependencies: List<String>? = null,
    val shouldReport: Boolean = false,
) : CheckTest(
    check = check,
    sampleFileSemantics = sampleFileSemantics,
    classpath = classpath,
    dependencies = dependencies
) {
    @Test
    fun `without semantics`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileNonCompiling ?: "$checkName$NON_COMPILING_TEST_FILE_POSTFIX"
            this.baseDir = NON_COMPILING_BASE_DIR
            this.classpath = this@CheckTestNonCompiling.classpath ?: emptyList()
            this.deps = this@CheckTestNonCompiling.dependencies ?: emptyList()
        }.let {
            if (this.shouldReport) it.verify() else it.verifyNoIssue()
        }
    }
}
