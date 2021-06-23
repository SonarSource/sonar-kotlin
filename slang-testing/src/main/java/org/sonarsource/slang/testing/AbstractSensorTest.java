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
package org.sonarsource.slang.testing;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public abstract class AbstractSensorTest {

  @org.junit.Rule
  public JUnitTempFolder temp = new JUnitTempFolder();

  protected File baseDir;
  protected SensorContextTester context;
  protected FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);

  @org.junit.Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @BeforeEach
  public void setup() {
    baseDir = temp.newDir();
    context = SensorContextTester.create(baseDir);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
  }

  protected CheckFactory checkFactory(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleKey : ruleKeys) {
      NewActiveRule newRule = new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(repositoryKey(), ruleKey))
        .setName(ruleKey)
        .build();
      builder.addRule(newRule);
    }
    context.setActiveRules(builder.build());
    return new CheckFactory(context.activeRules());
  }

  protected InputFile createInputFile(String relativePath, String content) {
    return new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(language().getKey())
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content)
      .build();
  }

  protected abstract String repositoryKey();

  protected abstract Language language();

}
