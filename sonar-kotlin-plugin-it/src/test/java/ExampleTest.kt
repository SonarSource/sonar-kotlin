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
package org.sonarsource.kotlin.plugin;

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.LanguageVersion;
import org.junit.jupiter.api.Test;
import org.sonarsource.kotlin.api.frontend.Environment;
import org.sonarsource.kotlin.api.frontend.analyzeAndGetBindingContext

internal class ExampleTest {

    @Test
    fun test() {
        val disposable = Disposer.newDisposable()
        val environment = Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE, useK2 = false)
        val ktFile = environment.ktPsiFactory.createFile("example.kt", "")
        analyzeAndGetBindingContext(environment.env, listOf(ktFile))
//        analyze(ktFile) {
//        }
        Disposer.dispose(disposable)
    }

}
