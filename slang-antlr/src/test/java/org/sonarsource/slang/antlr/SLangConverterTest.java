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
package org.sonarsource.slang.antlr;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.ImportDeclarationTree;
import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.MemberSelectTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.PackageDeclarationTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.PlaceHolderTree;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.GREATER_THAN;
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

class SLangConverterTest {

  private SLangConverter converter = new SLangConverter();

  @Test
  void top_level_block() {
    Tree tree = converter.parse("{ 2; };").children().get(0);
    assertTree(tree).isBlock(LiteralTree.class);
  }

  @Test
  void first_cpd_token() {
    TopLevelTree tree = (TopLevelTree) converter.parse("package abc; import x; import y; 42;");
    assertThat(tree.firstCpdToken().text()).isEqualTo("42");
  }

  @Test
  void package_declaration() {
    Tree tree = converter.parse("package abc;").children().get(0);
    assertThat(tree).isInstanceOf(PackageDeclarationTree.class);
    assertThat(tree.children()).hasSize(1);
    assertTree(tree.children().get(0)).isIdentifier("abc");
  }

  @Test
  void import_declaration() {
    Tree tree = converter.parse("import abc;").children().get(0);
    assertThat(tree).isInstanceOf(ImportDeclarationTree.class);
    assertThat(tree.children()).hasSize(1);
    assertTree(tree.children().get(0)).isIdentifier("abc");
  }

  @Test
  void simple_binary_expression() {
    BinaryExpressionTree binary = parseBinary("x + 1;");
    assertTree(binary).isBinaryExpression(Operator.PLUS).hasTextRange(1, 0, 1, 5);
    assertTree(binary.leftOperand()).isIdentifier("x").hasTextRange(1, 0, 1, 1);
    assertTree(binary.rightOperand()).isLiteral("1").hasTextRange(1, 4, 1, 5);
    assertThat(binary.operatorToken().text()).isEqualTo("+");
  }

  @Test
  void simple_unary_expression() {
    BinaryExpressionTree binary = parseBinary("!!x && !(y && z);");
    Tree left = binary.leftOperand();
    Tree right = binary.rightOperand();

    assertTree(left).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);
    assertTree(right).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);

    UnaryExpressionTree unaryLeft = (UnaryExpressionTree) left;
    UnaryExpressionTree unaryRight = (UnaryExpressionTree) right;

    assertTree(unaryLeft.operand()).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);
    assertTree(unaryRight.operand()).isInstanceOf(ParenthesizedExpressionTree.class);
    ParenthesizedExpressionTree parenthesizedExpression = (ParenthesizedExpressionTree) unaryRight.operand();
    assertTree(parenthesizedExpression.expression()).isBinaryExpression(Operator.CONDITIONAL_AND);
  }

  @Test
  void other_unary_expression() {
    assertTree(parseUnary("+ 2;")).isUnaryExpression(UnaryExpressionTree.Operator.PLUS);
    assertTree(parseUnary("+ +2;")).isUnaryExpression(UnaryExpressionTree.Operator.PLUS);
    assertTree(parseUnary("- 2;")).isUnaryExpression(UnaryExpressionTree.Operator.MINUS);
    assertTree(parseUnary("++2;")).isUnaryExpression(UnaryExpressionTree.Operator.INCREMENT);
    assertTree(parseUnary("-- 2;")).isUnaryExpression(UnaryExpressionTree.Operator.DECREMENT);
  }

  @Test
  void parenthesized_expression() {
    BinaryExpressionTree binary = parseBinary("((a && b) && (c || d)) || (y\n|| z);");
    assertTree(binary.leftOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    assertTree(binary.rightOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    ParenthesizedExpressionTree left = (ParenthesizedExpressionTree) binary.leftOperand();
    ParenthesizedExpressionTree right = (ParenthesizedExpressionTree) binary.rightOperand();

    assertTree(left).hasTextRange(1, 0, 1, 22);
    assertTree(left.expression()).isBinaryExpression(Operator.CONDITIONAL_AND);
    BinaryExpressionTree innerBinary = (BinaryExpressionTree) left.expression();
    assertTree(innerBinary.leftOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    assertTree(innerBinary.rightOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    assertTree(right).hasTextRange(1, 26, 2, 5);
    assertTree(right.expression()).isBinaryExpression(Operator.CONDITIONAL_OR);
  }

  @Test
  void conditional_and_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x && y && z;");
    assertTree(binary).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertRange(binary.operatorToken().textRange()).hasRange(1, 2, 1, 4);
  }

  @Test
  void additive_expression_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x + y\n- z;");
    assertTree(binary).isBinaryExpression(Operator.PLUS);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.MINUS).hasTextRange(1, 4, 2, 3);
  }

  @Test
  void binary_expression_with_place_holder() {
    BinaryExpressionTree binary = parseBinary("_ && y;");
    assertTree(binary).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(binary.leftOperand()).isInstanceOf(PlaceHolderTree.class);
    assertTree(binary.rightOperand()).isIdentifier("y");
  }

  @Test
  void variable_declaration() {
    Tree tree = converter.parse("int var x;").children().get(0);
    Tree valueTree = converter.parse("int val x;").children().get(0);
    Tree anotherTree = converter.parse("int var x;").children().get(0);
    Tree yetAnotherTree = converter.parse("boolean var x;").children().get(0);
    assertThat(tree).isInstanceOf(VariableDeclarationTree.class);

    VariableDeclarationTree varDeclX = (VariableDeclarationTree) tree;

    assertThat(varDeclX.children()).hasSize(2);
    assertTree(varDeclX.type()).isIdentifier("int");
    assertTree(varDeclX.identifier()).isIdentifier("x");
    assertTree(varDeclX).isEquivalentTo(anotherTree);
    assertTree(varDeclX).isNotEquivalentTo(yetAnotherTree);
    assertTree(varDeclX).isNotEquivalentTo(valueTree);
  }

  @Test
  void variable_declaration_annotated() {
    Tree tree = converter.parse("@MyAnnotation\n" +
      "int val x;").children().get(0);
    List<Annotation> annotations = tree.metaData().annotations();
    assertThat(annotations).hasSize(1);
    Annotation annotation = annotations.get(0);
    assertThat(annotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(annotation.argumentsText()).isEmpty();
  }

  @Test
  void variable_declaration_with_initializer() {
    Tree tree = converter.parse("int var x = 0;").children().get(0);
    Tree anotherTree = converter.parse("int val x = 0;").children().get(0);
    assertThat(tree).isInstanceOf(VariableDeclarationTree.class);
    assertThat(anotherTree).isInstanceOf(VariableDeclarationTree.class);

    VariableDeclarationTree varDeclX = (VariableDeclarationTree) tree;
    VariableDeclarationTree valDeclX = (VariableDeclarationTree) anotherTree;

    assertThat(varDeclX.children()).hasSize(3);
    assertTree(varDeclX.type()).isIdentifier("int");
    assertTree(varDeclX.identifier()).isIdentifier("x");
    assertTree(varDeclX.initializer()).isLiteral("0");
    assertThat(varDeclX.isVal()).isFalse();
    assertThat(valDeclX.children()).hasSize(3);
    assertTree(valDeclX.type()).isIdentifier("int");
    assertTree(valDeclX.identifier()).isIdentifier("x");
    assertTree(valDeclX.initializer()).isLiteral("0");
    assertThat(valDeclX.isVal()).isTrue();
    assertTree(varDeclX).isEquivalentTo(converter.parse("int var x = 0;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(valDeclX);
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("myint var x = 0;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("int var x = 1;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("var x = 0;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("var x;").children().get(0));
  }

  @Test
  void class_with_identifier_and_body() {
    ClassDeclarationTree classe = parseClass("class MyClass { int val x; fun foo (x); } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isIdentifier("MyClass");
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(3);
    assertTree(classChildren.children().get(0)).isIdentifier("MyClass");
    assertThat(classChildren.children().get(1)).isInstanceOf(VariableDeclarationTree.class);
    assertTree(classChildren.children().get(2)).isInstanceOf(FunctionDeclarationTree.class);
  }

  @Test
  void class_with_constructor() {
    ClassDeclarationTree classe = parseClass("class MyClass { fun constructor(x) { } fun foo(x) { } } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isIdentifier("MyClass");
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(3);
    assertTree(classChildren.children().get(0)).isIdentifier("MyClass");
    assertTree(classChildren.children().get(1)).isInstanceOf(FunctionDeclarationTree.class);
    assertTree(classChildren.children().get(2)).isInstanceOf(FunctionDeclarationTree.class);
    FunctionDeclarationTree constructorTree = (FunctionDeclarationTree) classChildren.children().get(1);
    assertThat(constructorTree.isConstructor()).isTrue();
    FunctionDeclarationTree functionTree = (FunctionDeclarationTree) classChildren.children().get(2);
    assertThat(functionTree.isConstructor()).isFalse();
  }

  @Test
  void class_without_body() {
    ClassDeclarationTree classe = parseClass("class MyClass { } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isIdentifier("MyClass");
    assertRange(classe.identifier().textRange()).hasRange(1, 6, 1, 13);
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(1);
    assertTree(classChildren.children().get(0)).isIdentifier("MyClass");
  }

  @Test
  void class_without_identifier() {
    ClassDeclarationTree classe = parseClass("class { int val x; } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isNull();
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(1);
    assertThat(classChildren.children().get(0)).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  void class_without_identifier_and_body() {
    ClassDeclarationTree classe = parseClass("class { } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isNull();
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).isEmpty();
  }

  @Test
  void nested_class_in_function() {
    FunctionDeclarationTree func = parseFunction("fun foo() { class { } }");
    assertThat(func.children()).hasSize(2);
    assertTree(func.name()).isIdentifier("foo");
    assertTree(func.body()).isBlock(ClassDeclarationTree.class);
  }

  @Test
  void class_with_annotation() {
    ClassDeclarationTree classe = parseClass("@MyAnnotation(\"abc\") class { } ");
    List<Annotation> annotations = classe.metaData().annotations();
    assertThat(annotations).hasSize(1);
    Annotation annotation = annotations.get(0);
    assertThat(annotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(annotation.argumentsText()).hasSize(1).containsExactly("\"abc\"");
  }

  @Test
  void class_with_annotation_complex_arguments() {
    ClassDeclarationTree classe = parseClass("@MyAnnotation(\"abc\", value =\"b\", value={\"b\",\"c\"}) class { } ");
    List<Annotation> annotations = classe.metaData().annotations();
    assertThat(annotations).hasSize(1);
    Annotation annotation = annotations.get(0);
    assertThat(annotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(annotation.argumentsText()).hasSize(3).containsExactly("\"abc\"", "value=\"b\"", "value={\"b\",\"c\"}");
  }

  @Test
  void class_and_method_with_annotation() {
    ClassDeclarationTree classe = parseClass("@MyAnnotation1   @MyAnnotation2(\"abc\")   class { @MyFunAnnotation(\"cba\") fun foo() { } } ");
    List<Annotation> annotations = classe.metaData().annotations();
    assertThat(annotations).hasSize(2);
    Annotation annotation1 = annotations.get(0);
    assertThat(annotation1.shortName()).isEqualTo("MyAnnotation1");
    assertThat(annotation1.argumentsText()).isEmpty();
    Annotation annotation2 = annotations.get(1);
    assertThat(annotation2.shortName()).isEqualTo("MyAnnotation2");
    assertThat(annotation2.argumentsText()).containsExactly("\"abc\"");

    FunctionDeclarationTree nestedFunction = (FunctionDeclarationTree) classe.children().get(0).children().get(0);
    List<Annotation> functionAnnotations = nestedFunction.metaData().annotations();
    assertThat(functionAnnotations).hasSize(1);
    Annotation myFunAnnotation = functionAnnotations.get(0);
    assertThat(myFunAnnotation.shortName()).isEqualTo("MyFunAnnotation");
    assertThat(myFunAnnotation.argumentsText()).containsExactly("\"cba\"");
  }

  @Test
  void function() {
    FunctionDeclarationTree function = parseFunction("private int fun foo(x1, x2) { x1 + x2 }");
    assertThat(function.name().name()).isEqualTo("foo");
    assertThat(function.modifiers()).hasSize(1);
    assertTree(function.returnType()).isIdentifier("int");
    assertThat(function.formalParameters()).hasSize(2);
    assertTree(function.formalParameters().get(0)).hasParameterName("x1");
    assertThat(function.body()).isNotNull();
    assertThat(function.nativeChildren()).isEmpty();

    FunctionDeclarationTree publicFunction = parseFunction("public int fun foo(p1);");
    assertThat(publicFunction.formalParameters()).hasSize(1);
    assertTree(publicFunction.formalParameters().get(0)).hasParameterName("p1");

    FunctionDeclarationTree emptyParamFunction = parseFunction("private int fun foo();");
    assertThat(emptyParamFunction.formalParameters()).isEmpty();
    assertThat(emptyParamFunction.body()).isNull();

    Tree privateModifier1 = function.modifiers().get(0);
    Tree publicModifier1 = publicFunction.modifiers().get(0);
    Tree privateModifier2 = emptyParamFunction.modifiers().get(0);
    assertTree(privateModifier1).isNotEquivalentTo(publicModifier1);
    assertTree(privateModifier1).isEquivalentTo(privateModifier2);
    assertTree(privateModifier1).isEquivalentTo(new ModifierTreeImpl(null, PRIVATE));
    assertTree(publicModifier1).isEquivalentTo(new ModifierTreeImpl(null, PUBLIC));

    FunctionDeclarationTree simpleFunction = parseFunction("fun foo() {}");
    assertThat(simpleFunction.modifiers()).isEmpty();
    assertThat(simpleFunction.returnType()).isNull();
    assertThat(simpleFunction.body().statementOrExpressions()).isEmpty();

    FunctionDeclarationTree overriddenFunction = parseFunction("override int fun foo();");
    assertThat(overriddenFunction.modifiers()).hasSize(1);
    ModifierTree modifier = (ModifierTree) overriddenFunction.modifiers().get(0);
    assertThat(modifier.kind()).isEqualTo(OVERRIDE);

    FunctionDeclarationTree functWithNativeModifier = parseFunction("native [] {} int fun foo();");
    assertThat(functWithNativeModifier.modifiers()).hasSize(1);
    assertThat(functWithNativeModifier.modifiers().get(0)).isInstanceOf(NativeTree.class);

    FunctionDeclarationTree noNameFunction = parseFunction("fun() {}");
    assertThat(noNameFunction.name()).isNull();

    FunctionDeclarationTree functionWithDefaultParam = parseFunction("fun foo(p1 = 1, p2, p3 = 1 + 3) {}");
    Tree p1 = functionWithDefaultParam.formalParameters().get(0);
    Tree p2 = functionWithDefaultParam.formalParameters().get(1);
    Tree p3 = functionWithDefaultParam.formalParameters().get(2);
    assertTree(p1).hasParameterName("p1");
    assertTree(p2).hasParameterName("p2");
    assertTree(p3).hasParameterName("p3");
    assertTree(((ParameterTree) p1).defaultValue()).isLiteral("1");
    assertTree(((ParameterTree) p2).defaultValue()).isNull();
    assertTree(((ParameterTree) p3).defaultValue()).isBinaryExpression(Operator.PLUS);

    FunctionDeclarationTree functionWithModifier = parseFunction("fun foo(p1, native [] {} p2) {}");
    assertTree(functionWithModifier).hasParameterNames("p1", "p2");
    Tree p1Mod = functionWithModifier.formalParameters().get(0);
    Tree p2Mod = functionWithModifier.formalParameters().get(1);
    assertThat(((ParameterTree) p1Mod).modifiers()).isEmpty();
    assertThat(((ParameterTree) p2Mod).modifiers()).hasSize(1);
    assertThat(((ParameterTree) p2Mod).modifiers().get(0)).isInstanceOf(NativeTree.class);
  }

  @Test
  void function_with_local_annotations() {
    FunctionDeclarationTree function = parseFunction("private int fun foo() {\n" +
      "@MyAnnotation\n" +
      "val i = 1;\n" +
      "}");
    List<Annotation> localVariableAnnotations = function.body().statementOrExpressions().get(0).metaData().annotations();
    assertThat(localVariableAnnotations).hasSize(1);
    Annotation annotation = localVariableAnnotations.get(0);
    assertThat(annotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(annotation.argumentsText()).isEmpty();
  }

  @Test
  void function_with_annotated_parameters() {
    FunctionDeclarationTree function = parseFunction("private int fun foo(int a, @B int b, @C int c, int d) {}");
    List<Tree> parameters = function.formalParameters();

    List<Annotation> bAnnotations = parameters.get(1).metaData().annotations();
    List<Annotation> cAnnotations = parameters.get(2).metaData().annotations();

    assertThat(parameters.get(0).metaData().annotations()).isEmpty();
    assertThat(bAnnotations).hasSize(1);
    assertThat(cAnnotations).hasSize(1);
    assertThat(parameters.get(3).metaData().annotations()).isEmpty();

    Annotation bAnnotation = bAnnotations.get(0);
    assertThat(bAnnotation.shortName()).isEqualTo("B");
    assertThat(bAnnotation.argumentsText()).isEmpty();
    Annotation cAnnotation = cAnnotations.get(0);
    assertThat(cAnnotation.shortName()).isEqualTo("C");
    assertThat(cAnnotation.argumentsText()).isEmpty();
  }

  @Test
  void if_without_else() {
    Tree tree = converter.parse("if (x > 0) { x = 1; };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 21);
    assertTree(ifTree.condition()).isBinaryExpression(GREATER_THAN);
    assertThat(ifTree.elseBranch()).isNull();
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseKeyword()).isNull();
  }

  @Test
  void if_with_else() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else { y };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 33);
    assertTree(ifTree.condition()).isBinaryExpression(GREATER_THAN);
    assertTree(ifTree.thenBranch()).isBlock(BinaryExpressionTree.class).hasTextRange(1, 11, 1, 22);
    assertTree(ifTree.elseBranch()).isBlock(IdentifierTree.class);
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseKeyword().text()).isEqualTo("else");
  }

  @Test
  void if_with_else_if() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else if (x < 1) { y };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree.elseBranch()).isInstanceOf(IfTree.class);
  }

  @Test
  void match() {
    Tree tree = converter.parse("match(x) { 1 -> a; else -> b; };").children().get(0);
    assertTree(tree).isInstanceOf(MatchTree.class).hasTextRange(1, 0, 1, 31);
    MatchTree matchTree = (MatchTree) tree;
    assertTree(matchTree.expression()).isIdentifier("x");
    assertThat(matchTree.cases()).hasSize(2);
    assertTree(matchTree.cases().get(0).expression()).isLiteral("1");
    assertTree(matchTree.cases().get(1).expression()).isNull();
    assertTree(matchTree.cases().get(1)).hasTextRange(1, 19, 1, 29);
    assertThat(matchTree.keyword().text()).isEqualTo("match");
  }

  @Test
  void match_without_expression() {
    Tree tree = converter.parse("match() { 1 -> a; else -> b; };").children().get(0);
    MatchTree matchTree = (MatchTree) tree;
    assertTree(matchTree.expression()).isNull();
  }

  @Test
  void match_case_without_body() {
    Tree tree = converter.parse("match(x) { 1 -> ; 2 -> a; else ->; };").children().get(0);
    MatchTree matchTree = (MatchTree) tree;
    assertTree(matchTree.cases().get(0).body()).isNull();
    assertTree(matchTree.cases().get(1).body()).isNotNull();
    assertTree(matchTree.cases().get(2).body()).isNull();
  }

  @Test
  void for_loop() {
    Tree tree = converter.parse("for (var x = list) { x; };").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 25);
    LoopTree forLoop = (LoopTree) tree;
    assertThat(forLoop.condition()).isNotNull();
    assertThat(forLoop.condition().children()).hasSize(2);
    assertTree(forLoop.body()).isBlock(IdentifierTree.class);
    assertThat(forLoop.kind()).isEqualTo(FOR);
    assertThat(forLoop.keyword().text()).isEqualTo("for");
  }

  @Test
  void for_loop_without_condition() {
    Tree tree = converter.parse("for {};").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 6);
    LoopTree forLoop = (LoopTree) tree;
    assertThat(forLoop.condition()).isNull();
    assertTree(forLoop.body()).isBlock();
    assertThat(forLoop.body().children()).isEmpty();
  }

  @Test
  void while_loop() {
    Tree tree = converter.parse("while (x > y) { x = x-1; };").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 26);
    LoopTree forLoop = (LoopTree) tree;
    assertTree(forLoop.condition()).isBinaryExpression(GREATER_THAN);
    assertTree(forLoop.body()).isBlock(AssignmentExpressionTree.class);
    assertThat(forLoop.kind()).isEqualTo(WHILE);
    assertThat(forLoop.keyword().text()).isEqualTo("while");
  }

  @Test
  void doWhile_loop() {
    Tree tree = converter.parse("do { x = x-1; } while (x > y);").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 29);
    LoopTree forLoop = (LoopTree) tree;
    assertTree(forLoop.condition()).isBinaryExpression(GREATER_THAN);
    assertTree(forLoop.body()).isBlock(AssignmentExpressionTree.class);
    assertThat(forLoop.kind()).isEqualTo(DOWHILE);
    assertThat(forLoop.keyword().text()).isEqualTo("do");
  }

  @Test
  void try_catch_finally() {
    Tree tree = converter.parse("try { 1 } catch (e) {} catch () {} finally {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1, 0, 1, 45);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(2);
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchParameter()).hasParameterName("e");
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchBlock()).isBlock();
    assertTree(exceptionHandlingTree.catchBlocks().get(1).catchParameter()).isNull();
    assertTree(exceptionHandlingTree.finallyBlock()).isBlock();
  }

  @Test
  void try_catch() {
    Tree tree = converter.parse("try { 1 } catch (e) {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1, 0, 1, 22);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(1);
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchParameter()).hasParameterName("e");
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchBlock()).isBlock();
    assertThat(exceptionHandlingTree.catchBlocks().get(0).keyword().text()).isEqualTo("catch");
    assertTree(exceptionHandlingTree.finallyBlock()).isNull();
  }

  @Test
  void try_finally() {
    Tree tree = converter.parse("try { 1 } finally {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1, 0, 1, 20);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).isEmpty();
    assertTree(exceptionHandlingTree.finallyBlock()).isBlock();
  }

  @Test
  void natives() {
    Tree tree = converter.parse("native [] {};").children().get(0);
    assertTree(tree).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 12);

    tree = converter.parse("native [] { [x] } = x;").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertTree(assignment.leftHandSide()).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 17);
  }

  @Test
  void simple_assignment() {
    Tree tree = converter.parse("x = 1;").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL).hasTextRange(1, 0, 1, 5);
  }

  @Test
  void nested_assignments() {
    Tree tree = converter.parse("x += y += 2;").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.PLUS_EQUAL).hasTextRange(1, 0, 1, 11);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertTree(assignment.leftHandSide()).isIdentifier("x");
    assertTree(assignment.statementOrExpression()).isAssignmentExpression(AssignmentExpressionTree.Operator.PLUS_EQUAL).hasTextRange(1, 5, 1, 11);
  }

  @Test
  void top_level_tree() {
    Tree tree1 = converter.parse("int fun foo(p1);\nx == 3;");
    Tree tree2 = converter.parse("x + y\n\n- z;");
    Tree emptyTree = converter.parse("");
    assertTree(tree1)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(FunctionDeclarationTree.class, BinaryExpressionTree.class)
      .hasTextRange(1, 0, 2, 7);
    assertTree(tree2)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(BinaryExpressionTree.class)
      .hasTextRange(1, 0, 3, 4);
    assertTree(emptyTree)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren()
      .hasTextRange(1, 0, 1, 0);
  }

  @Test
  void comments() {
    BinaryExpressionTree binary = parseBinary("/* comment1 */ x /* comment2 */ == // comment3\n1;");
    List<Comment> comments = binary.metaData().commentsInside();
    assertThat(comments).hasSize(2);
    Comment comment = comments.get(0);
    assertRange(comment.textRange()).hasRange(1, 17, 1, 31);
    assertRange(comment.contentRange()).hasRange(1, 19, 1, 29);
    assertThat(comment.text()).isEqualTo("/* comment2 */");
    assertThat(comment.contentText()).isEqualTo(" comment2 ");
    assertThat(comments.get(1).contentText()).isEqualTo(" comment3");
    assertRange(comments.get(1).contentRange()).hasRange(1, 37, 1, 46);
  }

  @Test
  void decimalLiterals() {
    Tree tree = converter.parse("0; 5; 10; 123; 1010; 5554; 12345567;");
    String[] values = {"0", "5", "10", "123", "1010", "5554", "12345567"};

    assertTree(tree).isNotNull();
    assertTree(tree).isInstanceOf(TopLevelTree.class);
    TopLevelTree topLevelTree = (TopLevelTree) tree;
    assertThat(topLevelTree.declarations()).hasSize(7);

    for (int i = 0; i < topLevelTree.declarations().size(); i++) {
      assertTree(topLevelTree.declarations().get(i)).isLiteral(values[i]);
    }
  }

  @Test
  void stringLiterals() {
    List<String> values = Arrays.asList("\"a\"", "\"string\"", "\"string with spaces\"");
    List<String> content = Arrays.asList("a", "string", "string with spaces");

    String slangCode = values.stream().collect(Collectors.joining(";"));
    Tree tree = converter.parse(slangCode + ";");

    assertTree(tree).isNotNull();
    assertTree(tree).isInstanceOf(TopLevelTree.class);
    TopLevelTree topLevelTree = (TopLevelTree) tree;
    assertThat(topLevelTree.declarations()).hasSize(3);

    for (int i = 0; i < topLevelTree.declarations().size(); i++) {
      assertTree(topLevelTree.declarations().get(i)).isLiteral(values.get(i));
      assertTree(topLevelTree.declarations().get(i)).isStringLiteral(content.get(i));
    }
  }

  @Test
  void jump() {
    JumpTree jumpTree = (JumpTree) converter.parse("break foo;").children().get(0);
    assertThat(jumpTree.label().name()).isEqualTo("foo");
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);

    jumpTree = (JumpTree) converter.parse("break;").children().get(0);
    assertThat(jumpTree.label()).isNull();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);

    jumpTree = (JumpTree) converter.parse("continue;").children().get(0);
    assertThat(jumpTree.label()).isNull();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);

    jumpTree = (JumpTree) converter.parse("continue foo;").children().get(0);
    assertThat(jumpTree.label().name()).isEqualTo("foo");
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);
  }

  @Test
  void returnTree() {
    ReturnTree returnTree = (ReturnTree) converter.parse("return true;").children().get(0);
    assertThat(returnTree.body()).isInstanceOf(LiteralTree.class);

    returnTree = (ReturnTree) converter.parse("return;").children().get(0);
    assertThat(returnTree.body()).isNull();
  }

  @Test
  void tokens() {
    Tree topLevel = converter.parse("if (cond == 42) \"a\";");
    IfTree ifTree = (IfTree) topLevel.children().get(0);
    assertThat(topLevel.metaData().tokens()).extracting(Token::text)
      .containsExactly("if", "(", "cond", "==", "42", ")", "\"a\"", ";");
    assertThat(topLevel.metaData().tokens()).extracting(Token::type)
      .containsExactly(KEYWORD, OTHER, OTHER, OTHER, OTHER, OTHER, STRING_LITERAL, OTHER);
    assertRange(topLevel.metaData().tokens().get(1).textRange()).hasRange(1, 3, 1, 4);
    assertThat(ifTree.condition().metaData().tokens()).extracting(Token::text).containsExactly("cond", "==", "42");
  }

  @Test
  void methodInvocations() {
    Tree topLevelNoArgument = converter.parse("function();");
    assertTree(topLevelNoArgument.children().get(0)).isInstanceOf(FunctionInvocationTree.class);
    FunctionInvocationTree functionInvocationNoArgument = (FunctionInvocationTree) topLevelNoArgument.children().get(0);
    assertTree(functionInvocationNoArgument.memberSelect()).isIdentifier("function");
    assertThat(functionInvocationNoArgument.arguments()).isEmpty();

    Tree topLevelTwoArgs = converter.parse("function(1, 2);");
    assertTree(topLevelTwoArgs.children().get(0)).isInstanceOf(FunctionInvocationTree.class);
    FunctionInvocationTree functionInvocationTreeTwoArgs = (FunctionInvocationTree) topLevelTwoArgs.children().get(0);
    assertTree(functionInvocationTreeTwoArgs.memberSelect()).isIdentifier("function");
    assertThat(functionInvocationTreeTwoArgs.arguments()).hasSize(2);
    assertThat(functionInvocationTreeTwoArgs.descendants()
      .anyMatch(e -> e instanceof LiteralTree && ((LiteralTree) e).value().equals("1"))).isTrue();

    assertTree(topLevelNoArgument).isEquivalentTo(topLevelNoArgument);
    assertTree(topLevelNoArgument).isEquivalentTo(converter.parse("function();"));
    assertTree(topLevelNoArgument).isNotEquivalentTo(converter.parse("function2();"));
    assertTree(topLevelNoArgument).isNotEquivalentTo(converter.parse("function(1);"));
    assertTree(topLevelNoArgument).isNotEquivalentTo(topLevelTwoArgs);
    assertTree(converter.parse("function(1);")).isEquivalentTo(converter.parse("function(1);"));
    assertTree(converter.parse("function(1);")).isNotEquivalentTo(topLevelTwoArgs);

    assertThat(functionInvocationNoArgument.descendants()
      .anyMatch(e -> e instanceof IdentifierTree && ((IdentifierTree) e).name().equals("function"))).isTrue();
  }

  @Test
  void memberSelect() {
    Tree topLevelNoArgument = converter.parse("A.B;");
    assertTree(topLevelNoArgument.children().get(0)).isInstanceOf(MemberSelectTree.class);

    MemberSelectTree memberSelectTree = (MemberSelectTree) topLevelNoArgument.children().get(0);
    assertTree(memberSelectTree.identifier()).isIdentifier("B");
    assertTree(memberSelectTree.expression()).isIdentifier("A");
  }

  @Test
  void memberSelectInAssignment() {
    Tree topLevelNoArgument = converter.parse("A.B.F = 1;");
    assertTree(topLevelNoArgument.children().get(0)).isInstanceOf(AssignmentExpressionTree.class);
    AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) topLevelNoArgument.children().get(0);
    assertTree(assignmentExpressionTree.leftHandSide()).isInstanceOf(MemberSelectTree.class);

    MemberSelectTree memberSelectTree = (MemberSelectTree)assignmentExpressionTree.leftHandSide();
    assertTree(memberSelectTree.identifier()).isIdentifier("F");

    assertTree(memberSelectTree.expression()).isInstanceOf(MemberSelectTree.class);
    MemberSelectTree nestedMemberSelect = (MemberSelectTree)memberSelectTree.expression();
    assertTree(nestedMemberSelect.identifier()).isIdentifier("B");
    assertTree(nestedMemberSelect.expression()).isIdentifier("A");
  }

  @Test
  void memberSelectInFunctionCall() {
    Tree topLevelNoArgument = converter.parse("A.B.F();");
    assertTree(topLevelNoArgument.children().get(0)).isInstanceOf(FunctionInvocationTree.class);
    FunctionInvocationTree functionInvocationNoArgument = (FunctionInvocationTree) topLevelNoArgument.children().get(0);
    assertTree(functionInvocationNoArgument.memberSelect()).isInstanceOf(MemberSelectTree.class);

    MemberSelectTree memberSelectTree = (MemberSelectTree)functionInvocationNoArgument.memberSelect();
    assertTree(memberSelectTree.identifier()).isIdentifier("F");

    assertTree(memberSelectTree.expression()).isInstanceOf(MemberSelectTree.class);
    MemberSelectTree nestedMemberSelect = (MemberSelectTree)memberSelectTree.expression();
    assertTree(nestedMemberSelect.identifier()).isIdentifier("B");
    assertTree(nestedMemberSelect.expression()).isIdentifier("A");
  }

  @Test
  void integerLiterals() {
    Tree tree = converter.parse("0252; 0o252; 0O252; 170; 0xaa; 0B10;");
    IntegerLiteralTree literal0 = (IntegerLiteralTree) tree.children().get(0);
    assertTree(literal0).isLiteral("0252");
    assertThat(literal0.getBase()).isEqualTo(IntegerLiteralTree.Base.OCTAL);
    assertThat(literal0.getIntegerValue().intValue()).isEqualTo(170);
    IntegerLiteralTree literal1 = (IntegerLiteralTree) tree.children().get(1);
    assertTree(literal1).isLiteral("0o252");
    assertThat(literal1.getBase()).isEqualTo(IntegerLiteralTree.Base.OCTAL);
    assertThat(literal1.getIntegerValue().intValue()).isEqualTo(170);
    IntegerLiteralTree literal2 = (IntegerLiteralTree) tree.children().get(2);
    assertTree(literal2).isLiteral("0O252");
    assertThat(literal2.getBase()).isEqualTo(IntegerLiteralTree.Base.OCTAL);
    assertThat(literal2.getIntegerValue().intValue()).isEqualTo(170);
    IntegerLiteralTree literal3 = (IntegerLiteralTree) tree.children().get(3);
    assertTree(literal3).isLiteral("170");
    assertThat(literal3.getBase()).isEqualTo(IntegerLiteralTree.Base.DECIMAL);
    assertThat(literal3.getIntegerValue().intValue()).isEqualTo(170);
    IntegerLiteralTree literal4 = (IntegerLiteralTree) tree.children().get(4);
    assertTree(literal4).isLiteral("0xaa");
    assertThat(literal4.getBase()).isEqualTo(IntegerLiteralTree.Base.HEXADECIMAL);
    assertThat(literal4.getIntegerValue().intValue()).isEqualTo(170);
    IntegerLiteralTree literal5 = (IntegerLiteralTree) tree.children().get(5);
    assertTree(literal5).isLiteral("0B10");
    assertThat(literal5.getBase()).isEqualTo(IntegerLiteralTree.Base.BINARY);
    assertThat(literal5.getIntegerValue().intValue()).isEqualTo(2);
  }

  @Test
  void placeholderAssignment() {
    Tree placeholderAssignment = converter.parse("A._=\"xxx\";");
    assertTree(placeholderAssignment.children().get(0)).isInstanceOf(AssignmentExpressionTree.class);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) placeholderAssignment.children().get(0);

    assertTree(assignment.leftHandSide()).isInstanceOf(MemberSelectTree.class);
    MemberSelectTree lhs = (MemberSelectTree) assignment.leftHandSide();

    assertTree(lhs.expression()).isInstanceOf(IdentifierTree.class);
    assertTree(lhs.identifier()).isInstanceOf(PlaceHolderTree.class);
  }

  @Test
  void parse_failure_1() {
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("x + 1"));
    assertThat(e).hasMessage("missing ';' before '<EOF>' at position 1:5");
  }

  @Test
  void parse_failure_2() {
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("private fun fun foo() {}"));
    assertThat(e).hasMessage("Unexpected parsing error occurred. Last found valid token: 'private' at position 1:0");
  }

  private BinaryExpressionTree parseBinary(String code) {
    return (BinaryExpressionTree) parseExpressionOrStatement(code);
  }

  private UnaryExpressionTree parseUnary(String code) {
    return (UnaryExpressionTree) parseExpressionOrStatement(code);
  }

  private Tree parseExpressionOrStatement(String code) {
    Tree tree = converter.parse(code);
    return tree.children().get(0);
  }

  private FunctionDeclarationTree parseFunction(String code) {
    return (FunctionDeclarationTree) converter.parse(code).children().get(0);
  }

  private ClassDeclarationTree parseClass(String code) {
    return (ClassDeclarationTree) converter.parse(code).children().get(0);
  }
}
