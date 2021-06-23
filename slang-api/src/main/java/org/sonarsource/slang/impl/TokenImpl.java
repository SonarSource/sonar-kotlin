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
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;

public class TokenImpl implements Token {

  private final TextRange textRange;
  private final String text;
  private final Type type;

  public TokenImpl(TextRange textRange, String text, Type type) {
    this.textRange = textRange;
    this.text = text;
    this.type = type;
  }

  @Override
  public TextRange textRange() {
    return textRange;
  }

  @Override
  public String text() {
    return text;
  }

  @Override
  public Type type() {
    return type;
  }
}
