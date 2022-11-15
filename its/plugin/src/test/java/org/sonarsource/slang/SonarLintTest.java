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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.Locators;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static StandaloneSonarLintEngine sonarlintEngine;

  private static File baseDir;

  @BeforeClass
  public static void prepare() throws Exception {
    // Orchestrator is used only to retrieve plugin artifacts from filesystem or maven
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv();
    Tests.addLanguagePlugins(orchestratorBuilder);
    Orchestrator orchestrator = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(Tests.SQ_VERSION_PROPERTY, Tests.DEFAULT_SQ_VERSION))
      .build();

    Locators locators = orchestrator.getConfiguration().locators();
    StandaloneGlobalConfiguration.Builder sonarLintConfigBuilder = StandaloneGlobalConfiguration.builder();
    orchestrator.getDistribution().getPluginLocations().stream()
      .filter(location -> !location.toString().contains("sonar-reset-data-plugin"))
      .map(plugin -> {
        try {
          return locators.locate(plugin).toURI().toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }).forEach(sonarLintConfigBuilder::addPlugin);

    sonarLintConfigBuilder
      .addEnabledLanguage(Language.KOTLIN)
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> {
        /* Don't pollute logs */
      });
    StandaloneGlobalConfiguration configuration = sonarLintConfigBuilder.build();
    sonarlintEngine = new StandaloneSonarLintEngineImpl(configuration);
    baseDir = temp.newFolder();
  }

  @AfterClass
  public static void stop() {
    sonarlintEngine.stop();
  }

  @Test
  public void test_kotlin() throws Exception {
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
      tuple("kotlin:S100", 1, inputFile.getPath(), "MINOR"),
      tuple("kotlin:S1145", 2, inputFile.getPath(), "MAJOR"));
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
