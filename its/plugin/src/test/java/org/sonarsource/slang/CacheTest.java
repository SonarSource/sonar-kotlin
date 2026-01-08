/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheTest extends TestBase {
  private static final String PROJECT_KEY = "cache";

  @Test
  public void cache() {
    final SonarScanner build = getSonarScanner(PROJECT_KEY, "projects/duplications", "kotlin");
    build.setProperty("sonar.branch.name", "main");
    BuildResult buildResult = ORCHESTRATOR.executeBuild(build);
    assertThat(buildResult.getLogsLines(line -> line.contains("2/2 source files have been analyzed")))
        .hasSize(1);
    assertAnalysisResult();

    buildResult = ORCHESTRATOR.executeBuild(build);
    assertThat(buildResult.getLogsLines(line -> line.contains("2/2 source files have been analyzed")))
        .hasSize(1);
    assertAnalysisResult();

    build.getProperties().remove("sonar.branch.name");
    build.setProperty("sonar.pullrequest.key", "myPR");
    build.setProperty("sonar.pullrequest.branch", "myBranch");
    build.setProperty("sonar.pullrequest.base", "main");
    buildResult = ORCHESTRATOR.executeBuild(build);
    assertThat(buildResult.getLogsLines(line -> line.contains("Only analyzing 0 changed Kotlin files out of 2.")))
        .hasSize(1);
    assertAnalysisResult();
  }

  private void assertAnalysisResult() {
    assertThat(getMeasureAsInt(PROJECT_KEY, "duplicated_lines")).isEqualTo(77);
    assertThat(getMeasureAsInt(PROJECT_KEY, "duplicated_blocks")).isEqualTo(5);
    assertThat(getMeasureAsInt(PROJECT_KEY, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasure(PROJECT_KEY, "duplicated_lines_density").getValue()).isEqualTo("53.5");
  }
}
