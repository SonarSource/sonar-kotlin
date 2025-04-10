/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import com.sonar.orchestrator.OrchestratorBuilder;

import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.FileLocation;

public class TestsHelper {

  static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  public static final OrchestratorExtension ORCHESTRATOR;

  static {
    OrchestratorExtensionBuilder orchestratorBuilder = OrchestratorExtension.builderEnv();
    addLanguagePlugins(orchestratorBuilder);
    ORCHESTRATOR = orchestratorBuilder
            .useDefaultAdminCredentialsForBuilds(true)
            .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
            .restoreProfileAtStartup(FileLocation.of("src/test/resources/suppress-warnings-kotlin.xml"))
            .restoreProfileAtStartup(FileLocation.of("src/test/resources/norule.xml"))
            .setServerProperty("sonar.telemetry.enable", "false")
            .build();
  }

  static void addLanguagePlugins(OrchestratorBuilder builder) {
    builder.addPlugin(FileLocation.of("../../sonar-kotlin-plugin/build/libs/sonar-kotlin-plugin.jar"));
  }

}
