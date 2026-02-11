/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.api.visiting

import com.intellij.openapi.util.Disposer
import io.mockk.mockkClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaCompilerTarget
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonarsource.kotlin.api.frontend.KotlinFileSystem
import org.sonarsource.kotlin.api.frontend.KotlinVirtualFile
import org.sonarsource.kotlin.api.frontend.compilerConfiguration
import org.sonarsource.kotlin.api.frontend.createK2AnalysisSession
import java.io.File

private class SonarKaSessionTest {

    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    @OptIn(KaExperimentalApi::class)
    @Test
    fun `compile should always throw UnsupportedOperationException`() {
        val ktFile = ktFile("")
        kaSession(ktFile) {
            withKaSession {
                val c = mockkClass(CompilerConfiguration::class)
                val t = mockkClass(KaCompilerTarget::class)
                assertThrows<UnsupportedOperationException> {
                    this.compile(ktFile, c, t) { _ -> false }
                }
            }
        }
    }

    private fun ktFile(content: String): KtFile {
        val analysisSession = createK2AnalysisSession(
            disposable,
            compilerConfiguration(
                listOf(),
                LanguageVersion.LATEST_STABLE,
                JvmTarget.JVM_1_8,
            ),
            listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), contentProvider = { content }))
        )
        return analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    }

}
