/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import com.sonar.orchestrator.junit5.OrchestratorExtension;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonarsource.sonarlint.core.analysis.AnalysisEngine;
import org.sonarsource.sonarlint.core.analysis.api.ActiveRule;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.analysis.api.ClientModuleFileSystem;
import org.sonarsource.sonarlint.core.analysis.api.ClientModuleInfo;
import org.sonarsource.sonarlint.core.analysis.api.Issue;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisEngineConfiguration;
import org.sonarsource.sonarlint.core.analysis.command.AnalyzeCommand;
import org.sonarsource.sonarlint.core.analysis.command.RegisterModuleCommand;
import org.sonarsource.sonarlint.core.commons.ImpactSeverity;
import org.sonarsource.sonarlint.core.commons.SoftwareQuality;
import org.sonarsource.sonarlint.core.commons.api.SonarLanguage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.sonarsource.sonarlint.core.commons.log.LogOutput;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;
import org.sonarsource.sonarlint.core.commons.progress.ProgressMonitor;
import org.sonarsource.sonarlint.core.plugin.commons.PluginsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @TempDir
  public static Path temp;

  private static AnalysisEngine analysisEngine;
  private final ProgressMonitor progressMonitor = new ProgressMonitor(null);

  @BeforeAll
  public static void prepare() {
    SonarLintLogger.setTarget(new NoOpLogOutput());
    var pluginJarLocations = getPluginJarLocations();
    var pluginConfiguration = new PluginsLoader.Configuration(pluginJarLocations, Set.of(SonarLanguage.KOTLIN), false, Optional.empty());
    // Closing pluginLoader here in prepare would make analysisEngine see 0 plugins during analysis
    var pluginLoader = new PluginsLoader().load(pluginConfiguration, Set.of());
    var analysisEngineConfiguration = AnalysisEngineConfiguration.builder()
      .setWorkDir(temp)
      .build();
    var loadedPlugins = pluginLoader.getLoadedPlugins();
    analysisEngine = new AnalysisEngine(analysisEngineConfiguration, loadedPlugins, new NoOpLogOutput());
  }

  private static @NotNull Set<Path> getPluginJarLocations() {
    var orchestratorBuilder = OrchestratorExtension.builderEnv();
    TestsHelper.addLanguagePlugins(orchestratorBuilder);
    var orchestrator = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(TestsHelper.SQ_VERSION_PROPERTY, TestsHelper.DEFAULT_SQ_VERSION))
      .build();
    var locators = orchestrator.getConfiguration().locators();
    return orchestrator.getDistribution().getPluginLocations().stream()
      .filter(location -> !location.toString().contains("sonar-reset-data-plugin"))
      .map(plugin -> locators.locate(plugin).toPath())
      .collect(Collectors.toSet());
  }

  @AfterAll
  public static void stop() {
    SonarLintLogger.setTarget(null);
    analysisEngine.stop();
  }

  @Test
  void test_kotlin() throws Exception {
    var inputFile = prepareInputFile("foo.kt",
      "fun foo_bar() {\n" +
        "    if (true) { \n" +
        "        val password = \"blabla\"\n" +
        "    } \n" +
        "}\n" +
        "\n" +
        "fun foo_bar_nosonar() {} // NOSONAR \n");

    var clientFileSystem = new ClientModuleFileSystem() {
      @Override
      public Stream<ClientInputFile> files(@NotNull String s, InputFile.@NotNull Type type) {
        return Stream.of(inputFile);
      }

      @Override
      public Stream<ClientInputFile> files() {
        return Stream.of(inputFile);
      }
    };
    var registerModuleCommand = new RegisterModuleCommand(new ClientModuleInfo("testModule", clientFileSystem));
    analysisEngine.post(registerModuleCommand, progressMonitor).get();

    var kotlinLanguageKey = SonarLanguage.KOTLIN.name();
    var analysisConfiguration = AnalysisConfiguration.builder()
      .setBaseDir(temp)
      .addInputFile(inputFile)
      .addActiveRules(
        new ActiveRule("kotlin:S100", kotlinLanguageKey),
        new ActiveRule("kotlin:S1481", kotlinLanguageKey),
        new ActiveRule("kotlin:S1145", kotlinLanguageKey))
      .build();
    var issues = new ArrayList<Issue>();
    var analyzeCommand = new AnalyzeCommand("testModule", analysisConfiguration, issues::add, new NoOpLogOutput());
    analysisEngine.post(analyzeCommand, progressMonitor).get();

    assertThat(issues).extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().uri()).containsOnly(
      tuple("kotlin:S100", 1, inputFile.uri()),
      tuple("kotlin:S1481", 3, inputFile.uri()),
      tuple("kotlin:S1145", 2, inputFile.uri()));
  }

  private ClientInputFile prepareInputFile(String relativePath, String content) throws IOException {
    var file = new File(temp.toFile(), relativePath);
    Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    return new PathBasedClientInputFile(file.toPath());
  }

  private static class PathBasedClientInputFile implements ClientInputFile {
    private final Path path;

    public PathBasedClientInputFile(Path path) {
      this.path = path;
    }

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
      return false;
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
      return Files.readString(path);
    }

    @Override
    public String relativePath() {
      return path.toString();
    }

    @Override
    public InputStream inputStream() throws IOException {
      return Files.newInputStream(path);
    }
  }

  private static class NoOpLogOutput implements LogOutput {
    @Override
    public void log(String formattedMessage, @NotNull Level level, @Nullable String stacktrace) {
      /* Don't pollute logs */
    }
  }

}
