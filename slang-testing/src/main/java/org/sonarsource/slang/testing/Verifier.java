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
package org.sonarsource.slang.testing;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.visitors.TreeContext;
import org.sonarsource.slang.visitors.TreeVisitor;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Verifier {

  private Verifier() {
    // utility class
  }

  public static void verify(ASTConverter converter, Path path, SlangCheck check) {
    createVerifier(converter, path, check).assertOneOrMoreIssues();
  }

  public static void verifyNoIssue(ASTConverter converter, Path path, SlangCheck check) {
    createVerifier(converter, path, check).assertNoIssues();
  }

  private static SingleFileVerifier createVerifier(ASTConverter converter, Path path, SlangCheck check) {

    SingleFileVerifier verifier = SingleFileVerifier.create(path, UTF_8);

    String testFileContent = readFile(path);
    Tree root = converter.parse(testFileContent, null);

    ((TopLevelTree) root).allComments()
      .forEach(comment -> {
        TextPointer start = comment.textRange().start();
        verifier.addComment(start.line(), start.lineOffset()+1, comment.text(), 2, 0);
      });
    
    TestContext ctx = new TestContext(verifier, path.getFileName().toString(), testFileContent);
    check.initialize(ctx);
    ctx.scan(root);

    return verifier;
  }

  private static String readFile(Path path) {
    try {
      return new String(Files.readAllBytes(path), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + path, e);
    }
  }

  private static class TestContext extends TreeContext implements InitContext, CheckContext {

    private final TreeVisitor<TestContext> visitor;
    private final SingleFileVerifier verifier;
    private final String filename;
    private String testFileContent;

    public TestContext(SingleFileVerifier verifier, String filename, String testFileContent) {
      this.verifier = verifier;
      this.filename = filename;
      this.testFileContent = testFileContent;
      visitor = new TreeVisitor<>();
    }

    public void scan(@Nullable Tree root) {
      visitor.scan(this, root);
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> consumer) {
      visitor.register(cls, (ctx, node) -> consumer.accept(this, node));
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
    public String filename() {
      return filename;
    }

    @Override
    public String fileContent() {
      return testFileContent;
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      reportIssue(textRange, message, Collections.emptyList(), null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
      reportIssue(toHighlight, message, secondaryLocations, null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      reportIssue(toHighlight.textRange(), message, secondaryLocations, gap);
    }

    public void reportFileIssue(String message) {
      reportFileIssue(message, null);
    }

    @Override
    public void reportFileIssue(String message, @Nullable Double gap) {
      verifier.reportIssue(message).onFile().withGap(gap);
    }

    private void reportIssue(TextRange textRange, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      TextPointer start = textRange.start();
      TextPointer end = textRange.end();
      SingleFileVerifier.Issue issue = verifier
        .reportIssue(message)
        .onRange(start.line(), start.lineOffset() + 1, end.line(), end.lineOffset())
        .withGap(gap);
      secondaryLocations.forEach(secondary -> issue.addSecondary(
        secondary.textRange.start().line(),
        secondary.textRange.start().lineOffset() + 1,
        secondary.textRange.end().line(),
        secondary.textRange.end().lineOffset(),
        secondary.message));
    }

  }

}
