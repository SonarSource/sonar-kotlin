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
import java.util.Collections;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

public class KotlinTree {
  final PsiFile psiFile;
  final Document document;
  final TreeMetaDataProvider metaDataProvider;
  final BindingContext bindingContext;

  public KotlinTree(String content, Environment environment) {
    KtFile ktFile = environment.getKtPsiFactory().createFile(normalizeEol(content));
    psiFile = ktFile;
    try {
      document = psiFile
        .getViewProvider()
        .getDocument();
    } catch (AssertionError e) {
      // A KotlinLexerException may occur when attempting to read invalid files
      throw new ParseException("Cannot correctly map AST with a null Document object");
    }
    CommentAnnotationsAndTokenVisitor commentsAndTokens = new CommentAnnotationsAndTokenVisitor(document);
    psiFile.accept(commentsAndTokens);
    metaDataProvider = new TreeMetaDataProvider(commentsAndTokens.getAllComments(), commentsAndTokens.getTokens(), commentsAndTokens.getAllAnnotations());
    checkParsingErrors(psiFile, document, metaDataProvider);

    bindingContext = KotlinCoreEnvironmentToolsKt.bindingContext(
      environment.getEnv(),
      environment.getClasspath(),
      Collections.singletonList(ktFile));
  }

  private static void checkParsingErrors(PsiFile psiFile, Document document, TreeMetaDataProvider metaDataProvider) {
    descendants(psiFile)
      .filter(PsiErrorElement.class::isInstance)
      .findFirst()
      .ifPresent(element -> {
        throw new ParseException("Cannot convert file due to syntactic errors",
          getErrorLocation(document, metaDataProvider, element));
      });
  }

  private static TextPointer getErrorLocation(Document document, TreeMetaDataProvider metaDataProvider, PsiElement element) {
    return metaDataProvider.metaData(KotlinTextRanges.textRange(document, element)).textRange().start();
  }

  @NotNull
  private static String normalizeEol(String content) {
    return content.replaceAll("\\r\\n?", "\n");
  }

  private static Stream<PsiElement> descendants(PsiElement element) {
    return Arrays.stream(element.getChildren())
      .flatMap(tree -> Stream.concat(Stream.of(tree), descendants(tree)));
  }
}
