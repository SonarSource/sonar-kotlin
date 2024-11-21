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

import org.assertj.core.api.Assertions.assertThat
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.Test
import java.io.File

class KotlinCoreEnvironmentToolsTest {

  @Test
  fun testEmptyBindingContext() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8),
      Disposer.newDisposable()
    )

    assertThat(bindingContext(kotlinCoreEnvironment, emptyList(), emptyList()))
      .isEqualTo(BindingContext.EMPTY)
  }

  @Test
  fun testNonEmptyBindingContext() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8),
      Disposer.newDisposable()
    )

    assertThat(bindingContext(kotlinCoreEnvironment, listOf(""), emptyList()))
      .isNotEqualTo(BindingContext.EMPTY)
  }

  @Test
  fun filter_non_jar_files_from_classpath() {
    val nonJar = File("./build.gradle.kts")
    assertThat(nonJar).exists()
    val nonExisting = File("./nonexisting")
    assertThat(nonExisting).doesNotExist()
    val jar = File("../kotlin-checks-test-sources/build/test-jars/jsr305-3.0.2.jar")
    assertThat(jar).isFile()
    val dir = File(".")

    val configuration = compilerConfiguration(
      listOf(
          nonExisting.path,
          nonJar.path,
          dir.path,
          jar.path,
      ),
      LanguageVersion.KOTLIN_1_4,
      JvmTarget.JVM_1_8,
    )
    assertThat(configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)).containsExactly(
      JvmClasspathRoot(dir),
      JvmClasspathRoot(jar),
    )
  }

}
