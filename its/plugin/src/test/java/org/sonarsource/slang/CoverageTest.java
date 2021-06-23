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

import com.sonar.orchestrator.build.SonarScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class CoverageTest extends TestBase {

  private static final Path BASE_DIRECTORY = Paths.get("projects","measures");
  private static final String ABSOLUTE_PATH_PLACEHOLDER = "{ABSOLUTE_PATH_PLACEHOLDER}";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();
  private Path workDir;

  public void setUpRuby(String coverageReportName) throws Exception {
    workDir = tmpDir.newFolder("ruby").toPath().toAbsolutePath();
    Path src = BASE_DIRECTORY.resolve("ruby/file.rb");
    Path srcCopy = workDir.resolve(src.getFileName()).toAbsolutePath();
    Files.copy(src, srcCopy);
    Files.copy(BASE_DIRECTORY.resolve("ruby/file_not_in_report.rb"), workDir.resolve("file_not_in_report.rb"));
    Path report = BASE_DIRECTORY.resolve("ruby/" + coverageReportName);
    String reportContent = new String(Files.readAllBytes(report), UTF_8);
    reportContent = reportContent.replace(ABSOLUTE_PATH_PLACEHOLDER, srcCopy.toString().replace("\\", "\\\\"));
    Path reportCopy = workDir.resolve("coverage/." + coverageReportName);
    Files.createDirectories(reportCopy.getParent());
    Files.write(reportCopy, reportContent.getBytes(UTF_8));
  }

  @Test
  public void ruby_coverage_resultset() throws Exception {
    final String projectKey = "rubyCoverageResultSet";
    setUpRuby("resultset.json");
    SonarScanner rubyScanner = getSonarScanner(projectKey, workDir.getParent().toString(), "ruby");
    ORCHESTRATOR.executeBuild(rubyScanner);

    assert_ruby_measures(projectKey);
  }

  @Test
  public void ruby_coverage_json_formatter() throws Exception {
    final String projectKey = "rubyCoverageJsonFormatter";
    setUpRuby("coverage.json");
    SonarScanner rubyScanner = getSonarScanner(projectKey, workDir.getParent().toString(), "ruby");
    rubyScanner.setProperty("sonar.ruby.coverage.reportPaths", "coverage/.coverage.json");
    ORCHESTRATOR.executeBuild(rubyScanner);

    assert_ruby_measures(projectKey);
  }

  private void assert_ruby_measures(String projectKey) {
    String componentKey = projectKey + ":file.rb";
    assertThat(getMeasureAsInt(componentKey, "lines_to_cover")).isEqualTo(7);
    assertThat(getMeasureAsInt(componentKey, "uncovered_lines")).isEqualTo(1);
    assertThat(getMeasureAsInt(componentKey, "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt(componentKey, "uncovered_conditions")).isNull();

    componentKey = projectKey + ":file_not_in_report.rb";
    assertThat(getMeasureAsInt(componentKey, "lines_to_cover")).isEqualTo(3);
    assertThat(getMeasureAsInt(componentKey, "uncovered_lines")).isEqualTo(3);
    assertThat(getMeasureAsInt(componentKey, "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt(componentKey, "uncovered_conditions")).isNull();
  }

  public void setUpScala() throws Exception {
    workDir = tmpDir.newFolder("scala").toPath().toAbsolutePath();
    Path src = BASE_DIRECTORY.resolve("scala/file2.scala");
    Path srcCopy = workDir.resolve(src.getFileName()).toAbsolutePath();
    Files.copy(src, srcCopy);
    Path report = BASE_DIRECTORY.resolve("scala/scoverage.xml");
    String reportContent = new String(Files.readAllBytes(report), UTF_8);
    reportContent = reportContent.replace(ABSOLUTE_PATH_PLACEHOLDER, srcCopy.toString().replace("\\", "\\\\"));
    Path reportCopy = workDir.resolve("coverage/.scoverage.xml");
    Files.createDirectories(reportCopy.getParent());
    Files.write(reportCopy, reportContent.getBytes(UTF_8));
  }

  @Test
  public void scala_coverage() throws Exception {
    final String projectKey = "scalaCoverage";
    setUpScala();
    SonarScanner scalaScanner = getSonarScanner(projectKey, workDir.getParent().toString(), "scala");
    scalaScanner.setProperty("sonar.scala.coverage.reportPaths", "coverage/.scoverage.xml");

    ORCHESTRATOR.executeBuild(scalaScanner);

    String componentKey = projectKey + ":file2.scala";
    assertThat(getMeasureAsInt(componentKey, "lines_to_cover")).isEqualTo(3);
    assertThat(getMeasureAsInt(componentKey, "uncovered_lines")).isEqualTo(1);
    assertThat(getMeasureAsInt(componentKey, "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt(componentKey, "uncovered_conditions")).isNull();
  }

  @Test
  public void go_coverage() {
    final String projectKey = "goCoverage";
    SonarScanner goScanner = getSonarScanner(projectKey, BASE_DIRECTORY.toString(), "go");
    goScanner.setProperty("sonar.go.coverage.reportPaths", "coverage.out");

    ORCHESTRATOR.executeBuild(goScanner);

    String componentKey = projectKey + ":pivot.go";
    assertThat(getMeasureAsInt(componentKey, "lines_to_cover")).isEqualTo(16);
    assertThat(getMeasureAsInt(componentKey, "uncovered_lines")).isEqualTo(4);
    assertThat(getMeasureAsInt(componentKey, "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt(componentKey, "uncovered_conditions")).isNull();
  }

}
