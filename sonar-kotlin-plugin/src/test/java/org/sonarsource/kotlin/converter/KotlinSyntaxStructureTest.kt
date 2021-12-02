/*
 * SonarSource Kotlin
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
package org.sonarsource.kotlin.converter

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.utils.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.readText

internal class KotlinSyntaxStructureTest {

    @Test
    fun `ensure file name is displayed on compiler exception`() {
        val path = Path.of("src/test/resources/api/sample/SimpleClass.kt")

        val expectedException = object : Exception() {}

        mockkStatic(BindingContextUtils::class)
        every { BindingContextUtils.getRecordedTypeInfo(any(), any()) } throws expectedException

        val content = path.readText()
        val environment = Environment(System.getProperty("java.class.path").split(":"))
        val inputFile = TestInputFileBuilder("moduleKey", path.toString())
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(content).build()

        assertThrows<KotlinExceptionWithAttachments> { kotlinTreeOf(content, environment, inputFile) }.apply {
            assertThat(this)
                .hasCause(expectedException)
                .hasMessage("Exception while analyzing expression at (4,17) in SimpleClass.kt")
        }

        unmockkAll()
    }

    @Test
    fun `ensure ktfile name is properly set`() {
        val path = Path.of("src/test/resources/api/sample/SimpleClass.kt")
        val content = path.readText()
        val environment = Environment(listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"))

        val inputFile = TestInputFileBuilder("moduleKey", path.toString())
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(content)
            .build()

        val (ktFile, _, _) = KotlinSyntaxStructure.of(content, environment, inputFile)
        assertThat(ktFile.containingFile.name).isEqualTo("SimpleClass.kt")
    }
}