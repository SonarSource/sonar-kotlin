/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonarsource.kotlin.verifier.KotlinVerifier
import java.lang.IllegalArgumentException

class FileHeaderCheckTest {
    @Test
    fun slang_regex() {
        val check = org.sonarsource.slang.checks.FileHeaderCheck()
        check.isRegularExpression = true
        check.headerFormat = "// copyright 20\\d\\d"
        KotlinVerifier.verify(
            "../../../../../kotlin-checks-test-sources/src/main/kotlin/checks/FileHeaderCheckSample.kt",
            check)
    }

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
    fun slang_text() {
        val check = org.sonarsource.slang.checks.FileHeaderCheck()
        check.headerFormat = "// copyright 2021"
        KotlinVerifier.verify(
            "../../../../../kotlin-checks-test-sources/src/main/kotlin/checks/FileHeaderCheckSample.kt",
            check)
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
