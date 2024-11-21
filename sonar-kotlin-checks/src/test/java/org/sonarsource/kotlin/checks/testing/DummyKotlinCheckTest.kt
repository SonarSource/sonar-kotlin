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
package org.sonarsource.kotlin.checks.testing

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier

internal class DummyKotlinCheckTest {
    @Test
    fun test() {
        KotlinVerifier(DummyKotlinCheck()) {
            fileName = "Dummy.kt"
            classpath = emptyList()
        }.verify()
    }

    @Test
    fun testNoIssues() {
        KotlinVerifier(DummyKotlinCheck()) {
            fileName = "DummyNoIssues.kt"
        }.verifyNoIssue()
    }
}
