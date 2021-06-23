package org.sonarsource.slang.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.ComparisonFailure;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.persistence.JsonTree;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class VerifierTest {

  private static final Path BASE_DIR = Paths.get("src", "test", "resources");

  private static final SlangCheck NO_ISSUE_CHECK = init -> {
  };

  private static final SlangCheck ISSUE_ON_IDENTIFIERS_CHECK = init ->
    init.register(IdentifierTree.class, (ctx, identifier) -> ctx.reportIssue(identifier, "primary"));

  private static final SlangCheck ISSUE_ON_IDENTIFIER_TEXT_RANGE_CHECK = init ->
    init.register(IdentifierTree.class, (ctx, identifier) -> ctx.reportIssue(identifier.textRange(), "primary"));

  private static final SlangCheck ISSUE_WITH_SECONDARY_LOCATION_CHECK = init ->
    init.register(TopLevelTree.class, (ctx, top) ->
      ctx.reportIssue(
        top.declarations().get(0),
        "primary",
        new SecondaryLocation(top.declarations().get(1), "secondary")));

  private static final SlangCheck ISSUE_ON_FILE = init ->
    init.register(TopLevelTree.class, (ctx, identifier) -> ctx.reportFileIssue(ctx.filename() + " length " + ctx.fileContent().length()));

  @Test
  void verify_with_issue() throws IOException {
    Path path = BASE_DIR.resolve("primary.code");
    ASTConverter converter = createConverter(path);
    Verifier.verify(converter, path, ISSUE_ON_IDENTIFIERS_CHECK);
    Verifier.verify(converter, path, ISSUE_ON_IDENTIFIER_TEXT_RANGE_CHECK);

    ComparisonFailure e = assertThrows(ComparisonFailure.class,
      () -> Verifier.verifyNoIssue(converter, path, ISSUE_ON_IDENTIFIERS_CHECK));
    assertThat(e).hasMessageStartingWith("ERROR: 'assertNoIssues()' is called but there's some 'Noncompliant' comments. In file (primary.code:1)");
  }

  @Test
  void verify_with_wrong_message() throws IOException {
    Path path = BASE_DIR.resolve("wrong-message.code");
    ASTConverter converter = createConverter(path);
    ComparisonFailure e = assertThrows(ComparisonFailure.class,
      () -> Verifier.verify(converter, path, ISSUE_ON_IDENTIFIERS_CHECK));
    assertThat(e).hasMessageContaining("" +
      "- 001: Noncompliant {{wrong message}}\n" +
      "+ 001: Noncompliant {{primary}}");
  }

  @Test
  void verify_with_primary_and_secondary() throws IOException {
    Path path = BASE_DIR.resolve("primary-and-secondary.code");
    ASTConverter converter = createConverter(path);
    Verifier.verify(converter, path, ISSUE_WITH_SECONDARY_LOCATION_CHECK);

    ComparisonFailure e = assertThrows(ComparisonFailure.class,
      () -> Verifier.verifyNoIssue(converter, path, ISSUE_WITH_SECONDARY_LOCATION_CHECK));
    assertThat(e).hasMessageStartingWith("ERROR: 'assertNoIssues()' is called but there's some 'Noncompliant' comments. In file (primary-and-secondary.code:1)");
  }

  @Test
  void verify_issue_on_file() throws IOException {
    Path path = BASE_DIR.resolve("file-issue.code");
    ASTConverter converter = createConverter(path);
    Verifier.verify(converter, path, ISSUE_ON_FILE);

    ComparisonFailure e = assertThrows(ComparisonFailure.class,
      () -> Verifier.verifyNoIssue(converter, path, ISSUE_ON_FILE));
    assertThat(e).hasMessageStartingWith("ERROR: 'assertNoIssues()' is called but there's some 'Noncompliant' comments. In file (file-issue.code:0)");
  }

  @Test
  void verify_without_issue() throws IOException {
    Path path = BASE_DIR.resolve("no-issue.code");
    ASTConverter converter = createConverter(path);
    Verifier.verifyNoIssue(converter, path, NO_ISSUE_CHECK);

    ComparisonFailure e = assertThrows(ComparisonFailure.class,
      () -> Verifier.verify(converter, path, NO_ISSUE_CHECK));
    assertThat(e).hasMessageStartingWith("ERROR: 'assertOneOrMoreIssues()' is called but there's no 'Noncompliant' comments. In file (no-issue.code:1)");
  }

  @Test
  void invalid_path() throws IOException {
    Path path = BASE_DIR.resolve("invalid-path.code");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> Verifier.verify(mock(ASTConverter.class), path, ISSUE_ON_IDENTIFIERS_CHECK));
    assertThat(e).hasMessageContaining("Cannot read");
  }

  private static ASTConverter createConverter(Path path) throws IOException {
    Path jsonPath = path.getParent().resolve(path.getFileName() + ".json");
    String code = new String(Files.readAllBytes(path), UTF_8);
    Tree tree = JsonTree.fromJson(new String(Files.readAllBytes(jsonPath), UTF_8));
    return content -> code.equals(content) ? tree : null;
  }

}
