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
package org.sonarsource.kotlin.converter;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.ImportDeclarationTree;
import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.PackageDeclarationTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.ThrowTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.parser.SLangConverter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.LESS_THAN;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.BINARY;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.DECIMAL;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.HEXADECIMAL;
import static org.sonarsource.slang.api.LoopTree.LoopKind.DOWHILE;
import static org.sonarsource.slang.api.LoopTree.LoopKind.FOR;
import static org.sonarsource.slang.api.LoopTree.LoopKind.WHILE;
import static org.sonarsource.slang.api.ModifierTree.Kind.OVERRIDE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PUBLIC;
import static org.sonarsource.slang.api.Token.Type.KEYWORD;
import static org.sonarsource.slang.api.Token.Type.OTHER;
import static org.sonarsource.slang.api.Token.Type.STRING_LITERAL;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;

class KotlinConverterTest {

  private KotlinConverter converter = new KotlinConverter(Collections.emptyList());

  @Test
  void testParseException() {
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("enum class A {\n<!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!>}"));
    assertThat(e.getMessage()).isEqualTo("Cannot convert file due to syntactic errors");
  }

  @Test
  void testWinEOL() {
    Tree tree = converter.parse(
      "fun main(args: Array<String>) {\r\n" +
        "\r\n" +
        "}\r\n");
    assertThat(tree.children()).hasSize(1);
  }

  @Test
  void testParsedExceptionWithPartialParsing() {
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("fun foo() { a b c ...}"));
    assertThat(e.getMessage()).isEqualTo("Cannot convert file due to syntactic errors");
  }

  @Test
  void testParseWithoutNullPointer() {
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("package ${package}"));
    assertThat(e.getMessage()).isEqualTo("Cannot convert file due to syntactic errors");
  }

  @Test
  void testFirstCpdToken() {
    TopLevelTree topLevel = (TopLevelTree) converter.parse("" +
      "@file:JvmName(\"xxx\")\n" +
      "package com.example\n" +
      "import com.example.MyClass\n" +
      "fun main(args: Array<String>) {}\n" +
      "class A {}");
    assertThat(topLevel.declarations()).hasSize(5);
    assertThat(topLevel.firstCpdToken().text()).isEqualTo("fun");
  }

  @Test
  void testImport() {
    Tree topLevel = converter.parse("import abc");
    assertThat(topLevel.children()).hasSize(1);
    assertThat(topLevel.children().get(0)).isInstanceOf(ImportDeclarationTree.class);
  }

  @Test
  void testPackage() {
    Tree topLevel = converter.parse("package abc");
    assertThat(topLevel.children()).hasSize(1);
    assertThat(topLevel.children().get(0)).isInstanceOf(PackageDeclarationTree.class);
  }

  @Test
  void testBinaryExpression() {
    assertTrees(kotlinStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"))
      .isEquivalentTo(slangStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"));
    assertThat(((BinaryExpressionTree) kotlinStatement("x + 2;")).operatorToken().text()).isEqualTo("+");
  }

  @Test
  void testParenthesisExpression() {
    assertTrees(kotlinStatements("(a && b); (a && b || (c && d)); (!a || !(a && b));"))
      .isEquivalentTo(slangStatements("(a && b); (a && b || (c && d)); (!a || !(a && b));"));
    ParenthesizedExpressionTree kotlinParenthesisExpression = (ParenthesizedExpressionTree) kotlinStatement("(a && b);");
    assertThat(kotlinParenthesisExpression.leftParenthesis().text()).isEqualTo("(");
    assertThat(kotlinParenthesisExpression.rightParenthesis().text()).isEqualTo(")");
    assertTree(kotlinParenthesisExpression.expression()).isBinaryExpression(BinaryExpressionTree.Operator.CONDITIONAL_AND);
  }

  @Test
  void testUnmappedBinaryExpression() {
    Tree or3 = kotlinStatement("x or 3");
    Tree or3b = kotlinStatement("x or 3");
    Tree and3 = kotlinStatement("x and 3");
    assertTree(or3).isInstanceOf(NativeTree.class);
    assertTree(and3).isInstanceOf(NativeTree.class);
    assertTree(or3).isEquivalentTo(or3b);
    assertTree(or3).isNotEquivalentTo(and3);
  }

  @Test
  void unaryExpressions() {
    assertTree(kotlinStatement("!x")).isEquivalentTo(kotlinStatement("!x"));
    assertTree(kotlinStatement("!x")).isEquivalentTo(slangStatement("!x;"));
    assertTree(kotlinStatement("!!x")).isEquivalentTo(slangStatement("!!x;"));
    assertTree(kotlinStatement("++x")).isEquivalentTo(slangStatement("++x;"));
    assertTree(kotlinStatement("--x")).isEquivalentTo(slangStatement("--x;"));
    assertTree(kotlinStatement("+1")).isEquivalentTo(kotlinStatement("+1"));
    assertTree(kotlinStatement("+1")).isNotEquivalentTo(kotlinStatement("+2"));
    assertTree(kotlinStatement("+1")).isNotEquivalentTo(kotlinStatement("-1"));
    assertTree(kotlinStatement("++x")).isNotEquivalentTo(kotlinStatement("--x"));
  }

  @Test
  void isExpressions() {
    assertTree(kotlinStatement("a is b")).isEquivalentTo(kotlinStatement("a is b"));
    assertTree(kotlinStatement("a !is b")).isEquivalentTo(kotlinStatement("a !is b"));
    assertTree(kotlinStatement("a !is b")).isNotEquivalentTo(kotlinStatement("a is b"));
    assertTree(kotlinStatement("a is b")).isNotEquivalentTo(kotlinStatement("a is c"));
  }

  @Test
  void testNullParameterNames() {
    // In the following case, the '(a, b)' part is not a parameter with a name, but a 'KtDestructuringDeclaration'
    assertTree(kotlinStatement("for ((a, b) in container) {a}"))
      .isNotEquivalentTo(kotlinStatement("for ((b, a) in container) {a}"));
    assertTree(kotlinStatement("for ((a, b) in container) {a}"))
      .isEquivalentTo(kotlinStatement("for ((a, b) in container) {a}"));
  }

  @Test
  void testVariableDeclaration() {
    Tree varX = kotlinStatement("var x : Int");
    Tree valY = kotlinStatement("val y : Int");
    assertTree(varX).isInstanceOf(VariableDeclarationTree.class);
    assertTree(valY).isInstanceOf(VariableDeclarationTree.class);
    assertTree(((VariableDeclarationTree) varX).identifier()).isIdentifier("x");
    assertThat(((VariableDeclarationTree) varX).isVal()).isFalse();
    assertTree(((VariableDeclarationTree) valY).identifier()).isIdentifier("y");
    assertThat(((VariableDeclarationTree) valY).isVal()).isTrue();
    assertTree(varX).isEquivalentTo(kotlinStatement("var x: Int"));
    assertTree(varX).isNotEquivalentTo(kotlinStatement("var y: Int"));
    assertTree(varX).isNotEquivalentTo(kotlinStatement("val x: Int"));
    assertTree(varX).isNotEquivalentTo(kotlinStatement("var x: Boolean"));
  }

  @Test
  void testVariableDeclarationWithInitializer() {
    Tree varX = kotlinStatement("\nvar x : Int = 0");
    Tree valY = kotlinStatement("\nval x : Int = \"4\"");
    assertTree(varX).isInstanceOf(VariableDeclarationTree.class);
    assertTree(valY).isInstanceOf(VariableDeclarationTree.class);
    assertThat(((VariableDeclarationTree) varX).initializer()).isInstanceOf(LiteralTree.class);
    assertThat(((VariableDeclarationTree) valY).initializer()).isInstanceOf(StringLiteralTree.class);
    assertTree(varX).isEquivalentTo(kotlinStatement("var x : Int = 0"));
    assertTree(varX).isNotEquivalentTo(valY);
    assertTree(varX).isNotEquivalentTo(kotlinStatement("var x: Int"));
    assertTree(varX).isNotEquivalentTo(kotlinStatement("var x: Boolean = true"));
    assertTree(((VariableDeclarationTree) varX).identifier()).hasTextRange(2, 4, 2, 5);
  }

  @Test
  void testClassWithBody() {
    Tree treeA = kotlin("class A { private fun function(a : Int): Boolean { true; }}");
    assertTree(treeA).isInstanceOf(ClassDeclarationTree.class);
    ClassDeclarationTree classA = (ClassDeclarationTree) treeA;
    assertTree(classA.identifier()).isIdentifier("A");
    assertThat(classA.children()).hasSize(1);
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun function(a : Int): Boolean { true; false; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun function(a : Int): Boolean { false; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun function(a : Int): Int { true; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun function(a : Boolean): Boolean { true; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun function(b : Int): Boolean { true; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun foo(a : Int): Boolean { true; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { public fun function(a : Int): Boolean { true; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class B { private fun function(a : Int): Boolean { true; }}"));
    assertTree(treeA).isNotEquivalentTo(kotlin("class A { val x: Int; private fun function(a : Int): Boolean { true; }}"));
  }

  @Test
  void testClassWithTwoAnnotation() {
    Tree twoAnnotations = kotlin("@my.test.MyAnnotation(\"something\")\n" +
      "@MyAnnotation2\n" +
      "class A {}");

    List<Annotation> annotations = twoAnnotations.metaData().annotations();
    assertThat(annotations).hasSize(2);
    Annotation firstAnnotation = annotations.get(0);
    assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(firstAnnotation.argumentsText()).containsExactly("\"something\"");
    Annotation secondAnnotation = annotations.get(1);
    assertThat(secondAnnotation.shortName()).isEqualTo("MyAnnotation2");
    assertThat(secondAnnotation.argumentsText()).isEmpty();
  }

  @Test
  void testClassWithComplexAnnotation() {
    Tree twoAnnotations = kotlin("@my.test.MyAnnotation(value = \"something\", \"somethingElse\", otherValue = [\"a\", \"b\"])\n" +
      "class A {}");

    List<Annotation> annotations = twoAnnotations.metaData().annotations();
    assertThat(annotations).hasSize(1);
    Annotation firstAnnotation = annotations.get(0);
    assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(firstAnnotation.argumentsText()).containsExactly("value = \"something\"", "\"somethingElse\"", "otherValue = [\"a\", \"b\"]");
  }

  @Test
  void testClassWithAnnotatedMember() {
    Tree tree = kotlin("class A {\n" +
      "@MyAnnotation\n" +
      "fun f(@MyAnnotation i: Int){ }" +
      "}\n");

    assertThat(tree.metaData().annotations()).isEmpty();

    List<Tree> annotatedDescendants = tree.descendants().filter(d -> !d.metaData().annotations().isEmpty()).collect(Collectors.toList());
    assertThat(annotatedDescendants).hasSize(2);
    annotatedDescendants.forEach(descendant -> {
      List<Annotation> annotations = descendant.metaData().annotations();
      assertThat(annotations).hasSize(1);
      Annotation annotation = annotations.get(0);
      assertThat(annotation.shortName()).isEqualTo("MyAnnotation");
      assertThat(annotation.argumentsText()).isEmpty();
    });
  }

  @Test
  void testClassWithoutBody() {
    Tree tree = kotlin("class A {}");
    assertTree(tree).isInstanceOf(ClassDeclarationTree.class);
    ClassDeclarationTree classA = (ClassDeclarationTree) tree;
    assertTree(classA.identifier()).isIdentifier("A");
    assertThat(classA.descendants().anyMatch(IdentifierTree.class::isInstance)).isTrue();
    assertRange(classA.identifier().textRange()).hasRange(1, 6, 1, 7);
    assertTree(tree).isEquivalentTo(kotlin("class A {}"));
    assertTree(tree).isNotEquivalentTo(kotlin("class A constructor(){}"));
    assertTree(tree).isNotEquivalentTo(kotlin("class B {}"));
  }

  @Test
  void testEnumClassEntries() {
    Tree tree = kotlin("enum class A { B, C, D }");
    assertTree(tree).isInstanceOf(ClassDeclarationTree.class);
    assertThat(tree.descendants().noneMatch(ClassDeclarationTree.class::isInstance)).isTrue();
  }

  @Test
  void testNestedClasses() {
    Tree tree = kotlin("class A { class B { class C {} } }");
    assertTree(tree).isInstanceOf(ClassDeclarationTree.class);
    assertThat(tree.descendants().filter(ClassDeclarationTree.class::isInstance).count()).isEqualTo(2);
  }

  @Test
  void testFunctionDeclaration() {
    FunctionDeclarationTree functionDeclarationTree = ((FunctionDeclarationTree) kotlin("private fun function1(a: Int, b: String): Boolean { true; }"));
    assertTree(functionDeclarationTree.name()).isIdentifier("function1").hasTextRange(1, 12, 1, 21);
    assertThat(functionDeclarationTree.modifiers()).hasSize(1);
    assertTree(functionDeclarationTree.returnType()).isIdentifier("Boolean");
    assertThat(functionDeclarationTree.formalParameters()).hasSize(2);
    assertTree(functionDeclarationTree).hasParameterNames("a", "b");
    assertTree(functionDeclarationTree.body()).isBlock(LiteralTree.class);

    FunctionDeclarationTree functionWithInternalModifier = (FunctionDeclarationTree) kotlin("internal fun function1(a: Int, c: String): Boolean = true");
    assertTree(functionWithInternalModifier.body()).isNotNull();
    assertThat(functionWithInternalModifier.modifiers()).hasSize(1);
    assertTree(functionWithInternalModifier).hasParameterNames("a", "c");
    assertTree(functionWithInternalModifier).isNotEquivalentTo(functionDeclarationTree);

    FunctionDeclarationTree functionWithProtectedModifier = (FunctionDeclarationTree) kotlin("protected fun function1(a: Int, c: String): Boolean = true");
    assertThat(functionWithProtectedModifier.modifiers()).hasSize(1);
    assertTree(functionWithProtectedModifier).isNotEquivalentTo(functionDeclarationTree);

    FunctionDeclarationTree functionWithOverride = (FunctionDeclarationTree) kotlin("override fun function2() {}");
    ModifierTree overriddenModifier = (ModifierTree) functionWithOverride.modifiers().get(0);
    assertThat(overriddenModifier.kind()).isEqualTo(OVERRIDE);

    FunctionDeclarationTree functionWithPrivate = (FunctionDeclarationTree) kotlin("private fun function2() {}");
    assertThat(functionWithPrivate.formalParameters()).isEmpty();
    Tree privateModifier = functionDeclarationTree.modifiers().get(0);
    Tree internalModifier = functionWithInternalModifier.modifiers().get(0);
    Tree protectedModifier = functionWithProtectedModifier.modifiers().get(0);
    assertTree(privateModifier).isNotEquivalentTo(internalModifier);
    assertTree(privateModifier).isEquivalentTo(functionWithPrivate.modifiers().get(0));
    assertTree(privateModifier).isEquivalentTo(new ModifierTreeImpl(null, PRIVATE));
    assertTree(privateModifier).isNotEquivalentTo(new ModifierTreeImpl(null, PUBLIC));
    assertTree(internalModifier).isNotEquivalentTo(new ModifierTreeImpl(null, PRIVATE));
    assertTree(internalModifier).isNotEquivalentTo(new ModifierTreeImpl(null, PUBLIC));
    assertTree(internalModifier).isNotEquivalentTo(protectedModifier);

    FunctionDeclarationTree noModifierFunction = (FunctionDeclarationTree) kotlin("fun function1(a: Int = 3, a: String) {}");
    assertThat(noModifierFunction.modifiers()).isEmpty();
    assertTree(noModifierFunction.returnType()).isNull();
    assertThat(noModifierFunction.formalParameters()).hasSize(2);
    assertThat(noModifierFunction.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
    assertTree(noModifierFunction.body()).isBlock();

    FunctionDeclarationTree emptyLambdaFunction = (FunctionDeclarationTree) kotlinStatement("{ }").children().get(0);
    assertTree(emptyLambdaFunction.name()).isNull();
    assertThat(emptyLambdaFunction.modifiers()).isEmpty();
    assertTree(emptyLambdaFunction.returnType()).isNull();
    assertThat(emptyLambdaFunction.formalParameters()).isEmpty();
    assertThat(emptyLambdaFunction.body()).isNull();

    ParameterTree aIntParam1 = (ParameterTree) functionDeclarationTree.formalParameters().get(0);
    Tree bStringParam = functionDeclarationTree.formalParameters().get(1);
    Tree aIntParam2 = functionWithInternalModifier.formalParameters().get(0);
    Tree aIntParamWithInitializer = noModifierFunction.formalParameters().get(0);
    Tree aStringParam = noModifierFunction.formalParameters().get(1);
    assertTree(aIntParam1).isNotEquivalentTo(bStringParam);
    assertTree(aIntParam1).isEquivalentTo(aIntParam2);
    assertTree(aIntParam1).isNotEquivalentTo(aStringParam);
    assertTree(aIntParam1).isNotEquivalentTo(aIntParamWithInitializer);
    assertTree(aStringParam).isNotEquivalentTo(bStringParam);
    assertTree(aIntParam1).hasTextRange(1, 22, 1, 28);
    assertTree(aIntParam1.identifier()).hasTextRange(1, 22, 1, 23);
    assertTree(aStringParam).isInstanceOf(ParameterTree.class);
    assertTree(aIntParamWithInitializer).hasTextRange(1, 14, 1, 24);
  }

  @Test
  void testFunctionDeclarationWithDefaultValue() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) kotlin(
      "fun function1(p1: Int = 1, p2: String, p3: String = \"def\") {}");

    assertThat(func.formalParameters()).hasSize(3);
    assertTree(func).hasParameterNames("p1", "p2", "p3");
    ParameterTree p1 = (ParameterTree) func.formalParameters().get(0);
    ParameterTree p2 = (ParameterTree) func.formalParameters().get(1);
    ParameterTree p3 = (ParameterTree) func.formalParameters().get(2);
    assertTree(p1.defaultValue()).isLiteral("1");
    assertTree(p2.defaultValue()).isNull();
    assertTree(p3.defaultValue()).isLiteral("\"def\"");
  }

  @Test
  void testExtensionFunction() {
    assertTree(kotlin("fun A.fun1() {}"))
      .isNotEquivalentTo(kotlin("fun B.fun1() {}"));
    assertTree(kotlin("fun A.fun1() {}"))
      .isNotEquivalentTo(kotlin("fun fun1() {}"));
    assertTree(kotlin("fun A.fun1() {}"))
      .isEquivalentTo(kotlin("fun A.fun1() {}"));
    assertTree(kotlin("fun A.fun1() {}"))
      .isNotEquivalentTo(kotlin("class A { fun fun1() {} }"));
  }

  @Test
  void testGenericFunctions() {
    assertTree(kotlin("fun f1() {}"))
      .isNotEquivalentTo(kotlin("fun <A> f1() {}"));
    assertTree(kotlin("fun <A> f1() {}"))
      .isEquivalentTo(kotlin("fun <A> f1() {}"));
    assertTree(kotlin("fun <A, B> f1() {}"))
      .isEquivalentTo(kotlin("fun <A, B> f1() {}"));
    assertTree(kotlin("fun <A, B> f1() {}"))
      .isNotEquivalentTo(kotlin("fun <A, B, C> f1() {}"));
  }

  @Test
  void testUnmappedFunctionModifiers() {
    assertTree(kotlin("fun f1() {}"))
      .isNotEquivalentTo(kotlin("inline fun f1() {}"));
    assertTree(kotlin("tailrec fun f1() {}"))
      .isNotEquivalentTo(kotlin("inline fun f1() {}"));
    assertTree(kotlin("inline fun f1() {}"))
      .isEquivalentTo(kotlin("inline fun f1() {}"));
  }

  @Test
  void testGenericClasses() {
    assertTree(kotlin("class A<T>() {}"))
      .isNotEquivalentTo(kotlin("class A() {}"));
    assertTree(kotlin("class A<T>() {}"))
      .isEquivalentTo(kotlin("class A<T>() {}"));
  }

  @Test
  void testConstructors() {
    assertTree(kotlin("class A() {}"))
      .isEquivalentTo(kotlin("class A constructor() {}"));
    assertTree(kotlin("class A(a: Int = 3) {}"))
      .isEquivalentTo(kotlin("class A constructor(a: Int = 3) {}"));
    assertTree(kotlin("class A(a: Int = 3) {}"))
      .isNotEquivalentTo(kotlin("class A(a: Int) {}"));
    assertTree(kotlin("class A(a: Int) { constructor() {} }"))
      .isEquivalentTo(kotlin("class A(a: Int) { constructor() {} }"));
    assertTree(kotlin("class A(a: Int) { constructor(): this(1) {} }"))
      .isNotEquivalentTo(kotlin("class A(a: Int) { constructor(): this(2) {} }"));
    assertTree(kotlin("class A(a: Int) { constructor(): this(0) {} }"))
      .isEquivalentTo(kotlin("class A(a: Int) { constructor(): this(0) {} }"));
  }

  @Test
  void testFunctionInvocation() {
    Tree tree = kotlinStatement("foo(\"Hello world!\")");
    assertThat(tree).isInstanceOf(NativeTree.class);
  }

  @Test
  void testLiterals() {
    assertTrees(kotlinStatements("554; true; false; null; \"string\"; 'c';"))
      .isEquivalentTo(slangStatements("554; true; false; null; \"string\"; 'c';"));
  }

  @Test
  void testSimpleStringLiterals() {
    assertTree(kotlinStatement(createEscapedString('\\'))).isStringLiteral(createEscaped('\\'));
    assertTree(kotlinStatement(createEscapedString('\''))).isStringLiteral(createEscaped('\''));
    assertTree(kotlinStatement(createEscapedString('\"'))).isStringLiteral(createEscaped('\"'));
    assertTree(kotlinStatement(createString(""))).isStringLiteral("");
  }

  @Test
  void testStringWithIdentifier() {
    assertTree(kotlinStatement("\"identifier ${x}\"")).isInstanceOf(NativeTree.class).hasChildren(NativeTree.class, NativeTree.class);
    assertTree(kotlinStatement("\"identifier ${x}\"")).isEquivalentTo(kotlinStatement("\"identifier ${x}\""));
    assertTree(kotlinStatement("\"identifier ${x}\"")).isNotEquivalentTo(kotlinStatement("\"identifier ${y}\""));
    assertTree(kotlinStatement("\"identifier ${x}\"")).isNotEquivalentTo(kotlinStatement("\"id ${x}\""));
    assertTree(kotlinStatement("\"identifier ${x}\"")).isNotEquivalentTo(kotlinStatement("\"identifier \""));
    assertTree(kotlinStatement("\"identifier ${x}\"").children().get(0)).isNotEquivalentTo(kotlinStatement("\"identifier \""));
  }

  @Test
  void testStringWithBlock() {
    Tree stringWithBlock = kotlinStatement("\"block ${1 == 1}\"");
    assertTree(stringWithBlock).isInstanceOf(NativeTree.class).hasChildren(NativeTree.class, NativeTree.class);
    Tree blockExpressionContainer = stringWithBlock.children().get(1);
    assertTree(blockExpressionContainer).isInstanceOf(NativeTree.class);
    assertThat(blockExpressionContainer.children()).hasSize(1);
    assertTree(blockExpressionContainer.children().get(0)).isBinaryExpression(BinaryExpressionTree.Operator.EQUAL_TO);

    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isEquivalentTo(kotlinStatement("\"block ${1 == 1}\""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"block ${1 == 0}\""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"B ${1 == 1}\""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"block \""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"").children().get(0)).isNotEquivalentTo(kotlinStatement("\"block \""));
  }

  @Test
  void testMultilineString() {
    assertTree(kotlinStatement("\"\"\"first\nsecond line\"\"\"")).isStringLiteral("\"\"first\nsecond line\"\"");
  }

  @Test
  void testRange() {
    FunctionDeclarationTree tree = ((FunctionDeclarationTree) kotlin("fun function1(a: Int, b: String): Boolean\n{ true; }"));
    assertTree(tree).hasTextRange(1, 0, 2, 9);
  }

  @Test
  void testIfExpressions() {
    assertTrees(kotlinStatements("if (x == 0) { 3; x + 2;}"))
      .isEquivalentTo(slangStatements("if (x == 0) { 3; x + 2;};"));

    assertTrees(kotlinStatements("if (x) 1 else 4"))
      .isEquivalentTo(slangStatements("if (x) 1 else 4;"));

    assertTrees(kotlinStatements("if (x) 1 else if (x > 2) 4"))
      .isEquivalentTo(slangStatements("if (x) 1 else if (x > 2) 4;"));

    // In kotlin a null 'then' branch is valid code, so this if will be mapped to a native tree as it is not valid in Slang AST
    NativeTree ifStatementWithNullThenBranch = (NativeTree) kotlinStatement("if (x) else 4");
    assertTrees(Collections.singletonList(ifStatementWithNullThenBranch))
      .isNotEquivalentTo(slangStatements("if (x) { } else 4;"));
    assertTree(ifStatementWithNullThenBranch).hasChildren(IdentifierTree.class, LiteralTree.class);

    NativeTree ifStatementWithNullBranches = (NativeTree) kotlinStatement("if (x) else;");
    assertTrees(Collections.singletonList(ifStatementWithNullBranches))
      .isNotEquivalentTo(slangStatements("if (x) { } else { };"));
    assertTree(ifStatementWithNullBranches).hasChildren(IdentifierTree.class);

    Tree tree = kotlinStatement("if (x) 1 else 4");
    assertTree(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseKeyword().text()).isEqualTo("else");
  }

  @Test
  void testSimpleMatchExpression() {
    Tree kotlinStatement = kotlinStatement("when (x) { 1 -> true; 1 -> false; 2 -> true; else -> true;}");
    assertTree(kotlinStatement).isInstanceOf(MatchTree.class);
    MatchTree matchTree = (MatchTree) kotlinStatement;
    assertTree(matchTree.expression()).isIdentifier("x");
    List<MatchCaseTree> cases = matchTree.cases();
    assertThat(cases).hasSize(4);
    assertTree(getCondition(cases, 0)).isEquivalentTo(getCondition(cases, 1));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 2));
    assertThat(getCondition(cases, 3)).isNull();
    assertThat(matchTree.keyword().text()).isEqualTo("when");
  }

  @Test
  void testComplexMatchExpression() {
    MatchTree complexWhen = (MatchTree) kotlinStatement("" +
      "when (x) { isBig() -> 1;1,2 -> x; in 5..10 -> y; !in 10..20 -> z; is String -> x; 1,2 -> y; }");
    List<MatchCaseTree> cases = complexWhen.cases();
    assertThat(cases).hasSize(6);
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 1));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 2));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 3));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 4));
    assertTree(getCondition(cases, 1)).isEquivalentTo(getCondition(cases, 5));

    Tree emptyWhen = kotlinStatement("when {}");
    assertTree(emptyWhen).isInstanceOf(MatchTree.class);
    MatchTree emptyMatchTree = (MatchTree) emptyWhen;
    assertTree(emptyMatchTree).hasChildren(0);
    assertTree(emptyMatchTree.expression()).isNull();
    assertThat(emptyMatchTree.cases()).isEmpty();
    assertTree(emptyMatchTree).isEquivalentTo(kotlinStatement("when {}"));
    assertTree(emptyMatchTree).isNotEquivalentTo(kotlinStatement("when (x) {}"));
    assertTree(emptyMatchTree).isNotEquivalentTo(kotlinStatement("when {1 -> true}"));
  }

  @Test
  void testForLoop() {
    Tree kotlinStatement = kotlinStatement("for (item : Int in ints) { x = item; x = x + 1; }");
    assertTree(kotlinStatement).isInstanceOf(LoopTree.class);
    LoopTree forLoop = (LoopTree) kotlinStatement;
    assertTree(forLoop.condition()).isInstanceOf(NativeTree.class);
    assertThat(forLoop.condition().children()).hasSize(2);
    assertTree(forLoop.condition().children().get(0)).hasParameterName("item");
    assertTree(forLoop.condition().children().get(1)).isIdentifier("ints");
    assertTree(forLoop.body()).isBlock(AssignmentExpressionTree.class, AssignmentExpressionTree.class);
    assertThat(forLoop.kind()).isEqualTo(FOR);
    assertThat(forLoop.keyword().text()).isEqualTo("for");
    assertTree(forLoop).isEquivalentTo(kotlinStatement("for (item : Int in ints) { x = item; x = x + 1; }"));
    assertTree(forLoop).isNotEquivalentTo(kotlinStatement("for (item : String in ints) { x = item; x = x + 1; }"));
    assertTree(forLoop).isNotEquivalentTo(kotlinStatement("for (it : Int in ints) { x = item; x = x + 1; }"));
    assertTree(forLoop).isNotEquivalentTo(kotlinStatement("for (item : Int in floats) { x = item; x = x + 1; }"));
  }

  @Test
  void testWhileLoop() {
    Tree kotlinStatement = kotlinStatement("while (x < j) { item = i; i = i + 1; }");
    assertTree(kotlinStatement).isInstanceOf(LoopTree.class);
    LoopTree whileLoop = (LoopTree) kotlinStatement;
    assertTree(whileLoop.condition()).isBinaryExpression(LESS_THAN);
    assertTree(whileLoop.body()).isBlock(AssignmentExpressionTree.class, AssignmentExpressionTree.class);
    assertThat(whileLoop.kind()).isEqualTo(WHILE);
    assertThat(whileLoop.keyword().text()).isEqualTo("while");
    assertTree(whileLoop).isEquivalentTo(slangStatement("while (x < j) { item = i; i = i + 1; };"));
    assertTree(whileLoop).isEquivalentTo(kotlinStatement("while (x < j) { item = i; i = i + 1; }"));
    assertTree(whileLoop).isNotEquivalentTo(kotlinStatement("while (x < k) { item = i; i = i + 1; }"));
  }

  @Test
  void testDoWhileLoop() {
    Tree kotlinStatement = kotlinStatement("do { item = i; i = i + 1; } while (x < j)");
    assertTree(kotlinStatement).isInstanceOf(LoopTree.class);
    LoopTree doWhileLoop = (LoopTree) kotlinStatement;
    assertTree(doWhileLoop.condition()).isBinaryExpression(LESS_THAN);
    assertTree(doWhileLoop.body()).isBlock(AssignmentExpressionTree.class, AssignmentExpressionTree.class);
    assertThat(doWhileLoop.kind()).isEqualTo(DOWHILE);
    assertThat(doWhileLoop.keyword().text()).isEqualTo("do");
    assertTree(doWhileLoop).isEquivalentTo(kotlinStatement("do { item = i; i = i + 1; } while (x < j)"));
    assertTree(doWhileLoop).isEquivalentTo(slangStatement("do { item = i; i = i + 1; } while (x < j);"));
    assertTree(doWhileLoop).isNotEquivalentTo(kotlinStatement("do { item = i; i = i + 1; } while (x < k)"));
    assertTree(doWhileLoop).isNotEquivalentTo(kotlinStatement("while (x < j) { item = i; i = i + 1; }"));
  }

  @Test
  void testTryCatch() {
    Tree kotlinStatement = kotlinStatement("try { 1 } catch (e: SomeException) { }");
    assertTree(kotlinStatement).isInstanceOf(ExceptionHandlingTree.class);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) kotlinStatement;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    List<CatchTree> catchTreeList = exceptionHandlingTree.catchBlocks();
    assertThat(catchTreeList).hasSize(1);
    assertThat(catchTreeList.get(0).keyword().text()).isEqualTo("catch");
    assertTree(catchTreeList.get(0).catchParameter()).isInstanceOf(ParameterTree.class);
    ParameterTree catchParameter = (ParameterTree) catchTreeList.get(0).catchParameter();
    assertTree(catchParameter).hasParameterName("e");
    assertThat(catchParameter.type()).isNotNull();
    assertTree(catchTreeList.get(0).catchBlock()).isBlock();
    assertThat(exceptionHandlingTree.finallyBlock()).isNull();
  }

  @Test
  void testTryFinally() {
    Tree kotlinStatement = kotlinStatement("try { 1 } finally { 2 }");
    assertTree(kotlinStatement).isInstanceOf(ExceptionHandlingTree.class);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) kotlinStatement;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    List<CatchTree> catchTreeList = exceptionHandlingTree.catchBlocks();
    assertThat(catchTreeList).isEmpty();
    assertThat(exceptionHandlingTree.finallyBlock()).isNotNull();
    assertTree(exceptionHandlingTree.finallyBlock()).isBlock(LiteralTree.class);
  }

  @Test
  void testTryCatchFinally() {
    Tree kotlinStatement = kotlinStatement("try { 1 } catch (e: SomeException) { } catch (e: Exception) { } finally { 2 }");
    assertTree(kotlinStatement).isInstanceOf(ExceptionHandlingTree.class);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) kotlinStatement;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    List<CatchTree> catchTreeList = exceptionHandlingTree.catchBlocks();
    assertThat(catchTreeList).hasSize(2);
    assertTree(catchTreeList.get(0).catchParameter()).isInstanceOf(ParameterTree.class);
    ParameterTree catchParameterOne = (ParameterTree) catchTreeList.get(0).catchParameter();
    assertTree(catchParameterOne).hasParameterName("e");
    assertThat(catchParameterOne.type()).isNotNull();
    assertTree(catchTreeList.get(0).catchBlock()).isBlock();
    assertThat(catchTreeList.get(1).catchParameter()).isNotNull();
    assertTree(catchTreeList.get(1).catchBlock()).isBlock();
    assertThat(exceptionHandlingTree.finallyBlock()).isNotNull();
    assertTree(exceptionHandlingTree.finallyBlock()).isBlock(LiteralTree.class);
  }

  @Test
  void testComments() {
    Tree parent = converter.parse("#! Shebang comment\n/** Doc comment \n*/\nfun function1(a: /* Block comment */Int, b: String): Boolean { // EOL comment\n true; }");
    assertTree(parent).isInstanceOf(TopLevelTree.class);
    assertThat(parent.children()).hasSize(1);

    TopLevelTree topLevelTree = (TopLevelTree) parent;
    List<Comment> comments = topLevelTree.allComments();
    assertThat(comments).hasSize(4);
    Comment comment = comments.get(1);
    assertRange(comment.textRange()).hasRange(2, 0, 3, 2);
    assertRange(comment.contentRange()).hasRange(2, 3, 3, 0);
    assertThat(comment.contentText()).isEqualTo(" Doc comment \n");
    assertThat(comment.text()).isEqualTo("/** Doc comment \n*/");

    FunctionDeclarationTree tree = (FunctionDeclarationTree) topLevelTree.declarations().get(0);
    List<Comment> commentsInsideFunction = tree.metaData().commentsInside();
    // Kotlin doc is considered part of the function
    assertThat(commentsInsideFunction).hasSize(3);
    comment = commentsInsideFunction.get(2);
    assertRange(comment.textRange()).hasRange(4, 63, 4, 77);
    assertRange(comment.contentRange()).hasRange(4, 65, 4, 77);
    assertThat(comment.text()).isEqualTo("// EOL comment");
  }

  @Test
  void testLambdas() {
    Tree lambdaWithDestructor = kotlinStatement("{ (a, b) -> a.length < b.length }");
    Tree lambdaWithoutDestructor = kotlinStatement("{ a, b -> a.length < b.length }");
    assertTree(lambdaWithDestructor).hasChildren(FunctionDeclarationTree.class);
    assertTree(lambdaWithoutDestructor).hasChildren(FunctionDeclarationTree.class);

    FunctionDeclarationTree emptyLambda = (FunctionDeclarationTree) kotlinStatement("{ }").children().get(0);
    assertThat(emptyLambda.body()).isNull();
  }

  @Test
  void testEquivalenceWithComments() {
    assertTrees(kotlinStatements("x + 2; // EOL comment\n"))
      .isEquivalentTo(slangStatements("x + 2;"));
  }

  @Test
  void testMappedComments() {
    TopLevelTree kotlinTree = (TopLevelTree) converter
      .parse("/** 1st comment */\n// comment 2\nfun function() = /* Block comment */ 3;");
    TopLevelTree slangTree = (TopLevelTree) new SLangConverter()
      .parse("/** 1st comment */\n// comment 2\nvoid fun function() { /* Block comment */ 3; }");

    assertThat(kotlinTree.allComments()).hasSize(3);
    assertThat(kotlinTree.allComments()).isNotEqualTo(slangTree.allComments()); // Kotlin considers the '/**' delimiter as separate comments
    List<String> slangCommentsWithDelimiters = slangTree.allComments().stream().map(Comment::text).collect(Collectors.toList());
    assertThat(kotlinTree.allComments()).extracting(Comment::text).isEqualTo(slangCommentsWithDelimiters);
  }

  @Test
  void testAssignments() {
    assertTrees(kotlinStatements("x = 3\nx += y + 3\n"))
      .isEquivalentTo(slangStatements("x = 3; x += y + 3;"));
  }

  @Test
  void testBreakContinue() {
    LoopTree tree = (LoopTree) kotlinStatement("while(true)\nbreak;");
    assertThat(tree.body()).isInstanceOf(JumpTree.class);
    JumpTree jumpTree = (JumpTree) tree.body();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);
    assertThat(jumpTree.keyword().text()).isEqualTo("break");
    assertThat(jumpTree.label()).isNull();

    tree = (LoopTree) kotlinStatement("while(true)\nbreak@foo;");
    assertThat(tree.body()).isInstanceOf(JumpTree.class);
    jumpTree = (JumpTree) tree.body();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);
    assertThat(jumpTree.keyword().text()).isEqualTo("break");
    assertThat(jumpTree.label().name()).isEqualTo("foo");
    assertThat(jumpTree.metaData().tokens()).extracting(Token::text).containsExactly("break", "@", "foo");
    assertThat(jumpTree.label().metaData().tokens()).extracting(Token::text).containsExactly("foo");

    tree = (LoopTree) kotlinStatement("while(true)\ncontinue;");
    assertThat(tree.body()).isInstanceOf(JumpTree.class);
    jumpTree = (JumpTree) tree.body();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);
    assertThat(jumpTree.keyword().text()).isEqualTo("continue");
    assertThat(jumpTree.label()).isNull();

    tree = (LoopTree) kotlinStatement("while(true)\ncontinue@foo;");
    assertThat(tree.body()).isInstanceOf(JumpTree.class);
    jumpTree = (JumpTree) tree.body();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);
    assertThat(jumpTree.keyword().text()).isEqualTo("continue");
    assertThat(jumpTree.label().name()).isEqualTo("foo");
    assertThat(jumpTree.metaData().tokens()).extracting(Token::text).containsExactly("continue", "@", "foo");
    assertThat(jumpTree.label().metaData().tokens()).extracting(Token::text).containsExactly("foo");

    assertTrees(kotlinStatements("while(true)\nbreak;"))
      .isEquivalentTo(slangStatements("while(true)\nbreak;"));

    assertTrees(kotlinStatements("while(true)\ncontinue;"))
      .isEquivalentTo(slangStatements("while(true)\ncontinue;"));

    assertTrees(kotlinStatements("while(true)\nbreak@foo;"))
      .isEquivalentTo(slangStatements("while(true)\nbreak foo;"));

    assertTrees(kotlinStatements("while(true)\ncontinue@foo;"))
      .isEquivalentTo(slangStatements("while(true)\ncontinue foo;"));

  }

  @Test
  void testReturn() {
    Tree tree = kotlinStatement("return 2;");
    assertThat(tree).isInstanceOf(ReturnTree.class);
    ReturnTree returnTree = (ReturnTree) tree;
    assertThat(returnTree.keyword().text()).isEqualTo("return");
    assertThat(returnTree.body()).isInstanceOf(LiteralTree.class);

    tree = kotlinStatement("return;");
    assertThat(tree).isInstanceOf(ReturnTree.class);
    returnTree = (ReturnTree) tree;
    assertThat(returnTree.keyword().text()).isEqualTo("return");
    assertThat(returnTree.body()).isNull();

    tree = kotlinStatement("return@foo 2;");
    assertThat(tree).isInstanceOf(NativeTree.class);

    assertTree(kotlinStatement("return 2;"))
      .isEquivalentTo(slangStatement("return 2;"));

    assertTree(kotlinStatement("return;"))
      .isEquivalentTo(slangStatement("return;"));

    assertTree(kotlinStatement("return@foo;"))
      .isNotEquivalentTo(slangStatement("return;"));

    assertTree(kotlinStatement("return@foo;"))
      .isNotEquivalentTo(kotlinStatement("return@bar;"));
  }

  @Test
  void testThrow() {
    Tree tree = kotlinStatement("throw Exception();");
    assertThat(tree).isInstanceOf(ThrowTree.class);
    ThrowTree throwTree = (ThrowTree) tree;
    assertThat(throwTree.keyword().text()).isEqualTo("throw");
    assertTree(throwTree.body()).isInstanceOf(NativeTree.class);
    assertTree(throwTree.body()).isEquivalentTo(kotlinStatement("Exception();"));
  }


  @Test
  void testTokens() {
    List<Token> tokens = kotlin("private fun foo() { 42 + \"a\" }").metaData().tokens();
    assertThat(tokens).extracting(Token::text).containsExactly(
      "private", "fun", "foo", "(", ")", "{", "42", "+", "\"", "a", "\"", "}");
    assertThat(tokens).extracting(Token::type).containsExactly(
      KEYWORD, KEYWORD, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, STRING_LITERAL, OTHER, OTHER);
  }

  @Test
  void testIntegerLiterals() {
    IntegerLiteralTree literal0 = (IntegerLiteralTree) kotlinStatement("0Xaa");
    assertTree(literal0).isLiteral("0Xaa");
    assertThat(literal0.getBase()).isEqualTo(HEXADECIMAL);
    assertThat(literal0.getIntegerValue().intValue()).isEqualTo(170);
    assertThat(literal0.getNumericPart()).isEqualTo("aa");

    IntegerLiteralTree literal2 = (IntegerLiteralTree) kotlinStatement("123");
    assertTree(literal2).isLiteral("123");
    assertThat(literal2.getBase()).isEqualTo(DECIMAL);
    assertThat(literal2.getIntegerValue().intValue()).isEqualTo(123);
    assertThat(literal2.getNumericPart()).isEqualTo("123");

    IntegerLiteralTree literal3 = (IntegerLiteralTree) kotlinStatement("0b101");
    assertTree(literal3).isLiteral("0b101");
    assertThat(literal3.getBase()).isEqualTo(BINARY);
    assertThat(literal3.getIntegerValue().intValue()).isEqualTo(5);
    assertThat(literal3.getNumericPart()).isEqualTo("101");
  }

  private static String createString(String s) {
    return "\"" + s + "\"";
  }

  private static String createEscaped(char s) {
    return "\\" + s;
  }

  private static String createEscapedString(char s) {
    return createString(createEscaped(s));
  }

  private static Tree getCondition(List<MatchCaseTree> cases, int i) {
    return cases.get(i).expression();
  }

  private Tree slangStatement(String innerCode) {
    List<Tree> slangStatements = slangStatements(innerCode);
    assertThat(slangStatements).hasSize(1);
    return slangStatements.get(0);
  }

  private List<Tree> slangStatements(String innerCode) {
    Tree tree = new SLangConverter().parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    return tree.children();
  }

  private Tree kotlinStatement(String innerCode) {
    List<Tree> kotlinStatements = kotlinStatements(innerCode);
    assertThat(kotlinStatements).hasSize(1);
    return kotlinStatements.get(0);
  }

  private Tree kotlin(String innerCode) {
    Tree tree = converter.parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    assertThat(tree.children()).hasSize(1);
    return tree.children().get(0);
  }

  private List<Tree> kotlinStatements(String innerCode) {
    FunctionDeclarationTree functionDeclarationTree = (FunctionDeclarationTree) kotlin("fun function1() { " + innerCode + " }");
    assertThat(functionDeclarationTree.body()).isNotNull();
    return functionDeclarationTree.body().statementOrExpressions();
  }
}
