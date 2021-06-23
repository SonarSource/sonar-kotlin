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

import java.util.stream.Collectors;
import org.jetbrains.kotlin.com.intellij.extapi.psi.ASTDelegatePsiElement;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.impl.AnnotationImpl;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;

import static org.jetbrains.kotlin.lexer.KtTokens.BLOCK_COMMENT;
import static org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT;
import static org.jetbrains.kotlin.lexer.KtTokens.EOL_COMMENT;
import static org.jetbrains.kotlin.lexer.KtTokens.SHEBANG_COMMENT;

public class CommentAnnotationsAndTokenVisitor extends KtTreeVisitorVoid {
  private static final int MIN_BLOCK_COMMENT_LENGTH = 4;
  private static final int MIN_DOC_COMMENT_LENGTH = 5;
  private static final int MIN_LINE_COMMENT_LENGTH = 2;
  private static final int BLOCK_COMMENT_PREFIX_LENGTH = 2;
  private static final int BLOCK_COMMENT_SUFFIX_LENGTH = 2;
  private static final int DOC_COMMENT_PREFIX_LENGTH = 3;
  private static final int DOC_COMMENT_SUFFIX_LENGTH = 2;
  private static final int LINE_COMMENT_PREFIX_LENGTH = 2;

  private final Document psiDocument;
  private final List<Comment> allComments = new ArrayList<>();
  private final List<Annotation> allAnnotations = new ArrayList<>();
  private final List<Token> tokens = new ArrayList<>();

  public CommentAnnotationsAndTokenVisitor(Document psiDocument) {
    this.psiDocument = psiDocument;
  }

  @Override
  public void visitElement(@NotNull PsiElement element) {
    if (element instanceof PsiComment) {
      allComments.add(createComment((PsiComment) element));
    } else if (element instanceof LeafPsiElement && !(element instanceof PsiWhiteSpace)) {
      LeafPsiElement leaf = (LeafPsiElement) element;
      String text = leaf.getText();
      Token.Type type = Token.Type.OTHER;
      if (leaf.getElementType() instanceof KtKeywordToken) {
        type = Token.Type.KEYWORD;
      } else if (leaf.getElementType() == KtTokens.REGULAR_STRING_PART) {
        type = Token.Type.STRING_LITERAL;
      }
      tokens.add(new TokenImpl(KotlinTextRanges.textRange(psiDocument, leaf), text, type));
    } else if (element instanceof KtAnnotationEntry) {
      KtAnnotationEntry annotationEntry = (KtAnnotationEntry) element;
      Name shortName = annotationEntry.getShortName();
      if (shortName != null) {
        List<String> argumentsText = annotationEntry.getValueArguments().stream()
          .filter(v -> v instanceof KtValueArgument)
          .map(KtValueArgument.class::cast)
          .map(ASTDelegatePsiElement::getText)
          .collect(Collectors.toList());
        TextRange range = KotlinTextRanges.textRange(psiDocument, element);
        allAnnotations.add(new AnnotationImpl(shortName.asString(), argumentsText, range));
      }
    }
    super.visitElement(element);
  }

  private Comment createComment(@NotNull PsiComment element) {
    String text = element.getText();
    IElementType tokenType = element.getTokenType();
    int length = text.length();
    int prefixLength;
    int suffixLength;

    if (BLOCK_COMMENT.equals(tokenType) && length >= MIN_BLOCK_COMMENT_LENGTH) {
      prefixLength = BLOCK_COMMENT_PREFIX_LENGTH;
      suffixLength = BLOCK_COMMENT_SUFFIX_LENGTH;
    } else if (DOC_COMMENT.equals(tokenType) && length >= MIN_DOC_COMMENT_LENGTH) {
      prefixLength = DOC_COMMENT_PREFIX_LENGTH;
      suffixLength = DOC_COMMENT_SUFFIX_LENGTH;
    } else if ((EOL_COMMENT.equals(tokenType) || SHEBANG_COMMENT.equals(tokenType)) && length >= MIN_LINE_COMMENT_LENGTH) {
      prefixLength = LINE_COMMENT_PREFIX_LENGTH;
      suffixLength = 0;
    } else {
      // FIXME error message: unknown comment type
      prefixLength = 0;
      suffixLength = 0;
    }

    String contentText = text.substring(prefixLength, length - suffixLength);
    TextRange range = KotlinTextRanges.textRange(psiDocument, element);
    TextPointer contentStart = new TextPointerImpl(range.start().line(), range.start().lineOffset() + prefixLength);
    TextPointer contentEnd = new TextPointerImpl(range.end().line(), range.end().lineOffset() - suffixLength);
    TextRange contentRange = new TextRangeImpl(contentStart, contentEnd);
    return new CommentImpl(text, contentText, range, contentRange);
  }

  public List<Comment> getAllComments() {
    return allComments;
  }

  public List<Annotation> getAllAnnotations() {
    return allAnnotations;
  }

  public List<Token> getTokens() {
    return tokens;
  }
}
