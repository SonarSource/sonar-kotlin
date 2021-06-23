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

import org.sonarsource.slang.api.CodeVerifier;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextRanges;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;

@Rule(key = "S125")
public class CommentedCodeCheck implements SlangCheck {
  private CodeVerifier codeVerifier;

  public CommentedCodeCheck(CodeVerifier codeVerifier) {
    this.codeVerifier = codeVerifier;
  }

  private static final String MESSAGE = "Remove this commented out code.";

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      List<List<Comment>> groupedComments =
        groupComments(tree.allComments());
      groupedComments.forEach(comments -> {
        String content = comments.stream()
          .map(Comment::contentText)
          .collect(Collectors.joining("\n"));
        if (codeVerifier.containsCode(content)) {
          List<TextRange> textRanges = comments.stream()
            .map(HasTextRange::textRange)
            .collect(Collectors.toList());
          ctx.reportIssue(TextRanges.merge(textRanges), MESSAGE);
        }
      });
    });
  }

  private static List<List<Comment>> groupComments(List<Comment> comments) {
    List<List<Comment>> groups = new ArrayList<>();
    List<Comment> currentGroup = null;
    for (Comment comment : comments) {
      if (currentGroup == null) {
        currentGroup = initNewGroup(comment);
      } else if (areAdjacent(currentGroup.get(currentGroup.size() - 1), comment)) {
        currentGroup.add(comment);
      } else {
        groups.add(currentGroup);
        currentGroup = initNewGroup(comment);
      }
    }
    if (currentGroup != null) {
      groups.add(currentGroup);
    }
    return groups;
  }

  private static List<Comment> initNewGroup(Comment comment) {
    List<Comment> group = new ArrayList<>();
    group.add(comment);
    return group;
  }

  private static boolean areAdjacent(Comment commentA, Comment commentB) {
    return commentA.textRange().start().line() + 1 == commentB.textRange().start().line();
  }

}
