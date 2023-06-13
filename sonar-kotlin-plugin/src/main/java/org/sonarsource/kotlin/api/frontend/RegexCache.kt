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