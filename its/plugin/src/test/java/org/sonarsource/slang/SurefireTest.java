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

import com.sonar.orchestrator.build.MavenBuild;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.sonar.orchestrator.junit5.OrchestratorExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonarqube.ws.Measures;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class SurefireTest extends TestBase {

  @RegisterExtension
  public static final OrchestratorExtension ORCHESTRATOR = TestsHelper.ORCHESTRATOR;
  private static final Path BASE_DIRECTORY = Paths.get("projects");


  @Test
  public void tests_without_main_code() {
    MavenBuild build = MavenBuild.create()
      .setPom(new File(BASE_DIRECTORY.toFile(), "tests-without-main-code/pom.xml"))
      .setGoals("clean test-compile surefire:test", "sonar:sonar");
    ORCHESTRATOR.executeBuild(build);

    Map<String, Measures.Measure> measures = getMeasures("org.sonarsource.it.projects:tests-without-main-code",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(1);
    assertThat(parseInt(measures.get("test_errors").getValue())).isZero();
    assertThat(parseInt(measures.get("test_failures").getValue())).isZero();
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(1);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isPositive();
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);

  }

  @Test
  public void tests_with_report_name_suffix() {
    MavenBuild build = MavenBuild.create()
      .setPom(new File(BASE_DIRECTORY.toFile(), "tests-surefire-suffix/pom.xml"))
      .setGoals("clean test-compile surefire:test -Dsurefire.reportNameSuffix=Run1", "test-compile surefire:test -Dsurefire.reportNameSuffix=Run2", "sonar:sonar");
    ORCHESTRATOR.executeBuild(build);

    Map<String, Measures.Measure> measures = getMeasures("org.sonarsource.it.projects:tests-surefire-suffix",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_errors").getValue())).isZero();
    assertThat(parseInt(measures.get("test_failures").getValue())).isZero();
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isPositive();
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);
  }

  @Test
  public void tests_with_submodule() {
    MavenBuild build = MavenBuild.create()
      .setPom(new File(BASE_DIRECTORY.toFile(), "tests-with-submodule/pom.xml"))
      .setGoals("clean test-compile surefire:test -Dsurefire.reportNameSuffix=Run1", "test-compile surefire:test -Dsurefire.reportNameSuffix=Run2", "sonar:sonar");
    String buildLog = ORCHESTRATOR.executeBuild(build).getLogs();

    Map<String, Measures.Measure> measures = getMeasures("org.sonarsource.it.projects:tests-with-submodule",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_errors").getValue())).isZero();
    assertThat(parseInt(measures.get("test_failures").getValue())).isZero();
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isPositive();
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);

    assertThat(buildLog)
            .as("KotlinProjectSensor must be executed after all executions of KotlinSensor for individual modules")
            .containsSubsequence(
                    "Run sensors on module submodule",
                    "Kotlin Sensor [kotlin] (done)",
                    "Run sensors on module tests-with-submodule",
                    "Kotlin Sensor [kotlin] (done)",
                    "Run sensors on project",
                    "KotlinProjectSensor [kotlin] (done)"
            );
  }

}
