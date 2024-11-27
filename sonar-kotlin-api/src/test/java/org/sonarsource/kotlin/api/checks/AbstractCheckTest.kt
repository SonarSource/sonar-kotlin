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
package org.sonarsource.kotlin.api.checks

import org.assertj.core.api.Assertions.assertThat
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.testapi.KotlinVerifier
import java.util.stream.Stream

class AbstractCheckTest {

    @Rule(key = "Dummy")
    class DummyCheck(val reportingFunction: (DummyCheck, KotlinFileContext, PsiElement) -> Unit) : AbstractCheck() {
        override fun visitNamedFunction(node: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
            reportingFunction(this, kotlinFileContext, node)
        }
    }

    class IssueReportingFunProvider : ArgumentsProvider {
        override fun provideArguments(p0: ExtensionContext?): Stream<out Arguments> = Stream.of(
            Arguments.of({ check: DummyCheck, kotlinFileContext: KotlinFileContext, node: PsiElement ->
                check.apply {
                    kotlinFileContext.reportIssue(
                        kotlinFileContext.textRange(node),
                        "Hello World!"
                    )
                }
            }),
            Arguments.of({ check: DummyCheck, kotlinFileContext: KotlinFileContext, node: PsiElement ->
                check.apply { kotlinFileContext.reportIssue(node, "Hello World!") }
            }),
            Arguments.of({ check: DummyCheck, kotlinFileContext: KotlinFileContext, node: PsiElement ->
                check.apply {
                    kotlinFileContext.reportIssue(
                        node,
                        "Hello World!",
                        listOf(SecondaryLocation(kotlinFileContext.textRange(21, 22, 23, 24), "secondary 2")),
                        2.2
                    )
                }
            })
        )
    }

    @Test
    fun `initialization test`() {
        val dummyCheck = DummyCheck { _, _, _ -> }
        val ruleKey = RuleKey.of("kotlinTest", "X99999")
        dummyCheck.initialize(ruleKey)
        assertThat(dummyCheck.ruleKey).isSameAs(ruleKey)
    }

    @ParameterizedTest
    @ArgumentsSource(IssueReportingFunProvider::class)
    fun `report issues in various ways`(reportingFunction: (DummyCheck, KotlinFileContext, PsiElement) -> Unit) {
        KotlinVerifier(
            DummyCheck(reportingFunction).apply { initialize(RuleKey.of("kotlinTest", "X99999")) }
        ) {
            fileName = "Dummy.kt"
            classpath = emptyList()
        }.verify()
    }
}

