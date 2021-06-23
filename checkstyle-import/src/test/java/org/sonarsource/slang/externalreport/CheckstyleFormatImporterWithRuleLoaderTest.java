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
package org.sonarsource.slang.externalreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.externalreport.CheckstyleFormatImporterTest.LINTER_KEY;
import static org.sonarsource.slang.externalreport.CheckstyleFormatImporterTest.PROJECT_DIR;
import static org.sonarsource.slang.externalreport.CheckstyleFormatImporterTest.createContext;

@EnableRuleMigrationSupport
class CheckstyleFormatImporterWithRuleLoaderTest {

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(
    LINTER_KEY,
    LINTER_KEY,
    "test-linter-rules.json",
    "kt");

  @Test
  void import_detekt_issues_with_rule_loader() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("detekt-checkstyle.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("detekt.EmptyIfBlock");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo("This empty block of code can be removed.");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:main.kt");
    assertThat(second.ruleKey().rule()).isEqualTo("detekt.MagicNumber");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.INFO);
    assertThat(second.remediationEffort().longValue()).isEqualTo(10L);
    assertThat(second.primaryLocation().message()).isEqualTo("This expression contains a magic number. Consider defining it to a well named constant.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:A.kt");
    assertThat(third.ruleKey().rule()).isEqualTo("detekt.EqualsWithHashCodeExist");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(third.primaryLocation().message()).isEqualTo("A class should always override hashCode when overriding equals and the other way around.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  private List<ExternalIssue> importIssues(String fileName) throws IOException {
    SensorContextTester context = createContext();
    CheckstyleFormatImporter importer = new CheckstyleFormatImporterWithRuleLoader(context, LINTER_KEY, RULE_LOADER);
    importer.importFile(PROJECT_DIR.resolve(fileName).toAbsolutePath().toFile());
    return new ArrayList<>(context.allExternalIssues());
  }

}
