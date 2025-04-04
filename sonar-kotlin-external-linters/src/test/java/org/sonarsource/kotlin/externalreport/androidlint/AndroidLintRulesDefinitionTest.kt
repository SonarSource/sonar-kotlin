/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.kotlin.externalreport.androidlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition

internal class AndroidLintRulesDefinitionTest {
    @Test
    fun android_lint_external_repository() {
        val context = RulesDefinition.Context()
        val rulesDefinition = AndroidLintRulesDefinition()
        rulesDefinition.define(context)
        assertThat(context.repositories()).hasSize(1)
        val repository = context.repository("external_android-lint")
        assertThat(repository!!.name()).isEqualTo("Android Lint")
        assertThat(repository.language()).isEqualTo("kotlin")
        assertThat(repository.isExternal).isTrue
        assertThat(repository.rules().size).isEqualTo(444)
        val rule = repository.rule("AaptCrash")
        assertThat(rule).isNotNull
        assertThat(rule!!.name()).isEqualTo("Potential AAPT crash")
        assertThat(rule.type()).isEqualTo(RuleType.BUG)
        assertThat(rule.severity()).isEqualTo("CRITICAL")
        assertThat(rule.htmlDescription()).isEqualTo(
            """
                <p>
                Vendor: Android Open Source Project
                Contact: https://groups.google.com/g/lint-dev
                Feedback: https://issuetracker.google.com/issues/new?component=192708
                </p>
                <p>
                Defining a style which sets android:id to a dynamically generated id can cause
                many versions of aapt, the resource packaging tool, to crash. To work around
                this, declare the id explicitly with &lt;item type=&quot;id&quot; name=&quot;...&quot; /&gt; instead.
                </p>
                <p>
                </p>
                """.trimIndent())
        assertThat(rule.tags()).containsExactlyInAnyOrder("android")
        assertThat(rule.debtRemediationFunction()!!.baseEffort()).isEqualTo("5min")
    }

    @Test
    fun text_files() {
        assertThat(isTextFile("AndroidManifest.xml")).isTrue
        assertThat(isTextFile("Main.java")).isTrue
        assertThat(isTextFile("App.kt")).isTrue
        assertThat(isTextFile("default.properties")).isTrue
        assertThat(isTextFile("build.gradle")).isTrue
        assertThat(isTextFile("proguard.cfg")).isTrue
        assertThat(isTextFile("proguard-project.txt")).isTrue
    }

    @Test
    fun binary_files() {
        assertThat(isTextFile("App.class")).isFalse
        assertThat(isTextFile("button.png")).isFalse
    }
}
