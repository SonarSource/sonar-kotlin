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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SuppressWarningsTest extends SitTestBase {

  private static final String BASE_DIRECTORY = "projects/suppress-warnings/kotlin";
  private static final String RULE_KEY = "S1145";
  private static final String COGNITIVE_COMPLEXITY_RULE_KEY = "S3776";
  private static final String TOO_MANY_CASES_RULE_KEY = "S1479";
  private static final String PROJECT_KEY = "issueSuppression";

  @Test
  public void test_kotlin_issue_suppression() {
    var result = analyze(PROJECT_KEY, BASE_DIRECTORY, profileRules("suppress-warnings-kotlin.xml"));

    assertThat(result.analyzedFileCount()).isEqualTo(2);
    assertThat(result.issuesForRule(LANGUAGE_KEY + ":" + RULE_KEY)).hasSize(7);
    assertThat(result.issuesForRule(LANGUAGE_KEY + ":" + COGNITIVE_COMPLEXITY_RULE_KEY)).hasSize(1);
    assertThat(result.issuesForRule(LANGUAGE_KEY + ":" + TOO_MANY_CASES_RULE_KEY)).isEmpty();
  }
}
