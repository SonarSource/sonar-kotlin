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
package org.sonarsource.slang.plugin;

import java.io.File;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.cpd.internal.TokensLine;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class CpdVisitorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  void test() throws Exception {
    File file = tempFolder.newFile();
    String content = "import util; foo(x\n * 42 \n+ \"abc\");";
    SensorContextTester sensorContext = SensorContextTester.create(tempFolder.getRoot());
    DefaultInputFile inputFile = new TestInputFileBuilder("moduleKey", file.getName())
      .setContents(content)
      .build();
    Tree root = new SLangConverter().parse(content);
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    new CpdVisitor().scan(ctx, root);

    List<TokensLine> cpdTokenLines = sensorContext.cpdTokens(inputFile.key());
    assertThat(cpdTokenLines).hasSize(3);

    assertThat(cpdTokenLines.get(0).getValue()).isEqualTo("foo(x");
    assertThat(cpdTokenLines.get(0).getStartLine()).isEqualTo(1);
    assertThat(cpdTokenLines.get(0).getStartUnit()).isEqualTo(1);
    assertThat(cpdTokenLines.get(0).getEndUnit()).isEqualTo(3);

    assertThat(cpdTokenLines.get(1).getValue()).isEqualTo("*42");
    assertThat(cpdTokenLines.get(1).getStartLine()).isEqualTo(2);
    assertThat(cpdTokenLines.get(1).getStartUnit()).isEqualTo(4);
    assertThat(cpdTokenLines.get(1).getEndUnit()).isEqualTo(5);

    assertThat(cpdTokenLines.get(2).getValue()).isEqualTo("+LITERAL);");
    assertThat(cpdTokenLines.get(2).getStartLine()).isEqualTo(3);
    assertThat(cpdTokenLines.get(2).getStartUnit()).isEqualTo(6);
    assertThat(cpdTokenLines.get(2).getEndUnit()).isEqualTo(9);
  }

}
