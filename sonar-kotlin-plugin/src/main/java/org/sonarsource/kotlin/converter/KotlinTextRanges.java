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

import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;

public class KotlinTextRanges {
  private KotlinTextRanges() {
  }

  @NotNull
  public static TextRange textRange(@NotNull Document psiDocument, @NotNull PsiElement element) {
    TextPointer startPointer = textPointerAtOffset(psiDocument, element.getTextRange().getStartOffset());
    TextPointer endPointer = textPointerAtOffset(psiDocument, element.getTextRange().getEndOffset());
    return new TextRangeImpl(startPointer, endPointer);
  }

  @NotNull
  public static TextPointer textPointerAtOffset(@NotNull Document psiDocument, int startOffset) {
    int startLineNumber = psiDocument.getLineNumber(startOffset);
    int startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber);
    int startLineOffset = startOffset - startLineNumberOffset;
    return new TextPointerImpl(startLineNumber + 1, startLineOffset);
  }
}
