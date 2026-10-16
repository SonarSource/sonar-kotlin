/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.api.rule.RuleKey
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val COMPOSABLE_ANNOTATION = "Composable"
private const val PREVIEW_ANNOTATION = "Preview"

/**
 * Simple names of the annotations marking a function as a test or as a test lifecycle hook, covering JUnit 4 and 5,
 * kotlin.test and TestNG. The name of such a function describes a scenario instead of being used to call it, so any
 * naming style is appropriate. Matching on the simple name also accepts the annotations that projects compose or
 * alias from these, which carry the same intent.
 */
private val TEST_ANNOTATION_NAMES = setOf(
    "Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate", "Theory",
    "Before", "After", "BeforeEach", "AfterEach", "BeforeAll", "AfterAll",
    "BeforeClass", "AfterClass", "BeforeTest", "AfterTest", "BeforeMethod", "AfterMethod",
    "BeforeSuite", "AfterSuite", "BeforeGroups", "AfterGroups",
)

private val PASCAL_CASE_REGEX = Regex("^[A-Z][a-zA-Z0-9]*$")
private val CAMEL_CASE_REGEX = Regex("^[a-z][a-zA-Z0-9]*$")

@Rule(key = "S100")
class BadFunctionNameCheck : AbstractCheck() {

    companion object {
        const val DEFAULT_FORMAT = "^[a-zA-Z][a-zA-Z0-9]*$"
    }

    @RuleProperty(
        key = "format",
        description = "Regular expression used to check the function names against.",
        defaultValue = DEFAULT_FORMAT,
    )
    var format: String = DEFAULT_FORMAT

    private lateinit var formatRegex: Regex

    override fun initialize(ruleKey: RuleKey) {
        super.initialize(ruleKey)
        formatRegex = Regex(format)
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val name = function.name ?: /* in case of anonymous functions */ return
        if (function.hasModifier(KtTokens.EXTERNAL_KEYWORD)) return
        if (function.overrides()) return
        if (hasAnnotation(function, TEST_ANNOTATION_NAMES)) return
        if (isBacktickedComposablePreview(function)) return
        if (name.matches(formatRegex)) return
        if (hasAnnotation(function, COMPOSABLE_ANNOTATION)) {
            checkComposableNaming(function, name, kotlinFileContext)
        } else {
            kotlinFileContext.reportIssue(
                function.nameIdentifier!!,
                """Rename function "$name" to match the regular expression $format"""
            )
        }
    }

    private fun checkComposableNaming(function: KtNamedFunction, name: String, kotlinFileContext: KotlinFileContext) {
        if (function.isLocal) return
        val returnsUnit = withKaSession { function.returnType.isUnitType }
        val isPrivateOrInternal = function.hasModifier(KtTokens.PRIVATE_KEYWORD) || function.hasModifier(KtTokens.INTERNAL_KEYWORD)

        val (follows, convention) = when {
            returnsUnit && isPrivateOrInternal ->
                (name.matches(PASCAL_CASE_REGEX) || name.matches(CAMEL_CASE_REGEX)) to "PascalCase or camelCase"
            returnsUnit ->
                name.matches(PASCAL_CASE_REGEX) to "PascalCase"
            else ->
                name.matches(CAMEL_CASE_REGEX) to "camelCase"
        }

        if (!follows) {
            kotlinFileContext.reportIssue(
                function.nameIdentifier!!,
                """Rename function "$name" to use $convention"""
            )
        }
    }

    /**
     * A `@Preview` composable is rendered in the Android Studio preview panel under its own name, which makes a
     * backtick name a human-readable label there.
     */
    private fun isBacktickedComposablePreview(function: KtNamedFunction): Boolean {
        val nameIdentifierText = function.nameIdentifier?.text ?: return false
        if (!nameIdentifierText.startsWith('`')) return false

        return hasAnnotation(function, PREVIEW_ANNOTATION) && hasAnnotation(function, COMPOSABLE_ANNOTATION)
    }

    private fun hasAnnotation(function: KtNamedFunction, annotation: String): Boolean =
        function.annotationEntries.any { annotation == it.shortName?.asString() }

    private fun hasAnnotation(function: KtNamedFunction, annotations: Set<String>): Boolean =
        function.annotationEntries.any { annotations.contains(it.shortName?.asString()) }

}
