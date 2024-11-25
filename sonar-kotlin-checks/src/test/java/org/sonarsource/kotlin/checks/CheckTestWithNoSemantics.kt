/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.testapi.KotlinVerifier

private const val NO_SEMANTICS_TEST_FILE_POSTFIX = "SampleNoSemantics.kt"

abstract class CheckTestWithNoSemantics(
    check: AbstractCheck,
    sampleFileSemantics: String? = null,
    val sampleFileNoSemantics: String? = null,
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
    fun `with empty classpath`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileNoSemantics ?: "$checkName$NO_SEMANTICS_TEST_FILE_POSTFIX"
            this.classpath = emptyList()
            this.deps = emptyList()
        }.let {
            if (this.shouldReport) it.verify() else it.verifyNoIssue()
        }
    }
}
