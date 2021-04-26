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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.config.LanguageVersion;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

import java.util.Arrays;
import java.util.Collections;

import static org.sonarsource.kotlin.converter.KotlinCoreEnvironmentToolsKt.compilerConfiguration;
import static org.sonarsource.kotlin.converter.KotlinCoreEnvironmentToolsKt.kotlinCoreEnvironment;

public class KotlinTree {
  final PsiFile psiFile;
  final Document document;
  final TreeMetaDataProvider metaDataProvider;
  final BindingContext bindingContext;

  private static Disposable disposable = Disposer.newDisposable();
  private static KotlinCoreEnvironment environment = kotlinCoreEnvironment(
    compilerConfiguration(
      Collections.emptyList(),
      LanguageVersion.KOTLIN_1_4,
      JvmTarget.JVM_1_8
    ),
    disposable
  );
  private static KtPsiFactory ktPsiFactory = new KtPsiFactory(environment.getProject(), false);

  public KotlinTree(String content) {
    KtFile ktFile = ktPsiFactory.createFile(normalizeEol(content));
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
      environment,
      // FIXME Add classpath
      Arrays.asList(""),
      Collections.singletonList(ktFile));
  }

  private static void checkParsingErrors(PsiFile psiFile, Document document, TreeMetaDataProvider metaDataProvider) {
    KotlinConverter.descendants(psiFile)
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
}
