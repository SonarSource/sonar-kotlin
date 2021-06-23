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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class MetricVisitorTest {

  private NoSonarFilter mockNoSonarFilter;
  private SLangConverter parser = new SLangConverter();
  private MetricVisitor visitor;
  private SensorContextTester sensorContext;
  private DefaultInputFile inputFile;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempFolder.getRoot());
    FileLinesContext mockFileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory mockFileLinesContextFactory = mock(FileLinesContextFactory.class);
    mockNoSonarFilter = mock(NoSonarFilter.class);
    when(mockFileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(mockFileLinesContext);
    visitor = new MetricVisitor(mockFileLinesContextFactory, mockNoSonarFilter);
  }

  @Test
  void emptySource() throws Exception {
    scan("");
    assertThat(visitor.linesOfCode()).isEmpty();
    assertThat(visitor.commentLines()).isEmpty();
    assertThat(visitor.numberOfFunctions()).isZero();
    verify(mockNoSonarFilter).noSonarInFile(inputFile, new HashSet<>());
  }

  @Test
  void linesOfCode() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.linesOfCode()).containsExactly(1, 3, 4);
  }

  @Test
  void commentLines() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.commentLines()).containsExactly(2, 3);
  }

  @Test
  void multiLineComment() throws Exception {
    scan("" +
      "/*start\n" +
      "x + 1\n" +
      "end*/");
    assertThat(visitor.commentLines()).containsExactly(1, 2, 3);
    assertThat(visitor.linesOfCode()).isEmpty();
  }

  @Test
  void nosonarLines() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// NOSONAR comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.nosonarLines()).containsExactly(2);
    Set<Integer> nosonarLines = new HashSet<>();
    nosonarLines.add(2);
    verify(mockNoSonarFilter).noSonarInFile(inputFile, nosonarLines);
  }

  @Test
  void functions() throws Exception {
    scan("" +
      "x + 1;\n" +
      "x = true || false;");
    assertThat(visitor.numberOfFunctions()).isZero();
    scan("" +
      "x + 1;\n" +
      "fun noBodyFunction();\n" + // Only functions with implementation bodies are considered for the metric
      "fun() { x = 1; }\n" + // Anonymous functions are not considered for function metric computation
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.numberOfFunctions()).isEqualTo(1);
  }

  @Test
  void classes() throws Exception {
    scan("" +
            "x + 1;\n" +
            "x = true || false;");
    assertThat(visitor.numberOfClasses()).isZero();
    scan("" +
            "class C {}\n" +
            "fun function() {}\n" +
            "class D { int val x = 0; }\n" +
            "class E {\n" +
            "  fun doSomething(int x) {}\n" +
            "}");
    assertThat(visitor.numberOfClasses()).isEqualTo(3);
  }

  @Test
  void cognitiveComplexity() throws Exception {
    scan("" +
      "class A { fun foo() { if(1 != 1) 1; } }" + // +1 for 'if'
      "fun function() {" +
      "  if (1 != 1) {" + // +1 for 'if'
      "    if (1 != 1) {" + // + 2 for nested 'if'
      "      1" +
      "    }" +
      "  };" +
      "  class B {" + // Nesting level reset here because of class declaration
      "    fun bar(int a) {" +
      "      match(a) {" + // +1 for match
      "        1 -> doSomething();" +
      "        2 -> doSomething();" +
      "        else -> if (1 != 1) doSomething();" + // +2 for nested 'if'
      "      }" +
      "    }" +
      "  };" +
      "}");
    assertThat(visitor.cognitiveComplexity()).isEqualTo(7);
  }

  @Test
  void executable_lines() throws Exception {
    scan("" +
      "package abc;\n" +
      "import x;\n" +
      "class A {\n" +
      "  fun foo() {\n" +
      "    statementOnSeveralLines(a,\n" +
      "      b);\n" +
      "  };\n" +
      "}\n" +
      "{\n" +
      "  x = 42;\n" +
      "};");
    assertThat(visitor.executableLines()).containsExactly(5, 10);
  }

  private void scan(String code) throws IOException {
    inputFile = new TestInputFileBuilder("moduleKey", tempFolder.newFile().getName())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(code).build();
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    visitor.scan(ctx, parser.parse(code));
  }

}
