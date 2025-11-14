/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SuppressWarningsTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/suppress-warnings/";
  private static final String SUPPRESS_WARNINGS_PROFILE = "suppress-warnings-profile";
  private static final String RULE_KEY = "S1145";
  private static final String COGNITIVE_COMPLEXITY_RULE_KEY = "S3776";
  private static final String TOO_MANY_CASES_RULE_KEY = "S1479";
  private static final String PROJECT_KEY = "issueSuppression";
  private static final String LANGUAGE = "kotlin";


  @Test
  public void test_kotlin_issue_suppression() {
    ORCHESTRATOR.executeBuild(getSonarScanner(PROJECT_KEY, BASE_DIRECTORY, LANGUAGE, SUPPRESS_WARNINGS_PROFILE));

    assertThat(getMeasureAsInt(PROJECT_KEY, "files")).isEqualTo(2);
    assertThat(getIssuesForRule(PROJECT_KEY, LANGUAGE + ":" + RULE_KEY)).hasSize(7);
    assertThat(getIssuesForRule(PROJECT_KEY, LANGUAGE + ":" + COGNITIVE_COMPLEXITY_RULE_KEY)).hasSize(1);
    assertThat(getIssuesForRule(PROJECT_KEY, LANGUAGE + ":" + TOO_MANY_CASES_RULE_KEY)).isEmpty();
  }
}
