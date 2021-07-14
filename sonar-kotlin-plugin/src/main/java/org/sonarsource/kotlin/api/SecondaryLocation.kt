package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

data class SecondaryLocation(val textRange: TextRange, val message: String? = null)

fun KotlinFileContext.secondaryOf(psiElement: PsiElement, msg: String? = null) = SecondaryLocation(textRange(psiElement), msg)
