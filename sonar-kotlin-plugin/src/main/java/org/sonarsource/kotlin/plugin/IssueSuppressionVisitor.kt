package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.sonarsource.kotlin.api.asString
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.visiting.KtTreeVisitor
import org.sonarsource.slang.api.TextRange

private val SUPPRESSION_ANNOTATION_NAMES = listOf("Suppress", "SuppressWarnings")

// Common Suppress annotation parameter used by kotlin compiler.
private val COMPILER_KEY_TO_SONAR_KEYS = mapOf(
    "UNUSED_PARAMETER" to sequenceOf("kotlin:S1172"),
    "UNUSED_VARIABLE" to sequenceOf("kotlin:S1481"),
    "UNUSED" to sequenceOf("kotlin:S1172", "kotlin:S1481"),
    "TOO_MANY_ARGUMENTS" to sequenceOf("kotlin:S107")
)

class IssueSuppressionVisitor : KotlinFileVisitor() {
    override fun visit(kotlinFileContext: KotlinFileContext) {
        with(IssueSuppressionTreeVisitor(kotlinFileContext, mutableMapOf())) {
            visitTree(kotlinFileContext.ktFile)
            kotlinFileContext.inputFileContext.setFilteredRules(acc)
        }
    }
}

private class IssueSuppressionTreeVisitor(
    val kotlinFileContext: KotlinFileContext,
    val acc: MutableMap<String, Set<TextRange>>,
) : KtTreeVisitor() {

    override fun visitNamedFunction(function: KtNamedFunction) = detectSuppressedRules(function)
    override fun visitClassOrObject(classOrObject: KtClassOrObject) = detectSuppressedRules(classOrObject)
    override fun visitParameter(parameter: KtParameter) = detectSuppressedRules(parameter)
    override fun visitProperty(property: KtProperty) = detectSuppressedRules(property)

    private fun detectSuppressedRules(node: KtAnnotated) {
        val suppressedRules = detectSuppressedRulesInAnnotation(node.annotationEntries.asSequence())
        val textRange by lazy(mode = LazyThreadSafetyMode.NONE) { kotlinFileContext.textRange(node) }

        suppressedRules.forEach { suppressedRuleKey ->
            acc.compute(suppressedRuleKey) { _, value ->
                if (value == null) {
                    setOf(textRange)
                } else {
                    value + textRange
                }
            }
        }
    }

    private fun detectSuppressedRulesInAnnotation(annotations: Sequence<KtAnnotationEntry>) =
        annotations
            .filter { it.shortName?.asString() in SUPPRESSION_ANNOTATION_NAMES }
            .flatMap { getArgumentsText(it.valueArguments.asSequence()) }
            .flatMap { ruleKey ->
                COMPILER_KEY_TO_SONAR_KEYS[ruleKey.uppercase()] ?: sequenceOf(ruleKey)
            }

    private fun getArgumentsText(args: Sequence<ValueArgument>) =
        args.flatMap { valueArgument ->
            when (val argExpr = valueArgument.getArgumentExpression()) {
                is KtStringTemplateExpression -> sequenceOf(argExpr.asString())
                is KtCollectionLiteralExpression -> argExpr.getInnerExpressions().asSequence()
                    .filterIsInstance<KtStringTemplateExpression>()
                    .map { it.asString() }
                else -> emptySequence()
            }
        }
}
