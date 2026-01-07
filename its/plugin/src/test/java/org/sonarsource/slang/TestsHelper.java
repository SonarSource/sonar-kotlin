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

import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class TestsHelper {

  static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static final Set<String> LANGUAGES = new HashSet<>(Collections.singletonList("kotlin"));

  public static final OrchestratorExtension ORCHESTRATOR;

  static {
    OrchestratorExtensionBuilder orchestratorBuilder = OrchestratorExtension.builderEnv();
    addLanguagePlugins(orchestratorBuilder);
    ORCHESTRATOR = orchestratorBuilder
            .setEdition(Edition.ENTERPRISE_LW)
            .activateLicense()
            .useDefaultAdminCredentialsForBuilds(true)
            .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
            .restoreProfileAtStartup(FileLocation.of("src/test/resources/suppress-warnings-kotlin.xml"))
            .restoreProfileAtStartup(FileLocation.of("src/test/resources/norule.xml"))
            .setServerProperty("sonar.telemetry.enable", "false")
            .build();
  }

  static void addLanguagePlugins(OrchestratorBuilder builder) {
    String slangVersion = System.getProperty("slangVersion");

    LANGUAGES.forEach(language -> {
      Location pluginLocation;
      String plugin = "sonar-" + language +"-plugin";
      if (slangVersion == null || slangVersion.isEmpty()) {
        // use the plugin that was built on local machine
        pluginLocation = FileLocation.byWildcardMavenFilename(new File("../../" + plugin + "/build/libs"), plugin + ".jar");
      } else {
        // QA environment downloads the plugin built by the CI job
        pluginLocation = MavenLocation.of("org.sonarsource.kotlin", plugin, slangVersion);
      }

      builder.addPlugin(pluginLocation);
    });
  }

}
