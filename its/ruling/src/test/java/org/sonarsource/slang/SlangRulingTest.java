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
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class SlangRulingTest {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static Orchestrator orchestrator;
  private static boolean keepSonarqubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));

  private static final Set<String> LANGUAGES = new HashSet<>(Collections.singletonList("kotlin"));

  @BeforeClass
  public static void setUp() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.8.0.1209"));

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
      if (StringUtils.isEmpty(slangVersion)) {
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
  // @Ignore because it should only be run manually
  @Ignore
  public void kotlin_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_kotlin_ktor();
    test_kotlin_android();
    test_kotlin_corda();
    test_kotlin_compiler();
    test_kotlin_okio();
    test_kotlin_intellij_rust();
  }

  @Test
  public void test_kotlin_ktor() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/ktor/**/*.kt");
    properties.put("sonar.exclusions", "**/testData/**/*");

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
    properties.put("sonar.java.binaries", binaries);

    run_ruling_test("kotlin/ktor", properties);
  }

  @Test
  public void test_kotlin_compiler() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/kotlin/**/*.kt");
    properties.put("sonar.exclusions", String.join(",",
            "**/testData/**/*"
            , "sources/kotlin/kotlin/compiler/daemon/src/org/jetbrains/kotlin/daemon/CompileServiceImpl.kt"
            , "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/ktPsiUtil.kt"
            , "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/psiUtils.kt"
            , "sources/kotlin/kotlin/j2k/src/org/jetbrains/kotlin/j2k/ast/Statements.kt"
    ));

    run_ruling_test("kotlin/kotlin", properties);
  }

  @Test
  public void test_resources_sources() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "ruling/src/test/resources/sources/kotlin/**/*.kt");
    properties.put("sonar.java.libraries", System.getProperty("gradle.main.compile.classpath"));
    run_ruling_test("kotlin/test-resources-sources", properties);
  }

  @Test
  public void test_kotlin_android() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/android-architecture-components/**/*.kt");
    properties.put("sonar.exclusions", "**/testData/**/*");

    run_ruling_test("kotlin/android-architecture-components", properties);
  }

  @Test
  public void test_kotlin_corda() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/corda/**/*.kt");
    properties.put("sonar.exclusions", "**/testData/**/*");

    run_ruling_test("kotlin/corda", properties);
  }

  @Test
  public void test_kotlin_intellij_rust() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/intellij-rust/**/*.kt");
    properties.put("sonar.exclusions", "**/testData/**/*");

    run_ruling_test("kotlin/intellij-rust", properties);
  }

  @Test
  public void test_kotlin_okio() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/okio/**/*.kt");
    properties.put("sonar.exclusions", "**/testData/**/*");

    run_ruling_test("kotlin/okio", properties);
  }

  private static String getFileLocationAbsolutePath(FileLocation location) {
    try {
      return location.getFile().getCanonicalFile().getAbsolutePath();
    } catch (IOException e) {
      return "";
    }
  }

  private void run_ruling_test(String project, Map<String, String> projectProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(projectProperties);
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");
    properties.put("sonar.kotlin.performance.measure", "true");

    File performanceMeasuresDirectory = FileLocation.of("build/performance/" + project).getFile();
    performanceMeasuresDirectory.mkdirs();

    properties.put("sonar.kotlin.performance.measure.json", performanceMeasuresDirectory.getAbsolutePath() + "/sonar.kotlin.performance.measure.json");

    String projectKey = project.replace("/", "-") + "-project";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    LANGUAGES.forEach(lang -> orchestrator.getServer().associateProjectToQualityProfile(projectKey, lang, "rules"));

    File actualDirectory = FileLocation.of("build/tmp/actual/" + project).getFile();
    actualDirectory.mkdirs();

    File litsDifferencesFile = FileLocation.of("build/" + projectKey + "-differences").getFile();
    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperties(properties)
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected/" + project).getFile().getAbsolutePath())
      .setProperty("dump.new", actualDirectory.getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.project", project)
      .setProperty("sonar.internal.analysis.failFast", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

    String debugPort = System.getProperty("sonar.rulingDebugPort");
    if (debugPort != null) {
      build.setEnvironmentVariable(
        "SONAR_SCANNER_DEBUG_OPTS", "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + debugPort);
    }

    orchestrator.executeBuild(build);

    String litsDifference = new String(Files.readAllBytes(litsDifferencesFile.toPath()));
    assertThat(litsDifference).isEmpty();
  }

  @AfterClass
  public static void after() {
    if (keepSonarqubeRunning) {
      // keep server running, use CTRL-C to stop it
      new Scanner(System.in).next();
    }
  }

}
