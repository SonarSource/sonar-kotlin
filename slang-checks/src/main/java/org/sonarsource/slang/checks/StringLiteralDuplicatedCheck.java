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

import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S1192")
public class StringLiteralDuplicatedCheck implements SlangCheck {

  private static final int DEFAULT_THRESHOLD = 3;
  private static final int MINIMAL_LITERAL_LENGTH = 5;
  private static final Pattern NO_SEPARATOR_REGEXP = Pattern.compile("\\w++");

  @RuleProperty(
    key = "threshold",
    description = "Number of times a literal must be duplicated to trigger an issue",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      Map<String, List<StringLiteralTree>> occurrences = new HashMap<>();
      tree.descendants()
        .filter(StringLiteralTree.class::isInstance)
        .map(StringLiteralTree.class::cast)
        .filter(literal -> literal.content().length() > MINIMAL_LITERAL_LENGTH && !NO_SEPARATOR_REGEXP.matcher(literal.content()).matches())
        .forEach(literal -> occurrences.computeIfAbsent(literal.content(), key -> new LinkedList<>()).add(literal));
      check(ctx, occurrences, threshold);
    });
  }

  private static void check(CheckContext ctx, Map<String, List<StringLiteralTree>> occurrencesMap, int threshold) {
    for (Map.Entry<String, List<StringLiteralTree>> entry : occurrencesMap.entrySet()) {
      List<StringLiteralTree> occurrences = entry.getValue();
      int size = occurrences.size();
      if (size >= threshold) {
        StringLiteralTree first = occurrences.get(0);
        String message = String.format("Define a constant instead of duplicating this literal \"%s\" %s times.", first.content(), size);
        List<SecondaryLocation> secondaryLocations = occurrences.stream()
          .skip(1)
          .map(stringLiteral -> new SecondaryLocation(stringLiteral.metaData().textRange(), "Duplication"))
          .collect(Collectors.toList());
        double gap = size - 1.0;
        ctx.reportIssue(first, message, secondaryLocations, gap);
      }
    }
  }

}
