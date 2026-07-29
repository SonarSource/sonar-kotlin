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
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalReportTest extends SitTestBase {

  private static final String BASE_DIRECTORY = "projects/externalreport/";

  @Test
  public void detekt() {
    var result = analyze("detekt", BASE_DIRECTORY + "detekt", Map.of("sonar.kotlin.detekt.reportPaths", "detekt-checkstyle.xml"));
    List<TextRangeIssue> issues = result.externalIssues();
    assertThat(issues).hasSize(2);

    TextRangeIssue first = findByRule(issues, "external_detekt:ForEachOnRange");
    assertThat(first.componentPath()).isEqualTo("main.kt");
    assertThat(first.line()).isEqualTo(2);
    assertThat(first.message()).isEqualTo("Using the forEach method on ranges has a heavy performance cost. Prefer using simple for loops.");

    TextRangeIssue second = findByRule(issues, "external_detekt:CustomIssue");
    assertThat(second.componentPath()).isEqualTo("main.kt");
    assertThat(second.line()).isEqualTo(2);
    assertThat(second.message()).isEqualTo("My custom issue.");
  }

  @Test
  public void android_lint() {
    var result = analyze("androidLint", BASE_DIRECTORY + "androidlint", Map.of("sonar.androidLint.reportPaths", "lint-results.xml"));
    List<TextRangeIssue> issues = result.externalIssues();
    assertThat(issues).hasSize(3);

    TextRangeIssue first = findByRule(issues, "external_android-lint:GradleDependency");
    assertThat(first.line()).isEqualTo(3);
    assertThat(first.message()).isEqualTo("A newer version of com.android.support:recyclerview-v7 than 26.0.0 is available: 27.1.1");

    TextRangeIssue second = findByRule(issues, "external_android-lint:UnusedAttribute");
    assertThat(second.line()).isEqualTo(2);
    assertThat(second.message()).isEqualTo("Attribute `required` is only used in API level 5 and higher (current min is 1)");

    TextRangeIssue third = findByRule(issues, "external_android-lint:UnknownIssue");
    assertThat(third.line()).isEqualTo(2);
    assertThat(third.message()).isEqualTo("My custom issue");
  }

  @Test
  public void ktlint() {
    var result = analyze("ktlint", BASE_DIRECTORY + "ktlint", Map.of("sonar.kotlin.ktlint.reportPaths", "ktlint-checkstyle.xml"));
    List<TextRangeIssue> issues = result.externalIssues();
    assertThat(issues).hasSize(2);

    TextRangeIssue first = findByRule(issues, "external_ktlint:standard:no-wildcard-imports");
    assertThat(first.componentPath()).isEqualTo("main.kt");
    assertThat(first.line()).isEqualTo(1);
    assertThat(first.message()).isEqualTo("Wildcard import (cannot be auto-corrected)");

    TextRangeIssue second = findByRule(issues, "external_ktlint:CustomIssue");
    assertThat(second.componentPath()).isEqualTo("main.kt");
    assertThat(second.line()).isEqualTo(2);
    assertThat(second.message()).isEqualTo("My custom issue.");
  }

  private static TextRangeIssue findByRule(List<TextRangeIssue> issues, String ruleKey) {
    return issues.stream().filter(issue -> issue.ruleKey().equals(ruleKey)).findFirst().orElseThrow();
  }

}
