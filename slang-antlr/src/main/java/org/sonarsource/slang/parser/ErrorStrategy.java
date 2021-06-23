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
package org.sonarsource.slang.parser;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.impl.TextPointerImpl;

public class ErrorStrategy extends DefaultErrorStrategy {

  @Override
  public void reportError(Parser recognizer, RecognitionException e) {
    Token t = recognizer.getCurrentToken();
    String errorMessage = String.format(
      "Unexpected parsing error occurred. Last found valid token: %s at position %s:%s",
      getTokenErrorDisplay(t),
      t.getLine(),
      t.getCharPositionInLine());
    TextPointer textPointer = new TextPointerImpl(t.getLine(), t.getCharPositionInLine());
    throw new ParseException(errorMessage, textPointer);
  }

  @Override
  public Token recoverInline(Parser recognizer) {
    Token matchedSymbol = singleTokenDeletion(recognizer);
    if (matchedSymbol != null) {
      String errorMessage = String.format(
        "Unexpected token found: %s at position %s:%s",
        matchedSymbol.getText(),
        matchedSymbol.getLine(),
        matchedSymbol.getCharPositionInLine());
      throw new ParseException(errorMessage);
    }

    singleTokenInsertion(recognizer);

    throw new ParseException("Unexpected parsing error");
  }

  @Override
  protected void reportMissingToken(Parser recognizer) {
    Token t = recognizer.getCurrentToken();
    IntervalSet expecting = getExpectedTokens(recognizer);
    String errorMessage = String.format(
      "missing %s before %s at position %s:%s",
      expecting.toString(recognizer.getVocabulary()),
      getTokenErrorDisplay(t),
      t.getLine(),
      t.getCharPositionInLine());
    throw new ParseException(errorMessage);
  }

}
