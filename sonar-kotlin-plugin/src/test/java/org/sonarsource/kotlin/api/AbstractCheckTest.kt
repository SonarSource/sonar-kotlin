/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.kotlin.api

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.verifier.KotlinVerifier
import org.sonarsource.slang.checks.api.SecondaryLocation
import org.sonarsource.slang.impl.TextRangeImpl
import java.util.stream.Stream

class AbstractCheckTest {
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
                        KotlinTextRanges.textRange(kotlinFileContext.ktFile.viewProvider.document!!, node),
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
                        listOf(SecondaryLocation(TextRangeImpl(21, 22, 23, 24), "secondary 2")),
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

