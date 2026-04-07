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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.util.Disposer
import java.io.File
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.sonarsource.kotlin.api.visiting.kaSession
import org.sonarsource.kotlin.api.visiting.withKaSession

class KotlinCoreEnvironmentToolsTest {

  private val disposable = Disposer.newDisposable()

  @AfterEach
  fun dispose() {
    Disposer.dispose(disposable)
  }

  // https://kotlinlang.org/docs/whatsnew20.html#smart-cast-improvements
  private val content = """
    fun example(any: Any) {
      val isString = any is String
      if (isString) {
        any.length
      }
    }
    """.trimIndent()

  @Test
  fun k2() {
    val analysisSession = createK2AnalysisSession(
      disposable,
      compilerConfiguration(
        listOf(),
        LanguageVersion.LATEST_STABLE,
        JvmTarget.JVM_1_8,
      ),
      listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), contentProvider = { content })),
    )
    val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    kaSession(ktFile) {
      withKaSession {
        assertThat(ktFile.findDescendantOfType<KtIsExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Boolean")
        assertThat(ktFile.findDescendantOfType<KtDotQualifiedExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Int")
      }
    }
  }

  @Test
  fun `k2 resolving Java class extending Kotlin source class should not throw`(@TempDir tempDir: Path) {
    // FooImpl.java needs AbstractFoo to be on the compilation classpath.
    // We use a Java stub here — the K2 analysis session will later resolve AbstractFoo from
    // the Kotlin source module (KotlinVirtualFile), not from this stub class.
    val stubOutputDir = tempDir.resolve("stub-out").apply { createDirectories() }
    val javaOutputDir = tempDir.resolve("java-out").apply { createDirectories() }
    val javaCompiler = ToolProvider.getSystemJavaCompiler()

    val abstractFooJavaStub = tempDir.resolve("AbstractFoo.java").apply {
      writeText("public abstract class AbstractFoo {}")
    }
    check(javaCompiler.run(null, null, null,
      "-d", stubOutputDir.toString(),
      abstractFooJavaStub.toString()
    ) == 0) { "Failed to compile AbstractFoo.java stub" }

    // Compile FooImpl.java against the AbstractFoo stub
    val fooImplJava = tempDir.resolve("FooImpl.java").apply {
      writeText("public class FooImpl extends AbstractFoo { public FooImpl() {} }")
    }
    check(javaCompiler.run(null, null, null,
      "-cp", stubOutputDir.toString(),
      "-d", javaOutputDir.toString(),
      fooImplJava.toString()
    ) == 0) { "Failed to compile FooImpl.java" }

    // Set up K2 session: AbstractFoo.kt and createFoo.kt are Kotlin sources;
    // only FooImpl.class is on the classpath (AbstractFoo.class is intentionally absent).
    val fileSystem = KotlinFileSystem()
    val abstractFooVF = KotlinVirtualFile(fileSystem, File("/AbstractFoo.kt"), contentProvider = { "abstract class AbstractFoo" })
    val createFooVF = KotlinVirtualFile(fileSystem, File("/createFoo.kt"), contentProvider = { "fun createFoo(): AbstractFoo = FooImpl()" })

    val session = createK2AnalysisSession(
      disposable,
      compilerConfiguration(
          classpath = listOf(javaOutputDir.toString()),
          languageVersion = LanguageVersion.KOTLIN_1_9,
          jvmTarget = JvmTarget.JVM_1_8
      ),
      listOf(abstractFooVF, createFooVF),
    )

    val createFooKtFile = session.modulesWithFiles.entries.first().value
      .filterIsInstance<KtFile>().first { it.name == "createFoo.kt" }

    kaSession(createFooKtFile) {
      withKaSession {
        val call = createFooKtFile.findDescendantOfType<KtCallExpression>()!!
        assertThatCode { call.resolveToCall() }.doesNotThrowAnyException()
      }
    }
  }

  @Test
  fun k2_language_version_1_9() {
    val analysisSession = createK2AnalysisSession(
      disposable,
      compilerConfiguration(
        listOf(),
        LanguageVersion.KOTLIN_1_9,
        JvmTarget.JVM_1_8,
      ),
      listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), contentProvider = { content })),
    )
    val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    kaSession(ktFile) {
      withKaSession {
        assertThat(ktFile.findDescendantOfType<KtIsExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Boolean")
        assertThat(ktFile.findDescendantOfType<KtDotQualifiedExpression>()!!.expressionType is KaErrorType)
          .isTrue()
      }
    }
  }

}
