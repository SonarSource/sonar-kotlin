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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.TokenLocation;

@Rule(key = "S1134")
public class FixMeCommentCheck implements SlangCheck {

  private final Pattern fixMePattern = Pattern.compile("(?i)(^|[[^\\p{L}]&&\\D])(fixme)($|[[^\\p{L}]&&\\D])");

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> tree.allComments().forEach(comment -> {
      Matcher matcher = fixMePattern.matcher(comment.text());
      if (matcher.find()) {
        TextPointer start = comment.textRange().start();
        TokenLocation location = new TokenLocation(
          start.line(),
          start.lineOffset(),
          comment.text().substring(0, matcher.start(2)));
        TextRange fixMeRange = new TextRangeImpl(
          new TextPointerImpl(location.endLine(), location.endLineOffset()),
          new TextPointerImpl(location.endLine(), location.endLineOffset() + 5));
        ctx.reportIssue(fixMeRange, "Take the required action to fix the issue indicated by this \"FIXME\" comment.");
      }
    }));
  }

}
