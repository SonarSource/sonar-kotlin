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
package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier
import java.io.File
import java.nio.file.Paths

internal class UselessNullCheckCheckTest : CheckTestWithNoSemantics(UselessNullCheckCheck(), shouldReport = true) {
    @Test
    fun `with partial semantics`() {
        KotlinVerifier(check) {
            this.baseDir = Paths.get("..", "kotlin-checks-test-sources", "src", "main", "files", "non-compiling", "checks")
            this.fileName = "${checkName}SampleNonCompiling.kt"
            this.classpath = System.getProperty("java.class.path").split(File.pathSeparatorChar)
            this.deps = emptyList()
        }.verifyNoIssue()
    }
}
