package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiKeyword
import org.jetbrains.kotlin.com.intellij.psi.PsiLiteral
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.sonar.api.batch.sensor.highlighting.NewHighlighting
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonarsource.kotlin.converter.KotlinTextRanges.commonTextRange
import org.sonarsource.kotlin.visiting.KotlinFileVisitor

class SyntaxHighlighter : KotlinFileVisitor() {
    override fun visit(kotlinFileContext: KotlinFileContext) {
        val newHighlighting = kotlinFileContext.inputFileContext.sensorContext.newHighlighting()
            .onFile(kotlinFileContext.inputFileContext.inputFile)
        highlightElementsRec(kotlinFileContext.ktFile, newHighlighting, kotlinFileContext)
        newHighlighting.save()
    }

    private fun highlightElementsRec(node: PsiElement, newHighlighting: NewHighlighting, context: KotlinFileContext) {
        val typeOfText = determineTypeOfText(node)
        if (typeOfText != null) {
            newHighlighting.highlight(context.commonTextRange(node), typeOfText)
        } else {
            node.allChildren.forEach { highlightElementsRec(it, newHighlighting, context) }
        }
    }

    private fun determineTypeOfText(node: PsiElement) =
        when (node) {
            is PsiAnnotation -> TypeOfText.ANNOTATION
            is KtAnnotationEntry -> TypeOfText.ANNOTATION
            is PsiDocComment -> TypeOfText.STRUCTURED_COMMENT
            is KDoc -> TypeOfText.STRUCTURED_COMMENT
            is PsiComment -> TypeOfText.COMMENT
            is PsiKeyword -> TypeOfText.KEYWORD
            is PsiLiteral -> TypeOfText.CONSTANT
            is KtConstantExpression -> TypeOfText.CONSTANT
            is KtStringTemplateExpression -> TypeOfText.STRING
            is LeafPsiElement -> determineTypeOfText(node)
            else -> null
        }

    private fun determineTypeOfText(node: LeafPsiElement) =
        if (node.elementType is KtKeywordToken) TypeOfText.KEYWORD
        else null
}
