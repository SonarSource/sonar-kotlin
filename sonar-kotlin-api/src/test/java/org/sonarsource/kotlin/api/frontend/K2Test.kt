/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.descendantsOfType
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

private const val TYPE_RESOLUTION_SAMPLE = """
package org.example

class Cat {
    fun purr() {
        println("Purr purr")
    }
}

fun petAnimal(animal: Any) {
    val isCat = animal is Cat
    if (isCat) {
        animal.purr() // doesn't work in 1.9
    }
}

fun main() {
    val kitty = Cat()
    petAnimal(kitty)
} """

class K2Test {

    @Test
    fun `test with Kotlin 1_9 resolves to Any`() {
        analyzeContent(LanguageVersion.KOTLIN_1_9, TYPE_RESOLUTION_SAMPLE) {
            org.jetbrains.kotlin.analysis.api.analyze(this) {
                val type = ((((declarations[1] as KtNamedFunction).bodyBlockExpression
                    ?.statements!![1] as KtIfExpression).then as KtBlockExpression)
                    .statements[0] as KtDotQualifiedExpression)
                    .receiverExpression.expressionType
                assertThat(type?.isAnyType).isTrue()
            }
        }
    }

    @Test
    fun `test with Kotlin 2+ resolves to correct type Cat`() {
        analyzeContent(LanguageVersion.LATEST_STABLE, TYPE_RESOLUTION_SAMPLE) {
            org.jetbrains.kotlin.analysis.api.analyze(this) {
                val type = ((((declarations[1] as KtNamedFunction).bodyBlockExpression
                    ?.statements!![1] as KtIfExpression).then as KtBlockExpression)
                    .statements[0] as KtDotQualifiedExpression)
                    .receiverExpression.expressionType
                assertThat(type?.symbol?.classId?.asFqNameString()).isEqualTo("org.example.Cat")
            }
        }
    }

    @Test
    fun `test with Kotlin 1_7+ non-exhaustive 'when' is an Error`() {
        analyzeContent(LanguageVersion.LATEST_STABLE, """
                        fun f(s: String){
                            when(s.isEmpty()) {
                                true -> println(s)
                            }
                        }
                    """
        ) {
            org.jetbrains.kotlin.analysis.api.analyze(this) {
                assertThat(collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                    .first().severity).isEqualTo(KaSeverity.ERROR)
            }
        }
    }

    @Test
    fun `test with Kotlin 1_6 non-exhaustive 'when' is a Warning`() {
        analyzeContent(LanguageVersion.KOTLIN_1_6, """
                        fun f(s: String){
                            when(s.isEmpty()) {
                                true -> println(s)
                            }
                        }
                    """
        ) {
            org.jetbrains.kotlin.analysis.api.analyze(this) {
                assertThat(collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                    .first().severity).isEqualTo(KaSeverity.WARNING)
            }
        }
    }

    @Test
    fun `test with Kotlin 1_5 non-exhaustive 'when' is a Warning as 1_5 API is unsupported`() {
        analyzeContent(LanguageVersion.KOTLIN_1_5, """
                        fun f(s: String){
                            when(s.isEmpty()) {
                                true -> println(s)
                            }
                        }
                    """
        ) {
            org.jetbrains.kotlin.analysis.api.analyze(this) {
                assertThat(collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                    .first().severity).isEqualTo(KaSeverity.WARNING)
            }
        }
    }

    @Test
    fun `test Kotlin 1_4 API is unsupported`() {
        analyzeContent(LanguageVersion.KOTLIN_1_4, """
                        sealed interface MyInterface
                    """
        ) {
            org.jetbrains.kotlin.analysis.api.analyze(this) {
                assertThat(collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS))
                    .isEmpty()
            }
        }
    }

    private fun analyzeContent(
        version: LanguageVersion,
        content: String,
        assertion: KtFile.() -> Unit
    ) {
        val disposable = Disposer.newDisposable()
        val compilerConfiguration = compilerConfiguration(
            System.getProperty("java.class.path").split(File.pathSeparatorChar),
            version,
            JvmTarget.JVM_1_8,
            0
        )
        val analysisSession = createK2AnalysisSession(
            disposable,
            compilerConfiguration,
            listOf(
                KotlinVirtualFile(
                    KotlinFileSystem(),
                    File("/fake.kt"), content.trimIndent(),
                )
            ),
        )
        val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
        org.jetbrains.kotlin.analysis.api.analyze(ktFile) {
            ktFile.assertion()
        }
        Disposer.dispose(disposable)
    }

}
