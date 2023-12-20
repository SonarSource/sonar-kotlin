/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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

import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.Locators;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.Language;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @TempDir
  public static File temp;

  private static StandaloneSonarLintEngine sonarlintEngine;

  private static File baseDir;

  @BeforeAll
  public static void prepare() throws Exception {
    // Orchestrator is used only to retrieve plugin artifacts from filesystem or maven
    OrchestratorExtensionBuilder orchestratorBuilder = OrchestratorExtension.builderEnv();
    TestsHelper.addLanguagePlugins(orchestratorBuilder);
    OrchestratorExtension orchestrator = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(TestsHelper.SQ_VERSION_PROPERTY, TestsHelper.DEFAULT_SQ_VERSION))
      .build();

    Locators locators = orchestrator.getConfiguration().locators();
    StandaloneGlobalConfiguration.Builder sonarLintConfigBuilder = StandaloneGlobalConfiguration.builder();
    orchestrator.getDistribution().getPluginLocations().stream()
      .filter(location -> !location.toString().contains("sonar-reset-data-plugin"))
      .map(plugin -> locators.locate(plugin).toPath()).forEach(sonarLintConfigBuilder::addPlugin);

    sonarLintConfigBuilder
      .addEnabledLanguage(Language.KOTLIN)
      .setSonarLintUserHome(temp.toPath())
      .setLogOutput((formattedMessage, level) -> {
        /* Don't pollute logs */
      });
    StandaloneGlobalConfiguration configuration = sonarLintConfigBuilder.build();
    sonarlintEngine = new StandaloneSonarLintEngineImpl(configuration);
    baseDir = temp;
  }

  @AfterAll
  public static void stop() {
    sonarlintEngine.stop();
  }

  @Test
  void test_kotlin() throws Exception {
    ClientInputFile inputFile = prepareInputFile("foo.kt",
      "fun foo_bar() {\n" +
        "    if (true) { \n" +
        "        val password = \"blabla\"\n" +
        "    } \n" +
        "}",
      false, "kotlin");

    List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration standaloneAnalysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();
    sonarlintEngine.analyze(standaloneAnalysisConfiguration, issues::add, null, null);

    assertThat(issues).extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().getPath(), Issue::getSeverity).containsOnly(
      tuple("kotlin:S100", 1, inputFile.getPath(), IssueSeverity.MINOR),
      tuple("kotlin:S1145", 2, inputFile.getPath(), IssueSeverity.MAJOR));
  }

  private ClientInputFile prepareInputFile(String relativePath, String content, final boolean isTest, String language) throws IOException {
    File file = new File(baseDir, relativePath);
    Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    return createInputFile(file.toPath(), isTest, language);
  }

  private ClientInputFile createInputFile(final Path path, final boolean isTest, String language) {
    return new ClientInputFile() {

      @Override
      public URI uri() {
        return path.toUri();
      }

      @Override
      public String getPath() {
        return path.toString();
      }

      @Override
      public boolean isTest() {
        return isTest;
      }

      @Override
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }


      @Override
      public <G> G getClientObject() {
        return null;
      }

      @Override
      public String contents() throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
      }

      @Override
      public String relativePath() {
        return path.toString();
      }

      @Override
      public InputStream inputStream() throws IOException {
        return Files.newInputStream(path);
      }

    };
  }

}
