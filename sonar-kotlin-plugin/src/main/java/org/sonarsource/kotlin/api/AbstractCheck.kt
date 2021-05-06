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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtVisitor
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation
import org.sonarsource.slang.api.TextRange as SonarTextRange


abstract class AbstractCheck : KotlinCheck, KtVisitor<Unit, KotlinFileContext>() {
    lateinit var ruleKey: RuleKey
        private set

    override fun initialize(ruleKey: RuleKey) {
        this.ruleKey = ruleKey
    }

    internal fun KotlinFileContext.reportIssue(
        textRange: SonarTextRange? = null,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = inputFileContext.reportIssue(ruleKey, textRange, message, secondaryLocations, gap)


    internal fun KotlinFileContext.reportIssue(
        psiElement: PsiElement,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null,
    ) = ktFile.viewProvider.document?.let { document ->
        reportIssue(KotlinTextRanges.textRange(document, psiElement), message, secondaryLocations, gap)
    }
}




