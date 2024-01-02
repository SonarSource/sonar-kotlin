/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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