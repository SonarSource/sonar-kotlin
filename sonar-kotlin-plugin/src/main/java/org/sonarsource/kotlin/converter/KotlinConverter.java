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
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.core.CoreFileTypeRegistry;
import org.jetbrains.kotlin.com.intellij.lang.ASTFactory;
import org.jetbrains.kotlin.com.intellij.lang.DefaultASTFactoryImpl;
import org.jetbrains.kotlin.com.intellij.lang.Language;
import org.jetbrains.kotlin.com.intellij.lang.LanguageASTFactory;
import org.jetbrains.kotlin.com.intellij.lang.LanguageParserDefinitions;
import org.jetbrains.kotlin.com.intellij.lang.MetaLanguage;
import org.jetbrains.kotlin.com.intellij.lang.PsiBuilderFactory;
import org.jetbrains.kotlin.com.intellij.lang.impl.PsiBuilderFactoryImpl;
import org.jetbrains.kotlin.com.intellij.mock.MockApplication;
import org.jetbrains.kotlin.com.intellij.mock.MockFileDocumentManagerImpl;
import org.jetbrains.kotlin.com.intellij.mock.MockProject;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.editor.impl.DocumentImpl;
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint;
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions;
import org.jetbrains.kotlin.com.intellij.openapi.fileEditor.FileDocumentManager;
import org.jetbrains.kotlin.com.intellij.openapi.fileTypes.FileTypeRegistry;
import org.jetbrains.kotlin.com.intellij.openapi.progress.ProgressManager;
import org.jetbrains.kotlin.com.intellij.openapi.progress.impl.CoreProgressManager;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.util.StaticGetter;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.PsiManager;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiManagerImpl;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.parsing.KotlinParserDefinition;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

public class KotlinConverter implements ASTConverter {
  private static final PsiFileFactory psiFileFactory = psiFileFactory();

  @Override
  public Tree parse(String content) {
    KotlinTree kotlinTree = new KotlinTree(content);
    KotlinTreeVisitor kotlinTreeVisitor = new KotlinTreeVisitor(kotlinTree.psiFile, kotlinTree.metaDataProvider);
    return kotlinTreeVisitor.getSLangAST();
  }

  private static Stream<PsiElement> descendants(PsiElement element) {
    return Arrays.stream(element.getChildren()).flatMap(
      tree -> Stream.concat(Stream.of(tree), descendants(tree)));
  }

  private static PsiFileFactory psiFileFactory() {
    CoreFileTypeRegistry fileTypeRegistry = new CoreFileTypeRegistry();
    fileTypeRegistry.registerFileType(KotlinFileType.INSTANCE, "kt");
    FileTypeRegistry.ourInstanceGetter = new StaticGetter<>(fileTypeRegistry);

    Disposable disposable = Disposer.newDisposable();

    MockApplication application = new MockApplication(disposable);
    FileDocumentManager fileDocMgr = new MockFileDocumentManagerImpl(DocumentImpl::new, null);
    application.registerService(FileDocumentManager.class, fileDocMgr);
    PsiBuilderFactoryImpl psiBuilderFactory = new PsiBuilderFactoryImpl();
    application.registerService(PsiBuilderFactory.class, psiBuilderFactory);
    application.registerService(ProgressManager.class, new CoreProgressManager());
    ApplicationManager.setApplication(application, FileTypeRegistry.ourInstanceGetter, disposable);

    Extensions.getRootArea().registerExtensionPoint(MetaLanguage.EP_NAME.getName(), MetaLanguage.class.getName(), ExtensionPoint.Kind.INTERFACE);

    MockProject project = new MockProject(null, disposable);

    LanguageParserDefinitions.INSTANCE.addExplicitExtension(KotlinLanguage.INSTANCE, new KotlinParserDefinition());
    ASTFactory astFactory = new DefaultASTFactoryImpl();
    LanguageASTFactory.INSTANCE.addExplicitExtension(KotlinLanguage.INSTANCE, astFactory);
    LanguageASTFactory.INSTANCE.addExplicitExtension(Language.ANY, astFactory);

    PsiManager psiManager = new PsiManagerImpl(project);
    return new PsiFileFactoryImpl(psiManager);
  }

  public static class KotlinTree {
    final PsiFile psiFile;
    final Document document;
    final TreeMetaDataProvider metaDataProvider;


    public KotlinTree(String content) {
      psiFile = psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, normalizeEol(content));
      try {
        document = psiFile.getViewProvider().getDocument();
      } catch (AssertionError e) {
        // A KotlinLexerException may occur when attempting to read invalid files
        throw new ParseException("Cannot correctly map AST with a null Document object");
      }
      CommentAnnotationsAndTokenVisitor commentsAndTokens = new CommentAnnotationsAndTokenVisitor(document);
      psiFile.accept(commentsAndTokens);
      metaDataProvider = new TreeMetaDataProvider(commentsAndTokens.getAllComments(), commentsAndTokens.getTokens(), commentsAndTokens.getAllAnnotations());
      checkParsingErrors(psiFile, document, metaDataProvider);

    }

    private static void checkParsingErrors(PsiFile psiFile, Document document, TreeMetaDataProvider metaDataProvider) {
      descendants(psiFile)
        .filter(element -> element instanceof PsiErrorElement)
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
}
