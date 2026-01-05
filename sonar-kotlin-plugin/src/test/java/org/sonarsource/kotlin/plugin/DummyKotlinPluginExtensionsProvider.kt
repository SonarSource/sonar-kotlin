/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
package org.sonarsource.kotlin.plugin

import com.sonarsource.plugins.kotlin.api.KotlinPluginExtensionsProvider
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

class DummyKotlinPluginExtensionsProvider : KotlinPluginExtensionsProvider {
    companion object {
        const val DUMMY_REPOSITORY_KEY = "dummy"
        const val DUMMY_NON_SONAR_WAY_REPOSITORY_KEY = "dummy2"
    }

    override fun registerKotlinPluginExtensions(extensions: KotlinPluginExtensionsProvider.Extensions) {
        extensions.registerRepository(DUMMY_REPOSITORY_KEY, "Dummy Repository")
        extensions.registerRule(DUMMY_REPOSITORY_KEY, DummyCheck::class.java, true)

        extensions.registerRepository(DUMMY_NON_SONAR_WAY_REPOSITORY_KEY, "Dummy2 Repository")
        extensions.registerRule(DUMMY_NON_SONAR_WAY_REPOSITORY_KEY, DummyCheck::class.java, false)
    }

    @Rule(key = "DummyRule")
    class DummyCheck : AbstractCheck() {
        override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
            kotlinFileContext.reportIssue(function, "Dummy message.")
        }
    }

}
