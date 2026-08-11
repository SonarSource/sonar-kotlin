/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.GradleBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.build.SonarScannerInstaller;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.Version;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Only {@code test_kotlin_language_server} remains here: it needs a real Gradle build to
 * supply the Java classpath, and its golden paths are project-dir-relative, so it cannot
 * move to the SIT-based {@code its/ruling} module. The other six ruling projects were ported
 * to {@code its/ruling}'s {@code KotlinRulingTest}.
 */
class SlangRulingTest {

  private static final Logger LOG = LoggerFactory.getLogger(SlangRulingTest.class);

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";
  public static final Configuration CONFIGURATION = Configuration.createEnv();
  private static OrchestratorExtension orchestrator;
  private static boolean keepSonarqubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));
  private static final boolean IGNORE_EXPECTED_ISSUES_AND_REPORT_ALL = "true".equals(System.getProperty("reportAll"));
  private static final boolean CLEAN_PROJECT_BINARIES = "true".equals(System.getProperty("cleanProjects"));

  @BeforeAll
  static void setUp() {
    OrchestratorExtensionBuilder builder = OrchestratorExtension.builderEnv()
      .setEdition(Edition.ENTERPRISE_LW)
      .activateLicense()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.11.0.2659"))
      .setServerProperty("sonar.telemetry.enable", "false");

    var plugin = "sonar-kotlin-plugin";
    var pluginLocation = FileLocation.byWildcardMavenFilename(new File("../../" + plugin + "/build/libs"), plugin + ".jar");
    builder.addPlugin(pluginLocation);

    orchestrator = builder.build();
    orchestrator.start();
    // installed scanner will be shared by all tests
    new SonarScannerInstaller(new Locators(CONFIGURATION)).install(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION), CONFIGURATION.fileSystem().workspace().toFile());

    ProfileGenerator.RulesConfiguration kotlinRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    kotlinRulesConfiguration.add("S1451", "headerFormat", "/\\*\n \\* Copyright \\d{4}-\\d{4} JetBrains s\\.r\\.o\\.");
    kotlinRulesConfiguration.add("S1451", "isRegularExpression", "true");

    File kotlinProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "kotlin", "kotlin", kotlinRulesConfiguration, Collections.emptySet());

    orchestrator.getServer().restoreProfile(FileLocation.of(kotlinProfile));
  }

  @Test
  void test_kotlin_language_server() throws IOException {
    executeGradleBuildAndAssertDifferences("kotlin/kotlin-language-server", Map.of());
  }

  private static Map<String, String> prepareAnalysisConfiguration(String project, Map<String, String> additionalProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(additionalProperties);
    String projectKey = projectKey(project);
    properties.put("sonar.projectKey", projectKey);
    properties.put("sonar.projectName", project);
    // Set per-project working directory to allow parallel test execution
    properties.put("sonar.working.directory", "build/sonar-workdir/" + projectKey);
    properties.put("sonar.projectVersion", "1");
    properties.put("sonar.sourceEncoding", "UTF-8");
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");
    properties.put("sonar.kotlin.performance.measure", "true");
    properties.put("sonar.cpd.exclusions", "**/*");
    properties.put("sonar.scm.disabled", "true");
    properties.put("sonar.internal.analysis.failFast", "true");

    Path moduleDirectory = moduleDirectory();
    Path performanceMeasuresDirectory = moduleDirectory.resolve(Path.of("build", "performance")).resolve(projectRelativePath(project));
    Files.createDirectories(performanceMeasuresDirectory);
    properties.put("sonar.kotlin.performance.measure.json", performanceMeasuresDirectory.resolve("sonar.kotlin.performance.measure.json").toString());

    Path expectedDirectory;
    if (IGNORE_EXPECTED_ISSUES_AND_REPORT_ALL) {
      expectedDirectory = moduleDirectory.resolve(Path.of("build", "tmp", "empty"));
      Files.createDirectories(expectedDirectory);
    } else {
      expectedDirectory = moduleDirectory.resolve(Path.of("src", "integrationTest", "resources", "expected")).resolve(projectRelativePath(project));
    }
    Path actualDirectory = moduleDirectory.resolve(Path.of("build", "tmp", "actual", project));
    Files.createDirectories(actualDirectory);

    properties.put("sonar.lits.dump.old", expectedDirectory.toString());
    properties.put("sonar.lits.dump.new", actualDirectory.toString());
    properties.put("sonar.lits.differences", litsDifferencesFilePath(project).toString());
    return properties;
  }

  private static String projectKey(String project) {
    return project.replace("/", "-") + "-project";
  }

  private static Path projectRelativePath(String project) {
    return Path.of(project.replace('/', File.separatorChar));
  }

  private static Path projectDirectory(String project) throws IOException {
    Path directory = Path.of("..", "sources").resolve(projectRelativePath(project));
    if (!Files.exists(directory)) {
      throw new IOException("Project directory not found: " + directory);
    }
    return directory.toRealPath();
  }

  private static Path moduleDirectory() throws IOException {
    Path currentDirectory = Path.of(".").toRealPath();
    if (!currentDirectory.getFileName().toString().equals("sq-integration")) {
      throw new IOException("Current directory is not its/sq-integration but: " + currentDirectory);
    }
    return currentDirectory;
  }

  private static Path litsDifferencesFilePath(String project) throws IOException {
    return moduleDirectory().resolve(Path.of("build", projectKey(project) + "-differences"));
  }

  private static Path pinSonarqubePluginScript() throws IOException {
    return moduleDirectory().resolve(Path.of("src", "integrationTest", "resources", "pin-sonarqube-plugin.gradle"));
  }

  private static GradleBuild gradleBuild(String project, Map<String, String> properties) throws IOException {
    return GradleBuild.create(projectDirectory(project).toFile())
      .setProperties(properties)
      .setEnvironmentVariable("GRADLE_OPTS", "-Xmx1024m")
      .addArguments("--stacktrace", "--info", "--console=plain", "-x", "test")
      .addArguments("--init-script", pinSonarqubePluginScript().toString())
      .setTimeoutSeconds(600);
  }

  private void executeGradleBuildAndAssertDifferences(String project, Map<String, String> additionalProperties) throws IOException {
    if (CLEAN_PROJECT_BINARIES) {
      orchestrator.executeBuild(gradleBuild(project, Map.of())
        .setTasks("clean"));
    }

    Map<String, String> properties = prepareAnalysisConfiguration(project, additionalProperties);
    String debugPort = System.getProperty("sonar.rulingDebugPort");
    if (debugPort != null) {
      properties.put("org.gradle.debug", "true");
      properties.put("org.gradle.debug.port", debugPort);
    }
    executeBuildAndAssertDifferences(project, gradleBuild(project, properties)
      .setTasks("build").addArguments("sonar", "-x", "test"));
  }

  private void executeBuildAndAssertDifferences(String project, Build<?> build) throws IOException {
    build.setProperty("sonar.scanner.skipJreProvisioning", "true");
    String projectKey = projectKey(project);
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "kotlin", "rules");
    orchestrator.executeBuild(build);
    String litsDifference = new String(Files.readAllBytes(litsDifferencesFilePath(project)));
    assertThat(litsDifference).isEmpty();
  }

  @AfterAll
  public static void after() {
    if (keepSonarqubeRunning) {
      try {
        LOG.info("::: Intentionally keep SonarQube running at {} use CTRL+C to stop it :::",
          orchestrator.getServer().getUrl());
        Thread.sleep(TimeUnit.HOURS.toMillis(2));
      } catch (InterruptedException e) {
        // CTRL-C was pressed, ignore the exception
      }
    }
  }

}
