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
import org.jetbrains.kotlin.analysis.api.components.KaCompilationOptions
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
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
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import org.junit.jupiter.api.Assertions.assertNull
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
                val options = mockkClass(KaCompilationOptions::class)
                assertThrows<UnsupportedOperationException> {
                    this.compile(ktFile, options)
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

    @Test
    fun `resolveToCall should return null instead of raising exception on resolve error`() {
        assertRecoversFromResolveError({ it.isVarCall().resolveToCall() }) { assertNull(it) }
    }

    @Test
    fun `resolveToSymbol should return null instead of raising exception on resolve error`() {
        assertRecoversFromResolveError({ it.isVarCall().calleeExpression!!.mainReference!!.resolveToSymbol() }) { assertNull(it) }
    }

    @Test
    fun `expressionType should return null instead of raising exception on resolve error`() {
        assertRecoversFromResolveError({ it.isVarCall().expressionType }) { assertNull(it) }
    }

    @Test
    fun `type should return KaErrorType instead of raising exception on resolve error`() {
        assertRecoversFromResolveError({ it.collectDescendantsOfType<KtTypeReference>().last().type }) { assertTrue(it is KaErrorType) }
    }

    @Test
    fun `returnType should return KaErrorType instead of raising exception on resolve error`() {
        assertRecoversFromResolveError({ it.collectDescendantsOfType<KtProperty>().single().returnType }) { assertTrue(it is KaErrorType) }
    }

    @Test
    fun `collectDiagnostics should return no diagnostics instead of raising exception on resolve error`() {
        assertRecoversFromResolveError({ it.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS) }) { assertTrue(it.isEmpty()) }
    }

    /**
     * Since Kotlin 2.4.21 resolving body of a function which contains smart cast to intersection type raises exception.
     * When original session stops raising it, workarounds in [SonarKaSession] can be removed.
     */
    private fun <R> assertRecoversFromResolveError(action: KaSession.(KtFile) -> R, assertFallback: (R) -> Unit) {
        val ktFile = ktFile(
            """
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor

fun isVar(target: DeclarationDescriptor?): Boolean? {
    when (target) {
        is VariableDescriptor -> 1
        is ClassDescriptor -> 2
        else -> return null
    }
    val result = (target as? VariableDescriptor)?.let { it.isVar() }
    return result
}
        """.trimIndent(),
            classpath = listOf(File(VariableDescriptor::class.java.protectionDomain.codeSource.location.toURI()).path),
        )

        assertThrows<KotlinIllegalArgumentExceptionWithAttachments> {
            analyze(ktFile) {
                action(ktFile)
            }
        }

        kaSession(ktFile) {
            assertFallback(withKaSession { action(ktFile) })
        }
    }

    private fun KtFile.isVarCall(): KtCallExpression =
        collectDescendantsOfType<KtCallExpression>().first { it.text == "isVar()" }

    private fun ktFile(content: String, classpath: List<String> = listOf()): KtFile {
        val analysisSession = createK2AnalysisSession(
            disposable,
            compilerConfiguration(
                classpath,
                LanguageVersion.LATEST_STABLE,
                JvmTarget.JVM_1_8,
            ),
            listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), contentProvider = { content }))
        )
        return analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    }

}
