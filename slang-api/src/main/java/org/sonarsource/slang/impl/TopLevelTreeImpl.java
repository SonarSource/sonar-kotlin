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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class TopLevelTreeImpl extends BaseTreeImpl implements TopLevelTree {

  private final List<Tree> declarations;
  private final List<Comment> allComments;
  private final Token firstCpdToken;

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments) {
    this(metaData, declarations, allComments, null);
  }

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments, @Nullable Token firstCpdToken) {
    super(metaData);
    this.declarations = declarations;
    this.allComments = allComments;
    this.firstCpdToken = firstCpdToken;
  }

  @Override
  public List<Tree> declarations() {
    return declarations;
  }

  @Override
  public List<Comment> allComments() {
    return allComments;
  }

  @CheckForNull
  @Override
  public Token firstCpdToken() {
    return firstCpdToken;
  }

  @Override
  public List<Tree> children() {
    return declarations();
  }
}
