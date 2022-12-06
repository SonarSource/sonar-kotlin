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
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.GradleBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class SlangRulingTest {

  private static final Logger LOG = LoggerFactory.getLogger(SlangRulingTest.class);

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static Orchestrator orchestrator;
  private static boolean keepSonarqubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));
  private static final boolean IGNORE_EXPECTED_ISSUES_AND_REPORT_ALL = "true".equals(System.getProperty("reportAll"));
  private static final boolean CLEAN_PROJECT_BINARIES = "true".equals(System.getProperty("cleanProjects"));
  private static final Set<String> LANGUAGES = new HashSet<>(Collections.singletonList("kotlin"));

  @BeforeClass
  public static void setUp() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.10.0.2181"));

    addLanguagePlugins(builder);

    orchestrator = builder.build();
    orchestrator.start();

    ProfileGenerator.RulesConfiguration kotlinRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    kotlinRulesConfiguration.add("S1451", "headerFormat", "/\\*\n \\* Copyright \\d{4}-\\d{4} JetBrains s\\.r\\.o\\.");
    kotlinRulesConfiguration.add("S1451", "isRegularExpression", "true");

    File kotlinProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "kotlin", "kotlin", kotlinRulesConfiguration, Collections.emptySet());

    orchestrator.getServer().restoreProfile(FileLocation.of(kotlinProfile));
  }

  private static void addLanguagePlugins(OrchestratorBuilder builder) {
    String slangVersion = System.getProperty("slangVersion");

    LANGUAGES.forEach(language -> {
      Location pluginLocation;
      String plugin = "sonar-" + language +"-plugin";
      if (slangVersion == null || slangVersion.isEmpty()) {
        // use the plugin that was built on local machine
        pluginLocation = FileLocation.byWildcardMavenFilename(new File("../../" + plugin + "/build/libs"), plugin + "-*-all.jar");
      } else {
        // QA environment downloads the plugin built by the CI job
        pluginLocation = MavenLocation.of("org.sonarsource.kotlin", plugin, slangVersion);
      }

      builder.addPlugin(pluginLocation);
    });
  }

  @Test
  public void test_kotlin_ktor() throws IOException {
    List<String> ktorDirs = Arrays.asList(
      "ktor-client/ktor-client-apache/",
      "ktor-client/ktor-client-cio/",
      "ktor-client/ktor-client-core/",
      "ktor-client/ktor-client-jetty/",
      "ktor-client/ktor-client-tests/",
      "ktor-features/ktor-auth/",
      "ktor-features/ktor-auth-jwt/",
      "ktor-features/ktor-auth-ldap/",
      "ktor-features/ktor-freemarker/",
      "ktor-features/ktor-gson/",
      "ktor-features/ktor-html-builder/",
      "ktor-features/ktor-jackson/",
      "ktor-features/ktor-locations/",
      "ktor-features/ktor-metrics/",
      "ktor-features/ktor-server-sessios/",
      "ktor-features/ktor-velocity/",
      "ktor-features/ktor-websockets/",
      "ktor-http/",
      "ktor-http-cio/",
      "ktor-network/",
      "ktor-network-tls/",
      "ktor-server/ktor-server-cio/",
      "ktor-server/ktor-server-core/",
      "ktor-server/ktor-server-host-common/",
      "ktor-server/ktor-server-jetty/",
      "ktor-server/ktor-server-netty/",
      "ktor-server/ktor-server-servlet/",
      "ktor-server/ktor-server-test-host/",
      "ktor-server/ktor-server-tomcat/");

    String binaries = ktorDirs.stream().map(dir -> FileLocation.of("../sources/kotlin/ktor/" + dir + "build/classes"))
      .map(SlangRulingTest::getFileLocationAbsolutePath)
      .collect(Collectors.joining(","));

    executeSonarScannerAndAssertDifferences("kotlin/ktor", Map.of(
      "sonar.inclusions", "sources/kotlin/ktor/**/*.kt",
      "sonar.exclusions", "**/testData/**/*",
      "sonar.java.binaries", binaries));
  }

  @Test
  public void test_kotlin_compiler() throws IOException {
    List<String> exclusions = List.of(
      "**/testData/**/*",
      "sources/kotlin/kotlin/compiler/daemon/src/org/jetbrains/kotlin/daemon/CompileServiceImpl.kt",
      "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/ktPsiUtil.kt",
      "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/psiUtils.kt",
      "sources/kotlin/kotlin/j2k/src/org/jetbrains/kotlin/j2k/ast/Statements.kt");
    executeSonarScannerAndAssertDifferences("kotlin/kotlin", Map.of(
      "sonar.inclusions", "sources/kotlin/kotlin/**/*.kt",
      "sonar.exclusions", String.join(",", exclusions)));
  }

  @Test
  public void test_resources_sources() throws IOException {
    executeSonarScannerAndAssertDifferences("kotlin/test-resources-sources", Map.of(
      "sonar.inclusions", "ruling/src/test/resources/sources/kotlin/**/*.kt",
      "sonar.java.libraries", System.getProperty("gradle.main.compile.classpath").replace(File.pathSeparatorChar, ',')
    ));
  }

  @Test
  public void test_kotlin_android() throws IOException {
    executeSonarScannerAndAssertDifferences("kotlin/android-architecture-components", Map.of(
      "sonar.inclusions", "sources/kotlin/android-architecture-components/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"
    ));
  }

  @Test
  public void test_kotlin_corda() throws IOException {
    executeSonarScannerAndAssertDifferences("kotlin/corda", Map.of(
      "sonar.inclusions", "sources/kotlin/corda/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"
    ));
  }

  @Test
  public void test_kotlin_intellij_rust() throws IOException {
    executeSonarScannerAndAssertDifferences("kotlin/intellij-rust", Map.of(
      "sonar.inclusions", "sources/kotlin/intellij-rust/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"
    ));
  }

  @Test
  public void test_kotlin_okio() throws IOException {
    executeSonarScannerAndAssertDifferences("kotlin/okio", Map.of(
      "sonar.inclusions", "sources/kotlin/okio/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"));
  }

  @Test
  public void test_kotlin_language_server() throws IOException {
    executeGradleBuildAndAssertDifferences("kotlin/kotlin-language-server", Map.of());
  }

  private static String getFileLocationAbsolutePath(FileLocation location) {
    try {
      return location.getFile().getCanonicalFile().getAbsolutePath();
    } catch (IOException e) {
      return "";
    }
  }

  private static Map<String, String> prepareAnalysisConfiguration(String project, Map<String, String> additionalProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(additionalProperties);
    properties.put("sonar.projectKey", projectKey(project));
    properties.put("sonar.projectName", project);
    properties.put("sonar.projectVersion", "1");
    properties.put("sonar.sourceEncoding", "UTF-8");
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");
    properties.put("sonar.kotlin.performance.measure", "true");
    properties.put("sonar.cpd.exclusions", "**/*");
    properties.put("sonar.scm.disabled", "true");
    properties.put("sonar.internal.analysis.failFast", "true");

    Path rulingDirectory = rulingDirectory();
    Path performanceMeasuresDirectory = rulingDirectory.resolve(Path.of("build", "performance")).resolve(projectRelativePath(project));
    Files.createDirectories(performanceMeasuresDirectory);
    properties.put("sonar.kotlin.performance.measure.json", performanceMeasuresDirectory.resolve("sonar.kotlin.performance.measure.json").toString());

    Path expectedDirectory;
    if (IGNORE_EXPECTED_ISSUES_AND_REPORT_ALL) {
      expectedDirectory = rulingDirectory.resolve(Path.of("build", "tmp", "empty"));
      Files.createDirectories(expectedDirectory);
    } else {
      expectedDirectory = rulingDirectory.resolve(Path.of("src", "test", "resources", "expected")).resolve(projectRelativePath(project));
    }
    Path actualDirectory = rulingDirectory.resolve(Path.of("build", "tmp", "actual", project));
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

  private static Path rulingDirectory() throws IOException {
    Path currentDirectory = Path.of(".").toRealPath();
    if (!currentDirectory.getFileName().toString().equals("ruling")) {
      throw new IOException("Current directory is not its/ruling but: " + currentDirectory);
    }
    return currentDirectory;
  }

  private static Path litsDifferencesFilePath(String project) throws IOException {
    return rulingDirectory().resolve(Path.of("build", projectKey(project) + "-differences"));
  }

  private static GradleBuild gradleBuild(String project, Map<String, String> properties) throws IOException {
    return GradleBuild.create(projectDirectory(project).toFile())
      .setProperties(properties)
      .setEnvironmentVariable("GRADLE_OPTS", "-Xmx1024m")
      .addArguments("--stacktrace", "--info", "--console=plain", "-x", "test")
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

  private void executeSonarScannerAndAssertDifferences(String project, Map<String, String> additionalProperties) throws IOException {
    Map<String, String> properties = prepareAnalysisConfiguration(project, additionalProperties);

    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setSourceDirs("./")
      .setProperties(properties)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

    String debugPort = System.getProperty("sonar.rulingDebugPort");
    if (debugPort != null) {
      build.setEnvironmentVariable("SONAR_SCANNER_DEBUG_OPTS",
        "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + debugPort);
    }

    executeBuildAndAssertDifferences(project, build);
  }

  private void executeBuildAndAssertDifferences(String project, Build<?> build) throws IOException {
    String projectKey = projectKey(project);
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    LANGUAGES.forEach(lang -> orchestrator.getServer().associateProjectToQualityProfile(projectKey, lang, "rules"));
    orchestrator.executeBuild(build);
    String litsDifference = new String(Files.readAllBytes(litsDifferencesFilePath(project)));
    assertThat(litsDifference).isEmpty();
  }

  @AfterClass
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
