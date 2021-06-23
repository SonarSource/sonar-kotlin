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
package org.sonarsource.slang.plugin;

import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeVisitor;

import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CONSTANT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.STRING;

public class SyntaxHighlighter extends TreeVisitor<InputFileContext> {

  private NewHighlighting newHighlighting;

  public SyntaxHighlighter() {
    register(TopLevelTree.class, (ctx, tree) -> {
      tree.allComments().forEach(
        comment -> highlight(ctx, comment.textRange(), COMMENT));
      tree.metaData().tokens().stream()
        .filter(t -> t.type() == Token.Type.KEYWORD)
        .forEach(token -> highlight(ctx, token.textRange(), KEYWORD));
    });

    register(LiteralTree.class, (ctx, tree) ->
      highlight(ctx, tree.metaData().textRange(), tree instanceof StringLiteralTree ? STRING : CONSTANT));
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    newHighlighting = ctx.sensorContext.newHighlighting()
      .onFile(ctx.inputFile);
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    newHighlighting.save();
  }

  private void highlight(InputFileContext ctx, TextRange range, TypeOfText typeOfText) {
    newHighlighting.highlight(ctx.textRange(range), typeOfText);
  }

}
