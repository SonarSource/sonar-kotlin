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
package org.sonarsource.kotlin.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtPostfixExpression;
import org.jetbrains.kotlin.psi.KtPrefixExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtThisExpression;
import org.sonarsource.slang.api.CodeVerifier;
import org.sonarsource.slang.api.ParseException;

public class KotlinCodeVerifier implements CodeVerifier {
  private static final List<String> KDOC_TAGS = Arrays.asList(
    "@param", "@name", "@return", "@constructor", "@receiver", "@property", "@throws",
    "@exception", "@sample", "@see", "@author", "@since", "@suppress");

  @Override
  public boolean containsCode(String content) {
    int words = content.trim().split("\\w+").length;
    if (words < 2 || isKDoc(content)) {
      return false;
    }
    try {
      String wrappedContent = "fun function () { " + content + " }";
      KotlinConverter.KotlinTree kotlinTree = new KotlinConverter.KotlinTree(wrappedContent);
      return !isSimpleExpression(kotlinTree.psiFile);
    } catch (ParseException e) {
      // do nothing
    }
    return false;
  }

  private static boolean isKDoc(String content) {
    return KDOC_TAGS.stream().anyMatch(tag -> content.toLowerCase(Locale.ENGLISH).contains(tag));
  }

  // Filter natural language sentences parsed
  // as literals, infix notations or single expressions
  private static boolean isSimpleExpression(PsiFile tree) {
    // Since kotlin 1.3, compiler adds 2 hidden elements in the hierarchy: a `KtScript`, having a `KtBlockExpression`
    PsiElement content = getLastChild(getLastChild(getLastChild(tree.getLastChild())));
    if (content == null) {
      throw new IllegalStateException("AST is missing expected elements");
    }
    PsiElement[] elements = content.getChildren();
    return Arrays.stream(elements).allMatch(element ->
      element instanceof KtNameReferenceExpression ||
        element instanceof KtCollectionLiteralExpression ||
        element instanceof KtConstantExpression ||
        element instanceof KtIsExpression ||
        element instanceof KtThisExpression ||
        element instanceof KtStringTemplateExpression ||
        isInfixNotation(element))
      || isSingleExpression(elements);
  }

  @CheckForNull
  private static PsiElement getLastChild(@Nullable PsiElement tree) {
    if (tree != null) {
      return tree.getLastChild();
    }
    return null;
  }

  private static PsiElement[] removeParenthesizedExpressions(PsiElement[] elements) {
    return Arrays.stream(elements)
      .filter(element -> !(element instanceof KtParenthesizedExpression))
      .toArray(PsiElement[]::new);
  }

  // Check for strings parsed as a single expression
  // e.g. "this is fine" as IsExpression, "-- foo" as InfixExpression
  private static boolean isSingleExpression(PsiElement [] elements) {
    PsiElement [] elementsWithoutParenthesis = removeParenthesizedExpressions(elements);
    if (elementsWithoutParenthesis.length == 0) {
      return true;
    }
    if (elementsWithoutParenthesis.length > 1) {
      return false;
    }
    PsiElement element = elementsWithoutParenthesis[0];
    return element instanceof KtPrefixExpression ||
      element instanceof KtPostfixExpression ||
      element instanceof KtBinaryExpression ||
      element instanceof KtBinaryExpressionWithTypeRHS ||
      element instanceof KtDotQualifiedExpression;
  }

  // Kotlin supports infix function invocation like `1 shl 2` instead of `1.shl(2)`
  // A regular three words sentence would be parsed as infix notation by Kotlin
  private static boolean isInfixNotation(PsiElement element) {
    if (element instanceof KtBinaryExpression) {
      PsiElement[] binaryExprChildren = element.getChildren();
      return binaryExprChildren.length == 3 &&
        binaryExprChildren[1] instanceof KtOperationReferenceExpression;
    }
    return false;
  }

}
