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
package org.sonarsource.slang;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Measures;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class SurefireTest extends TestBase {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Tests.ORCHESTRATOR;
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
    ORCHESTRATOR.executeBuild(build);

    Map<String, Measures.Measure> measures = getMeasures("org.sonarsource.it.projects:tests-with-submodule",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_errors").getValue())).isZero();
    assertThat(parseInt(measures.get("test_failures").getValue())).isZero();
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isPositive();
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);
  }

}
