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

import com.sonar.orchestrator.build.SonarScanner;
import org.junit.jupiter.api.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.SearchRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
public class ExternalReportTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/externalreport/";

  @Test
  public void detekt() {
    final String projectKey = "detekt";
    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "detekt");
    sonarScanner.setProperty("sonar.kotlin.detekt.reportPaths", "detekt-checkstyle.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(2);

    Issue first = issues.stream().filter(issue -> issue.getRule().equals("external_detekt:ForEachOnRange")).findFirst().orElse(null);
    assertThat(first.getComponent()).isEqualTo(projectKey + ":main.kt");
    assertThat(first.getLine()).isEqualTo(2);
    assertThat(first.getMessage()).isEqualTo("Using the forEach method on ranges has a heavy performance cost. Prefer using simple for loops.");
    assertThat(first.getSeverity().name()).isEqualTo("CRITICAL");
    assertThat(first.getDebt()).isEqualTo("5min");

    Issue second = issues.stream().filter(issue -> issue.getRule().equals("external_detekt:CustomIssue")).findFirst().orElse(null);
    assertThat(second.getComponent()).isEqualTo(projectKey + ":main.kt");
    assertThat(second.getLine()).isEqualTo(2);
    assertThat(second.getMessage()).isEqualTo("My custom issue.");
    assertThat(second.getSeverity().name()).isEqualTo("MAJOR");
    assertThat(second.getDebt()).isEqualTo("5min");
  }

  @Test
  public void android_lint() {
    final String projectKey = "androidLint";
    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "androidlint");
    sonarScanner.setProperty("sonar.androidLint.reportPaths", "lint-results.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(3);

    Issue first = issues.stream().filter(issue -> issue.getRule().equals("external_android-lint:GradleDependency")).findFirst().orElse(null);
    assertThat(first.getLine()).isEqualTo(3);
    assertThat(first.getMessage()).isEqualTo("A newer version of com.android.support:recyclerview-v7 than 26.0.0 is available: 27.1.1");
    assertThat(first.getSeverity().name()).isEqualTo("MINOR");
    assertThat(first.getDebt()).isEqualTo("5min");

    Issue secondIssue = issues.stream().filter(issue -> issue.getRule().equals("external_android-lint:UnusedAttribute")).findFirst().orElse(null);
    assertThat(secondIssue.getLine()).isEqualTo(2);
    assertThat(secondIssue.getMessage()).isEqualTo("Attribute `required` is only used in API level 5 and higher (current min is 1)");
    assertThat(secondIssue.getSeverity().name()).isEqualTo("MINOR");
    assertThat(secondIssue.getDebt()).isEqualTo("5min");

    Issue third = issues.stream().filter(issue -> issue.getRule().equals("external_android-lint:UnknownIssue")).findFirst().orElse(null);
    assertThat(third.getLine()).isEqualTo(2);
    assertThat(third.getMessage()).isEqualTo("My custom issue");
    assertThat(third.getSeverity().name()).isEqualTo("MAJOR");
    assertThat(third.getDebt()).isEqualTo("5min");
  }

  @Test
  public void ktlint() {
    final String projectKey = "ktlint";
    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "ktlint");
    sonarScanner.setProperty("sonar.kotlin.ktlint.reportPaths", "ktlint-checkstyle.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(2);

    Issue first = issues.stream().filter(issue -> issue.getRule().equals("external_ktlint:standard:no-wildcard-imports")).findFirst().orElse(null);
    assertThat(first.getComponent()).isEqualTo(projectKey + ":main.kt");
    assertThat(first.getLine()).isEqualTo(1);
    assertThat(first.getMessage()).isEqualTo("Wildcard import (cannot be auto-corrected)");
    assertThat(first.getSeverity().name()).isEqualTo("MAJOR");
    assertThat(first.getDebt()).isEqualTo("0min");

    Issue second = issues.stream().filter(issue -> issue.getRule().equals("external_ktlint:CustomIssue")).findFirst().orElse(null);
    assertThat(second.getComponent()).isEqualTo(projectKey + ":main.kt");
    assertThat(second.getLine()).isEqualTo(2);
    assertThat(second.getMessage()).isEqualTo("My custom issue.");
    assertThat(second.getSeverity().name()).isEqualTo("MAJOR");
    assertThat(second.getDebt()).isEqualTo("5min");
  }

  private List<Issue> getExternalIssues(String projectKey) {
    return newWsClient()
      .issues()
      .search(new SearchRequest().setProjects(Collections.singletonList(projectKey)))
      .getIssuesList()
      .stream()
      .filter(issue -> issue.getRule().startsWith("external_"))
      .collect(Collectors.toList());
  }

}
