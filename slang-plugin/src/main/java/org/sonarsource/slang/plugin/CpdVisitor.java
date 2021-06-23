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

import java.util.List;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeVisitor;

public class CpdVisitor extends TreeVisitor<InputFileContext> {

  private NewCpdTokens cpdTokens;

  public CpdVisitor() {
    register(TopLevelTree.class, (ctx, tree) -> {
      List<Token> tokens = tree.metaData().tokens();

      boolean foundFirstToken = (tree.firstCpdToken() == null);

      for (Token token : tokens) {
        foundFirstToken = foundFirstToken || (token == tree.firstCpdToken());
        if (foundFirstToken) {
          String text = token.type() == Token.Type.STRING_LITERAL ? "LITERAL" : token.text();
          cpdTokens.addToken(ctx.textRange(token.textRange()), text);
        }
      }
    });
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    cpdTokens = ctx.sensorContext.newCpdTokens().onFile(ctx.inputFile);
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    cpdTokens.save();
  }
}
