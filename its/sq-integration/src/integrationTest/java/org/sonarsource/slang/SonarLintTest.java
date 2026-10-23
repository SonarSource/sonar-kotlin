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

import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.locator.Locators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesAndTrackParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.StandaloneRuleConfigDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.UpdateStandaloneRulesConfigurationParams;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;
import org.sonarsource.sonarlint.core.test.utils.junit5.SonarLintTestHarness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

public class SonarLintTest {

  private static final String CONFIG_SCOPE_ID = "testModule";

  @org.sonarsource.sonarlint.core.test.utils.junit5.SonarLintTest
  void test_kotlin(SonarLintTestHarness harness, @TempDir Path baseDir) throws IOException {
    var path = baseDir.resolve("foo.kt");
    Files.writeString(path,
      "fun foo_bar() {\n" +
        "    if (true) { \n" +
        "        val password = \"blabla\"\n" +
        "    } \n" +
        "}\n" +
        "\n" +
        "fun foo_bar_nosonar() {} // NOSONAR \n");
    var inputFile = new ClientFileDto(path.toUri(), baseDir.relativize(path), CONFIG_SCOPE_ID,
      false, null, path, null, Language.KOTLIN, true);
    var client = harness.newFakeClient()
      .withInitialFs(CONFIG_SCOPE_ID, baseDir, List.of(inputFile))
      .build();
    var builder = harness.newBackend()
      .withEnabledLanguageInStandaloneMode(Language.KOTLIN)
      .withUnboundConfigScope(CONFIG_SCOPE_ID);
    getPluginJarLocations().forEach(builder::withStandaloneEmbeddedPlugin);
    var backend = builder.start(client);
    var activeRules = Set.of("kotlin:S100", "kotlin:S1481", "kotlin:S1145");
    var ruleConfiguration = backend.getRulesService().listAllStandaloneRulesDefinitions().join().getRulesByKey().keySet().stream()
      .collect(Collectors.toMap(ruleKey -> ruleKey, ruleKey -> new StandaloneRuleConfigDto(activeRules.contains(ruleKey), Map.of())));
    backend.getRulesService().updateStandaloneRulesConfiguration(new UpdateStandaloneRulesConfigurationParams(ruleConfiguration));

    var analysis = backend.getAnalysisService().analyzeFilesAndTrack(new AnalyzeFilesAndTrackParams(
      CONFIG_SCOPE_ID, UUID.randomUUID(), List.of(inputFile.getUri()), Map.of(), false)).join();

    assertThat(analysis.getFailedAnalysisFiles()).isEmpty();
    await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
      var issuesByFile = client.getRaisedIssuesForScopeId(CONFIG_SCOPE_ID);
      assertThat(issuesByFile).containsOnlyKeys(inputFile.getUri());
      assertThat(issuesByFile.get(inputFile.getUri()))
        .extracting(issue -> issue.getRuleKey(), issue -> issue.getTextRange().getStartLine())
        .containsExactlyInAnyOrder(
          tuple("kotlin:S100", 1),
          tuple("kotlin:S1481", 3),
          tuple("kotlin:S1145", 2));
    });
  }

  private static Set<Path> getPluginJarLocations() {
    var orchestratorBuilder = OrchestratorExtension.builderEnv();
    TestsHelper.addLanguagePlugins(orchestratorBuilder);
    var orchestrator = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(TestsHelper.SQ_VERSION_PROPERTY, TestsHelper.DEFAULT_SQ_VERSION))
      .build();
    var locators = new Locators(orchestrator.getConfiguration());
    return orchestrator.getDistribution().getPluginLocations().stream()
      .filter(location -> !location.toString().contains("sonar-reset-data-plugin"))
      .map(plugin -> locators.locate(plugin).toPath())
      .collect(Collectors.toSet());
  }

}
