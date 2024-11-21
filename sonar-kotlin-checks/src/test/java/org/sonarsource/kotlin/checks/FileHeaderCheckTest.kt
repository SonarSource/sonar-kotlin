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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonarsource.kotlin.testapi.KotlinVerifier

class FileHeaderCheckTest {
    @Test
    fun regex() {
        val check = FileHeaderCheck()
        check.isRegularExpression = true
        check.headerFormat = "// copyright 20\\d\\d"
        KotlinVerifier(check) {
            fileName = "FileHeaderCheckSample.kt"
        }.verify()
        KotlinVerifier(check) {
            fileName = "FileHeaderCheckSampleCompliant.kt"
        }.verifyNoIssue()
    }

    @Test
    fun `invalid regex`() {
        val check = FileHeaderCheck()
        check.isRegularExpression = true
        check.headerFormat = "["
        val e = assertThrows<IllegalArgumentException> {
            KotlinVerifier(check) {
                fileName = "FileHeaderCheckSample.kt"
            }.verify()
        }
        assertEquals("[FileHeaderCheck] Unable to compile the regular expression: [", e.message)
    }

    @Test
    fun text() {
        val check = FileHeaderCheck()
        check.headerFormat = "// copyright 2021"
        KotlinVerifier(check) {
            fileName = "FileHeaderCheckSample.kt"
        }.verify()
        KotlinVerifier(check) {
            fileName = "FileHeaderCheckSampleCompliant.kt"
        }.verifyNoIssue()
    }
}
