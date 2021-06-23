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

import java.net.URI;
import java.net.URISyntaxException;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.checks.utils.ExpressionUtils;

import javax.annotation.Nullable;

@Rule(key = "S2068")
public class HardcodedCredentialsCheck implements SlangCheck {

  private static final String DEFAULT_VALUE = "password,passwd,pwd,passphrase";
  private static final Pattern URI_PREFIX = Pattern.compile("^\\w{1,8}://");

  @RuleProperty(
    key = "credentialWords",
    description = "Comma separated list of words identifying potential credentials",
    defaultValue = DEFAULT_VALUE)
  public String credentialWords = DEFAULT_VALUE;

  private List<Pattern> variablePatterns;
  private List<Pattern> literalPatterns;

  @Override
  public void initialize(InitContext init) {
    init.register(AssignmentExpressionTree.class, (ctx, tree) -> {
      Tree leftHandSide = tree.leftHandSide();
      ExpressionUtils.getMemberSelectOrIdentifierName(leftHandSide)
          .ifPresent(variableName -> checkVariable(ctx, leftHandSide, variableName, tree.statementOrExpression()));
    });

    init.register(VariableDeclarationTree.class, (ctx, tree) ->
      checkVariable(ctx, tree.identifier(), tree.identifier().name(), tree.initializer())
    );

    init.register(StringLiteralTree.class, (ctx, tree) -> {
      String content = tree.content();
      if (isURIWithCredentials(content)) {
        ctx.reportIssue(tree, "Review this hard-coded URL, which may contain a credential.");
      } else {
        literalPatterns()
          .map(pattern -> pattern.matcher(content))
          .filter(Matcher::find)
          .map(matcher -> matcher.group(1))
          .filter(match -> !isQuery(content, match))
          .forEach(credential -> report(ctx, tree, credential));
      }
    });
  }

  private static boolean isURIWithCredentials(String stringLiteral) {
    if (URI_PREFIX.matcher(stringLiteral).find()) {
      try {
        String userInfo = new URI(stringLiteral).getUserInfo();
        if (userInfo != null) {
          String[] parts = userInfo.split(":");
          return parts.length > 1 && !parts[0].equals(parts[1]);
        }
      } catch (URISyntaxException e) {
        // ignore, stringLiteral is not a valid URI
      }
    }
    return false;
  }

  private static boolean isNotEmptyString(@Nullable Tree tree) {
    return tree instanceof StringLiteralTree
      && !((StringLiteralTree)tree).content().isEmpty();
  }

  private static boolean isQuery(String value, String match) {
    String followingString = value.substring(value.indexOf(match) + match.length());
    return followingString.startsWith("=?")
      || followingString.startsWith("=%")
      || followingString.startsWith("=:")
      || followingString.startsWith("={") // string format
      || followingString.equals("='");
  }

  private static void report(CheckContext ctx, Tree tree, String matchName) {
    String message = String.format("\"%s\" detected here, make sure this is not a hard-coded credential.", matchName);
    ctx.reportIssue(tree, message);
  }

  private void checkVariable(CheckContext ctx, Tree variable, String variableName, @Nullable Tree value) {
    if (isNotEmptyString(value)) {
      variablePatterns()
        .map(pattern -> pattern.matcher(variableName))
        .filter(Matcher::find)
        .forEach(matcher -> checkAssignedValue(ctx, matcher, variable, ((StringLiteralTree) value).value()));
    }
  }

  private static void checkAssignedValue(CheckContext ctx, Matcher matcher, Tree leftHand, String value) {
    if (!matcher.pattern().matcher(value).find()) {
      report(ctx, leftHand, matcher.group(1));
    }
  }

  private Stream<Pattern> variablePatterns() {
    if (variablePatterns == null) {
      variablePatterns = toPatterns("");
    }
    return variablePatterns.stream();
  }

  private Stream<Pattern> literalPatterns() {
    if (literalPatterns == null) {
      literalPatterns = toPatterns("=\\S");
    }
    return literalPatterns.stream();
  }

  private List<Pattern> toPatterns(String suffix) {
    return Stream.of(credentialWords.split(","))
      .map(String::trim)
      .map(word -> Pattern.compile("(" + word + ")" + suffix, Pattern.CASE_INSENSITIVE))
      .collect(Collectors.toList());
  }

}
