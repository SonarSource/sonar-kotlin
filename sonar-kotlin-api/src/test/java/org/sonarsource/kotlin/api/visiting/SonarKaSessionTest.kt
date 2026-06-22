/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow

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

    @Test
    fun `should return KaErrorType instead of raising exception when obtaining type for KtTypeReference`() {
        val ktFile = ktFile(
            """
//class ObservableList<K> {
//    fun addListener(listener: Listener<K>) {
//    }
//
//    fun interface Listener<K> {
//        fun callback(change: Change<out K>)
//
//        interface Change<K>
//    }
//}

fun <K> example(list: ObservableList<K>) {
    list.addListener { _: ObservableList.Listener.Change<out K> ->
    }
}
        """.trimIndent()
        )

        val action: KaSession.() -> KaType = {
            ktFile.collectDescendantsOfType<KtTypeReference>()[2].type
        }

        kaSession(ktFile) {
            val type = withKaSession(action)
            assertTrue(type is KaErrorType)
        }

        assertDoesNotThrow {
            analyze(ktFile) {
                action()
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
