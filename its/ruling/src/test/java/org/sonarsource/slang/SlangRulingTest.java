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

  private static final Set<String> LANGUAGES = new HashSet<>(Arrays.asList("kotlin", "ruby", "scala", "go"));

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

    ProfileGenerator.RulesConfiguration rubyRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    rubyRulesConfiguration.add("S1451", "headerFormat", "# Copyright 201\\d Twitch Interactive, Inc.  All Rights Reserved.");
    rubyRulesConfiguration.add("S1451", "isRegularExpression", "true");
    rubyRulesConfiguration.add("S1479", "maximum", "10");

    ProfileGenerator.RulesConfiguration scalaRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    scalaRulesConfiguration.add("S1451", "headerFormat", "^(?i).*copyright");
    scalaRulesConfiguration.add("S1451", "isRegularExpression", "true");

    ProfileGenerator.RulesConfiguration goRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    goRulesConfiguration.add("S1451", "headerFormat", "^(?i).*copyright");
    goRulesConfiguration.add("S1451", "isRegularExpression", "true");

    File kotlinProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "kotlin", "kotlin", kotlinRulesConfiguration, Collections.emptySet());
    File rubyProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "ruby", "ruby", rubyRulesConfiguration, Collections.emptySet());
    File scalaProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "scala", "scala", scalaRulesConfiguration, Collections.emptySet());
    File goProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "go", "go", goRulesConfiguration, Collections.emptySet());

    orchestrator.getServer().restoreProfile(FileLocation.of(kotlinProfile));
    orchestrator.getServer().restoreProfile(FileLocation.of(rubyProfile));
    orchestrator.getServer().restoreProfile(FileLocation.of(scalaProfile));
    orchestrator.getServer().restoreProfile(FileLocation.of(goProfile));
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
        pluginLocation = MavenLocation.of("org.sonarsource.slang", plugin, slangVersion);
      }

      builder.addPlugin(pluginLocation);
    });
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void kotlin_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_kotlin();
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void ruby_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_ruby();
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void scala_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_scala();
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void go_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_go();
  }

  @Test
  public void test_kotlin() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/kotlin/**/*.kt, ruling/src/test/resources/sources/kotlin/**/*.kt");
    properties.put("sonar.exclusions", String.join(",",
            "**/testData/**/*"
            , "sources/kotlin/corda/samples/simm-valuation-demo/flows/src/main/kotlin/net/corda/vega/analytics/example/OGSwapPricingCcpExample.kt"
            , "sources/kotlin/corda/samples/simm-valuation-demo/flows/src/main/kotlin/net/corda/vega/analytics/example/OGSwapPricingExample.kt"
            , "sources/kotlin/kotlin/compiler/daemon/src/org/jetbrains/kotlin/daemon/CompileServiceImpl.kt"
            , "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/ktPsiUtil.kt"
            , "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/psiUtils.kt"
            , "sources/kotlin/kotlin/j2k/src/org/jetbrains/kotlin/j2k/ast/Statements.kt"
    ));

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
    
    run_ruling_test("kotlin", properties);
  }

  private static String getFileLocationAbsolutePath(FileLocation location) {
    try {
      return location.getFile().getCanonicalFile().getAbsolutePath();
    } catch (IOException e) {
      return "";
    }
  }


  @Test
  public void test_ruby() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/ruby/**/*.rb, ruling/src/test/resources/sources/ruby/**/*.rb");
    run_ruling_test("ruby", properties);
  }

  @Test
  public void test_scala() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/scala/**/*.scala, ruling/src/test/resources/sources/scala/**/*.scala");
    run_ruling_test("scala", properties);
  }

  @Test
  public void test_go() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/go/**/*.go, ruling/src/test/resources/sources/go/**/*.go");
    properties.put("sonar.exclusions", "**/*generated*.go, **/*.pb.go");
    properties.put("sonar.tests", ".");
    properties.put("sonar.test.inclusions", "**/*_test.go");
    run_ruling_test("go", properties);
  }

  private void run_ruling_test(String language, Map<String, String> languageProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(languageProperties);
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");

    String projectKey = language + "-project";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    LANGUAGES.forEach(lang -> orchestrator.getServer().associateProjectToQualityProfile(projectKey, lang, "rules"));

    File actualDirectory = FileLocation.of("build/tmp/actual/" + language).getFile();
    actualDirectory.mkdirs();

    File litsDifferencesFile = FileLocation.of("build/" + language + "-differences").getFile();
    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperties(properties)
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected/" + language).getFile().getAbsolutePath())
      .setProperty("dump.new", actualDirectory.getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.language", language)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

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
