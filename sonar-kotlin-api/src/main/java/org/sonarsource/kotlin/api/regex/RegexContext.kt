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
package org.sonarsource.kotlin.api.regex

import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.sonarsource.analyzer.commons.regex.RegexIssueLocation
import org.sonarsource.analyzer.commons.regex.ast.FlagSet
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement
import org.sonarsource.kotlin.api.frontend.KotlinAnalyzerRegexSource
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.RegexCache

class RegexContext(
    private val stringTemplateEntries: Iterable<KtStringTemplateEntry>,
    kotlinFileContext: KotlinFileContext,
) {
    private val cache: RegexCache = kotlinFileContext.regexCache

    private val _reportedIssues = mutableListOf<ReportedIssue>()
    val reportedIssues: List<ReportedIssue>
        get() = _reportedIssues

    val regexSource = KotlinAnalyzerRegexSource(
        stringTemplateEntries,
        kotlinFileContext.inputFileContext.inputFile,
        kotlinFileContext.ktFile.viewProvider.document!!
    )

    fun parseRegex(flags: FlagSet) = cache.addIfAbsent(stringTemplateEntries, flags, regexSource)

    fun reportIssue(
        regexElement: RegexSyntaxElement,
        message: String,
        secondaryLocations: List<RegexIssueLocation> = emptyList(),
        gap: Double? = null
    ) = _reportedIssues.add(ReportedIssue(regexElement, message, secondaryLocations, gap))

    fun reportIssue(
        regexElement: RegexSyntaxElement,
        message: String,
        gap: Int?,
        secondaryLocations: List<RegexIssueLocation>
    ) = reportIssue(regexElement, message, secondaryLocations, gap?.toDouble())
}

data class ReportedIssue(
    val regexElement: RegexSyntaxElement,
    val message: String,
    val secondaryLocations: List<RegexIssueLocation>,
    val gap: Double?
)
