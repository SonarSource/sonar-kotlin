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
package org.sonarsource.kotlin.its;

import com.sonarsource.scanner.integrationtester.dsl.ActiveRule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Builds the active-rules list for the ruling test by scanning the plugin jar's rule metadata, replacing
 * {@code org.sonarsource.analyzer.commons.ProfileGenerator} (which needed a live server to register a profile).
 */
final class RulingRules {

  private RulingRules() {
    // utility class
  }

  static List<ActiveRule> nativeRules(Path analyzerJar, String languageKey, Map<String, Map<String, String>> parametersByRuleKey) {
    Pattern pattern = Pattern.compile("(?:org|com)/sonar/l10n/" + Pattern.quote(languageKey)
      + "/rules/" + Pattern.quote(languageKey) + "/([^/]+)\\.json");
    var ruleKeys = new TreeSet<String>();
    try (JarFile jar = new JarFile(analyzerJar.toFile())) {
      var entries = jar.entries();
      while (entries.hasMoreElements()) {
        var matcher = pattern.matcher(entries.nextElement().getName());
        if (matcher.matches() && !"Sonar_way_profile".equals(matcher.group(1))) {
          ruleKeys.add(matcher.group(1));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read rules from analyzer JAR " + analyzerJar, e);
    }
    if (ruleKeys.isEmpty()) {
      throw new IllegalStateException("No rules found for language '" + languageKey + "' in " + analyzerJar);
    }
    return ruleKeys.stream()
      .map(ruleKey -> activeRule(languageKey, ruleKey, parametersByRuleKey.getOrDefault(ruleKey, Map.of())))
      .toList();
  }

  private static ActiveRule activeRule(String languageKey, String ruleKey, Map<String, String> parameters) {
    var builder = ActiveRule.builder()
      .withKey(languageKey, ruleKey)
      .withName(ruleKey)
      .withLanguageKey(languageKey)
      .withSeverity(ActiveRule.Severity.INFO);
    parameters.forEach(builder::withParameter);
    return builder.build();
  }
}
