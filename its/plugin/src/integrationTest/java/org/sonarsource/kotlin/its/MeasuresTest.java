/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.its;

import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasuresTest extends SitTestBase {

  private static final String BASE_DIRECTORY = "projects/measures/kotlin";

  @Test
  public void kotlin_measures() {
    var result = analyze("kotlinMeasures", BASE_DIRECTORY, List.of(activeRule("S100")));

    assertThat(result.analyzedFileCount()).isEqualTo(3);

    assertThat(result.measureAsInt("empty_file.kt", "ncloc")).isNull();
    assertThat(result.measureAsInt("file1.kt", "ncloc")).isEqualTo(7);
    assertThat(result.measureAsInt("file2.kt", "ncloc")).isEqualTo(8);

    assertThat(result.measureAsInt("empty_file.kt", "comment_lines")).isNull();
    assertThat(result.measureAsInt("file1.kt", "comment_lines")).isEqualTo(8);
    assertThat(result.measureAsInt("file2.kt", "comment_lines")).isEqualTo(3);

    assertThat(result.measureAsInt("empty_file.kt", "statements")).isNull();
    assertThat(result.measureAsInt("file1.kt", "statements")).isEqualTo(3);
    assertThat(result.measureAsInt("file2.kt", "statements")).isEqualTo(2);

    assertThat(result.measureAsInt("file1.kt", "cognitive_complexity")).isEqualTo(0);
    assertThat(result.measureAsInt("file2.kt", "cognitive_complexity")).isEqualTo(2);

    List<TextRangeIssue> issuesForRule = result.issuesForRule("kotlin:S100").stream()
      .filter(TextRangeIssue.class::isInstance)
      .map(TextRangeIssue.class::cast)
      .toList();
    assertThat(issuesForRule).extracting(TextRangeIssue::line).containsExactly(2, 7);
    assertThat(issuesForRule).extracting(TextRangeIssue::componentPath).containsExactly("file2.kt", "file2.kt");
  }

}
