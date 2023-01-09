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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  DuplicationsTest.class,
  ExternalReportTest.class,
  MeasuresTest.class,
  SuppressWarningsTest.class,
  SurefireTest.class
})
public class Tests {

  static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static final Set<String> LANGUAGES = new HashSet<>(Collections.singletonList("kotlin"));

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv();
    addLanguagePlugins(orchestratorBuilder);
    ORCHESTRATOR = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/suppress-warnings-kotlin.xml"))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/norule.xml"))
      .build();
  }

  static void addLanguagePlugins(OrchestratorBuilder builder) {
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

}
