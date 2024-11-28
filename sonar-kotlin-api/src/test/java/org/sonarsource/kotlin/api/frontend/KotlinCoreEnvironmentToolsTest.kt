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
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class KotlinCoreEnvironmentToolsTest {

  private val disposable = Disposer.newDisposable()

  @AfterEach
  fun dispose() {
    Disposer.dispose(disposable)
  }

  @Test
  fun testNonEmptyBindingContext() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8),
      disposable
    )

    assertThat(analyzeAndGetBindingContext(kotlinCoreEnvironment, emptyList()))
      .isNotEqualTo(BindingContext.EMPTY)
  }

}
