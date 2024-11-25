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

import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.RegexParser
import org.sonarsource.analyzer.commons.regex.ast.FlagSet

class RegexCache(
    private val globalCache: MutableMap<Pair<List<KtStringTemplateEntry>, Int>, RegexParseResult> = mutableMapOf()
) {
    fun addIfAbsent(
        stringTemplateEntries: Iterable<KtStringTemplateEntry>,
        flags: FlagSet,
        regexSource: KotlinAnalyzerRegexSource,
    ) =
        globalCache.computeIfAbsent(stringTemplateEntries.toList() to flags.mask) {
            RegexParser(regexSource, flags).parse()
        }
}