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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.visitors.TreeVisitor;

public class ChecksVisitor extends TreeVisitor<InputFileContext> {

  private final DurationStatistics statistics;

  public ChecksVisitor(Checks<SlangCheck> checks, DurationStatistics statistics) {
    this.statistics = statistics;
    Collection<SlangCheck> rulesActiveInSonarQube = checks.all();
    for (SlangCheck check : rulesActiveInSonarQube) {
      RuleKey ruleKey = checks.ruleKey(check);
      Objects.requireNonNull(ruleKey);
      check.initialize(new ContextAdapter(ruleKey));
    }
  }

  public class ContextAdapter implements InitContext, CheckContext {

    public final RuleKey ruleKey;
    private InputFileContext currentCtx;

    public ContextAdapter(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> visitor) {
      ChecksVisitor.this.register(cls, statistics.time(ruleKey.rule(), (ctx, tree) -> {
        currentCtx = ctx;
        visitor.accept(this, tree);
      }));
    }

    @Override
    public Deque<Tree> ancestors() {
      return currentCtx.ancestors();
    }

    @Override
    public String filename() {
      return currentCtx.inputFile.filename();
    }

    @Override
    public String fileContent() {
      try {
        return currentCtx.inputFile.contents();
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read content of " + currentCtx.inputFile, e);
      }
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      reportIssue(textRange, message, Collections.emptyList(), null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message) {
      reportIssue(toHighlight, message, Collections.emptyList());
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation) {
      reportIssue(toHighlight, message, Collections.singletonList(secondaryLocation));
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
      reportIssue(toHighlight, message, secondaryLocations, null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      reportIssue(toHighlight.textRange(), message, secondaryLocations, gap);
    }

    @Override
    public void reportFileIssue(String message) {
      reportFileIssue(message, null);
    }

    @Override
    public void reportFileIssue(String message, @Nullable Double gap) {
      reportIssue((TextRange) null, message, Collections.emptyList(), gap);
    }

    private void reportIssue(@Nullable TextRange textRange, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      currentCtx.reportIssue(ruleKey, textRange, message, secondaryLocations, gap);
    }

  }

}
