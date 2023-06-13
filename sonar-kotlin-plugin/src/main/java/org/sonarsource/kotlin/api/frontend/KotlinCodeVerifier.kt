package org.sonarsource.kotlin.api.frontend

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.sonarsource.analyzer.commons.recognizers.CamelCaseDetector
import org.sonarsource.analyzer.commons.recognizers.CodeRecognizer
import org.sonarsource.analyzer.commons.recognizers.ContainsDetector
import org.sonarsource.analyzer.commons.recognizers.Detector
import org.sonarsource.analyzer.commons.recognizers.EndWithDetector
import org.sonarsource.analyzer.commons.recognizers.KeywordsDetector
import org.sonarsource.analyzer.commons.recognizers.LanguageFootprint

object KotlinCodeVerifier {

    private val environment = Environment(emptyList(), LanguageVersion.LATEST_STABLE)
    private val codeRecognizer = CodeRecognizer(0.9, KotlinFootprint)

    private val KDOC_TAGS = listOf(
        "@param",
        "@return",
        "@constructor",
        "@receiver",
        "@property",
        "@throws",
        "@exception",
        "@sample",
        "@see",
        "@author",
        "@since",
        "@suppress",
        "`"
    )

    fun containsCode(content: String): Boolean {
        val words = content.trim().split(Regex("\\w+")).filter { it.isNotBlank() }.sumOf { it.trim().length }
        return words > 2 && !isKDoc(content) && codeRecognizer.isLineOfCode(content) && parsingIsSuccessful(content)
    }

    private fun parsingIsSuccessful(content: String) = try {
        val wrappedContent = "fun function () { $content }"
        val ktFile = environment.ktPsiFactory.createFile(wrappedContent)

        ktFile.findDescendantOfType<PsiErrorElement>() == null && !isNonCodeParsedAsCode(ktFile)
    } catch (e: Exception) {
        false
    }

    private fun isKDoc(content: String) = KDOC_TAGS.any { content.lowercase().contains(it) }

    // Filter natural language sentences parsed without errors
    private fun isNonCodeParsedAsCode(tree: PsiFile) =
        tree.lastChild.lastChild.children.let { elements ->
            elements.all { isInfixNotation(it) }
        }

    // Kotlin supports infix function invocation like `1 shl 2` instead of `1.shl(2)`
    // A regular three words sentence would be parsed as infix notation by Kotlin
    private fun isInfixNotation(element: PsiElement) =
        element is KtBinaryExpression && element.getChildren().let { binaryExprChildren ->
            binaryExprChildren.size == 3 && binaryExprChildren[1] is KtOperationReferenceExpression
        }
}

private object KotlinFootprint : LanguageFootprint {
    private val KOTLIN_KEYWORDS = arrayOf(
        "public",
        "abstract",
        "class",
        "return",
        "throw",
        "private",
        "internal",
        "enum",
        "continue",
        "assert",
        "package",
        "Boolean",
        "this",
        "Double",
        "interface",
        "Long",
        "Int",
        "Float",
        "super",
        "true",
        "false",
        "object",
        "companion"
    )

    private val COMMON_KOTLIN_PATTERN_SNIPPETS = arrayOf(
        "for(", "if(", "while(", "catch(", "when(", "try{", "else{", ".let{", ".also{", ".run{", ".apply{", ".map{", ".forEach{", "()"
    )

    private val detectors = setOf(
        EndWithDetector(0.95, '}', '{'),
        KeywordsDetector(0.7, "++", "||", "&&", "+=", "){", "$", "(\"", "\")"),
        KeywordsDetector(0.3, *KOTLIN_KEYWORDS),

        ContainsDetector(0.95, *COMMON_KOTLIN_PATTERN_SNIPPETS),
        CamelCaseDetector(0.25),

        RegexDetector(
            0.95,
            Regex("""\{\s*+\w++\s*+->"""), // matches lamdbas: { foo ->
            Regex("""[\s{(]it\.[a-zA-Z]"""), // matches calls on 'it' in blocks or as args: { it.foo
            Regex("""va[lr]\s++\w++\s*+="""), // matches val/var declarations: val foo =

            // matches function calls with & without string as first arg:
            // foo("foo bar")
            // foo("foo bar",
            // foo(bar)
            // foo(bar,
            // Will (try to) not match sentences with parentheses, e.g. hello(this is a sentence) with some incorrect formatting
            Regex("""\w++\(([^\s,)]++|"[^"]*+")[,)]"""),
        ),
    )

    override fun getDetectors() = detectors
}

/**
 * Does pretty much the same as [org.sonarsource.analyzer.commons.recognizers.RegexDetector] but in a Kotlin-idiomatic way and allowing
 * multiple regular expressions to be specified.
 */
private class RegexDetector(probability: Double, private vararg val regexes: Regex) : Detector(probability) {
    override fun scan(line: String): Int = regexes.sumOf { it.findAll(line).count() }
}
