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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationsTest extends TestBase {
  private static final String BASE_DIRECTORY = "projects/duplications/";

  @Test
  public void kotlin_duplications() {
    final String projectKey = "kotlinDuplications";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, "kotlin"));

    assertThat(getMeasureAsInt(projectKey, "duplicated_lines")).isEqualTo(77);
    assertThat(getMeasureAsInt(projectKey, "duplicated_blocks")).isEqualTo(5);
    assertThat(getMeasureAsInt(projectKey, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasure(projectKey, "duplicated_lines_density").getValue()).isEqualTo("53.5");
  }

  @Test
  public void ruby_duplications() {
    final String projectKey = "rubyDuplications";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, "ruby"));

    assertThat(getMeasureAsInt(projectKey, "duplicated_lines")).isEqualTo(95);
    assertThat(getMeasureAsInt(projectKey, "duplicated_blocks")).isEqualTo(5);
    assertThat(getMeasureAsInt(projectKey, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasure(projectKey, "duplicated_lines_density").getValue()).isEqualTo("57.9");
  }

  @Test
  public void scala_duplications() {
    final String projectKey = "scalaDuplications";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, "scala"));

    assertThat(getMeasureAsInt(projectKey, "duplicated_lines")).isEqualTo(79);
    assertThat(getMeasureAsInt(projectKey, "duplicated_blocks")).isEqualTo(5);
    assertThat(getMeasureAsInt(projectKey, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasure(projectKey, "duplicated_lines_density").getValue()).isEqualTo("64.2");
  }

  @Test
  public void go_duplications() {
    final String projectKey = "goDuplications";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, "go"));

    assertThat(getMeasureAsInt(projectKey, "duplicated_lines")).isEqualTo(135);
    assertThat(getMeasureAsInt(projectKey, "duplicated_blocks")).isEqualTo(5);
    assertThat(getMeasureAsInt(projectKey, "duplicated_files")).isEqualTo(3);
    assertThat(getMeasure(projectKey, "duplicated_lines_density").getValue()).isEqualTo("97.8");

    final String componentKey = projectKey + ":pivot.go";
    assertThat(getMeasureAsInt(componentKey, "duplicated_lines")).isEqualTo(47);
    assertThat(getMeasureAsInt(componentKey, "duplicated_blocks")).isEqualTo(2);
    assertThat(getMeasureAsInt(componentKey, "duplicated_files")).isEqualTo(1);
    assertThat(getMeasure(componentKey, "duplicated_lines_density").getValue()).isEqualTo("97.9");
  }

}
