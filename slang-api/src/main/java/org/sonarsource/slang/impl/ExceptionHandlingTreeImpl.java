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

import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ExceptionHandlingTreeImpl extends BaseTreeImpl implements ExceptionHandlingTree {

  private final Tree tryBlock;
  private final List<CatchTree> catchBlocks;
  private final Tree finallyBlock;
  private final Token tryKeyword;

  public ExceptionHandlingTreeImpl(TreeMetaData metaData, Tree tryBlock, Token tryKeyword, List<CatchTree> catchBlocks, @Nullable Tree finallyBlock) {
    super(metaData);
    this.tryBlock = tryBlock;
    this.catchBlocks = catchBlocks;
    this.finallyBlock = finallyBlock;
    this.tryKeyword = tryKeyword;
  }

  @Override
  public Tree tryBlock() {
    return tryBlock;
  }

  @Override
  public List<CatchTree> catchBlocks() {
    return catchBlocks;
  }

  @CheckForNull
  @Override
  public Tree finallyBlock() {
    return finallyBlock;
  }

  @Override
  public Token tryKeyword() {
    return tryKeyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();

    children.add(tryBlock);
    children.addAll(catchBlocks);

    if (finallyBlock != null) {
      children.add(finallyBlock);
    }

    return children;
  }
}
