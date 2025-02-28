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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.util.Disposer
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
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
