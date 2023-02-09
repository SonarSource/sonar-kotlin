/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.Test

class KotlinCoreEnviromentToolsTest {

  @Test
  fun testEmptyBindingContext() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8, null),
      Disposer.newDisposable()
    )

    assertThat(bindingContext(kotlinCoreEnvironment, emptyList(), emptyList()))
      .isEqualTo(BindingContext.EMPTY)
  }

  @Test
  fun testNonEmptyBindingContext() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8, null),
      Disposer.newDisposable()
    )

    assertThat(bindingContext(kotlinCoreEnvironment, listOf(""), emptyList()))
      .isNotEqualTo(BindingContext.EMPTY)
  }
}
