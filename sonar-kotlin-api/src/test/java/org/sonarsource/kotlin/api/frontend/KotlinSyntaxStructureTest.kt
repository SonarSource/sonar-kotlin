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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.util.Disposer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.readText

internal class KotlinSyntaxStructureTest {
    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `ensure file name is displayed on compiler exception`() {
        val path = Path.of("src/test/resources/api/sample/SimpleClass.kt")

        val expectedException = object : Exception() {}

        mockkStatic(BindingContextUtils::class)
        every { BindingContextUtils.getRecordedTypeInfo(any(), any()) } throws expectedException

        val content = path.readText()
        val environment = Environment(disposable, System.getProperty("java.class.path").split(File.pathSeparatorChar), LanguageVersion.LATEST_STABLE)
        val inputFile = TestInputFileBuilder("moduleKey", path.toString())
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(content).build()

        assertThrows<KotlinExceptionWithAttachments> { kotlinTreeOf(content, environment, inputFile) }.apply {
            assertThat(this)
                .hasCause(expectedException)
                .hasMessageStartingWith("Exception while analyzing expression in (4,17) in ")
                .hasMessageContaining("/moduleKey/src/test/resources/api/sample/SimpleClass.kt")
        }
    }

    @Test
    fun `ensure ktfile name is properly set`() {
        val path = Path.of("src/test/resources/api/sample/SimpleClass.kt")
        val content = path.readText()
        val environment = Environment(disposable, listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"), LanguageVersion.LATEST_STABLE)

        val inputFile = TestInputFileBuilder("moduleKey", path.toString())
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(content)
            .build()

        val (ktFile, _, _) = KotlinSyntaxStructure.of(content, environment, inputFile)
        assertThat(ktFile.containingFile.name).endsWith("/moduleKey/src/test/resources/api/sample/SimpleClass.kt")
    }
}
