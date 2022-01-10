/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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
import org.sonarqube.ws.Issues;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasuresTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/measures/";

  @Test
  public void kotlin_measures() {
    final String projectKey = "kotlinMeasures";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, "kotlin"));

    assertThat(getMeasureAsInt(projectKey, "files")).isEqualTo(3);

    final String emptyFile = projectKey + ":empty_file.kt";
    final String file1 = projectKey + ":file1.kt";
    final String file2 = projectKey + ":file2.kt";

    assertThat(getMeasure(emptyFile, "ncloc")).isNull();
    assertThat(getMeasureAsInt(file1, "ncloc")).isEqualTo(7);
    assertThat(getMeasureAsInt(file2, "ncloc")).isEqualTo(8);

    assertThat(getMeasure(emptyFile, "comment_lines")).isNull();
    assertThat(getMeasureAsInt(file1, "comment_lines")).isEqualTo(8);
    assertThat(getMeasureAsInt(file2, "comment_lines")).isEqualTo(3);

    assertThat(getMeasure(emptyFile, "statements")).isNull();
    assertThat(getMeasureAsInt(file1, "statements")).isEqualTo(3);
    assertThat(getMeasureAsInt(file2, "statements")).isEqualTo(2);

    assertThat(getMeasureAsInt(file1, "cognitive_complexity")).isEqualTo(0);
    assertThat(getMeasureAsInt(file2, "cognitive_complexity")).isEqualTo(2);

    assertThat(getMeasure(emptyFile, "ncloc_data")).isNull();
    assertThat(getMeasure(file1, "ncloc_data").getValue()).isEqualTo("1=1;3=1;4=1;7=1;8=1;13=1;14=1");
    assertThat(getMeasure(file2, "ncloc_data").getValue()).isEqualTo("1=1;2=1;3=1;4=1;5=1;7=1;10=1;11=1");

    assertThat(getMeasure(file1, "executable_lines_data").getValue()).isEqualTo("4=1;8=1;13=1");

    List<Issues.Issue> issuesForRule = getIssuesForRule(projectKey, "kotlin:S100");
    assertThat(issuesForRule).extracting(Issues.Issue::getLine).containsExactly(2, 7);
    assertThat(issuesForRule).extracting(Issues.Issue::getComponent).containsExactly(file2, file2);
  }

}
