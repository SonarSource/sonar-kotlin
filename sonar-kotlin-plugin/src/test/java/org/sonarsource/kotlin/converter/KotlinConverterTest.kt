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
package org.sonarsource.kotlin.converter

import java.util.function.Consumer
import java.util.stream.Collectors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.sonarsource.slang.api.AssignmentExpressionTree
import org.sonarsource.slang.api.BinaryExpressionTree
import org.sonarsource.slang.api.ClassDeclarationTree
import org.sonarsource.slang.api.Comment
import org.sonarsource.slang.api.ExceptionHandlingTree
import org.sonarsource.slang.api.FunctionDeclarationTree
import org.sonarsource.slang.api.IdentifierTree
import org.sonarsource.slang.api.IfTree
import org.sonarsource.slang.api.ImportDeclarationTree
import org.sonarsource.slang.api.IntegerLiteralTree
import org.sonarsource.slang.api.JumpTree
import org.sonarsource.slang.api.LiteralTree
import org.sonarsource.slang.api.LoopTree
import org.sonarsource.slang.api.LoopTree.LoopKind
import org.sonarsource.slang.api.MatchCaseTree
import org.sonarsource.slang.api.MatchTree
import org.sonarsource.slang.api.ModifierTree
import org.sonarsource.slang.api.NativeTree
import org.sonarsource.slang.api.PackageDeclarationTree
import org.sonarsource.slang.api.ParameterTree
import org.sonarsource.slang.api.ParenthesizedExpressionTree
import org.sonarsource.slang.api.ParseException
import org.sonarsource.slang.api.ReturnTree
import org.sonarsource.slang.api.StringLiteralTree
import org.sonarsource.slang.api.ThrowTree
import org.sonarsource.slang.api.Token
import org.sonarsource.slang.api.TopLevelTree
import org.sonarsource.slang.api.Tree
import org.sonarsource.slang.api.VariableDeclarationTree
import org.sonarsource.slang.impl.ModifierTreeImpl
import org.sonarsource.slang.parser.SLangConverter
import org.sonarsource.slang.testing.RangeAssert
import org.sonarsource.slang.testing.TreeAssert
import org.sonarsource.slang.testing.TreesAssert

internal class KotlinConverterTest {
    private val converter = KotlinConverter(emptyList())
    @Test
    fun testParseException() {
        val e = Assertions.assertThrows(
            ParseException::class.java
        ) { converter.parse("enum class A {\n<!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!>}") }
        assertThat(e.message).isEqualTo("Cannot convert file due to syntactic errors")
    }

    @Test
    fun testWinEOL() {
        val tree: Tree = converter.parse(
            """
                  fun main(args: Array<String>) {
                  
                  }
                  
                  """.trimIndent())
        assertThat(tree.children()).hasSize(1)
    }

    @Test
    fun testParsedExceptionWithPartialParsing() {
        val e = Assertions.assertThrows(
            ParseException::class.java
        ) { converter.parse("fun foo() { a b c ...}") }
        assertThat(e.message).isEqualTo("Cannot convert file due to syntactic errors")
    }

    @Test
    fun testParseWithoutNullPointer() {
        val e = Assertions.assertThrows(
            ParseException::class.java
        ) { converter.parse("package \${package}") }
        assertThat(e.message).isEqualTo("Cannot convert file due to syntactic errors")
    }

    @Test
    fun testFirstCpdToken() {
        val topLevel = converter.parse("""
    @file:JvmName("xxx")
    package com.example
    import com.example.MyClass
    fun main(args: Array<String>) {}
    class A {}
    """.trimIndent()) as TopLevelTree
        assertThat(topLevel.declarations()).hasSize(5)
        assertThat(topLevel.firstCpdToken()!!.text()).isEqualTo("fun")
    }

    @Test
    fun testImport() {
        val topLevel: Tree = converter.parse("import abc")
        assertThat(topLevel.children()).hasSize(1)
        assertThat(topLevel.children()[0]).isInstanceOf(
            ImportDeclarationTree::class.java)
    }

    @Test
    fun testPackage() {
        val topLevel: Tree = converter.parse("package abc")
        assertThat(topLevel.children()).hasSize(1)
        assertThat(topLevel.children()[0]).isInstanceOf(
            PackageDeclarationTree::class.java)
    }

    @Test
    fun testBinaryExpression() {
        TreesAssert.assertTrees(kotlinStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"))
            .isEquivalentTo(slangStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"))
        assertThat((kotlinStatement("x + 2;") as BinaryExpressionTree).operatorToken()
            .text()).isEqualTo("+")
    }

    @Test
    fun testParenthesisExpression() {
        TreesAssert.assertTrees(kotlinStatements("(a && b); (a && b || (c && d)); (!a || !(a && b));"))
            .isEquivalentTo(slangStatements("(a && b); (a && b || (c && d)); (!a || !(a && b));"))
        val kotlinParenthesisExpression = kotlinStatement("(a && b);") as ParenthesizedExpressionTree
        assertThat(kotlinParenthesisExpression.leftParenthesis().text()).isEqualTo("(")
        assertThat(kotlinParenthesisExpression.rightParenthesis().text()).isEqualTo(")")
        TreeAssert.assertTree(kotlinParenthesisExpression.expression())
            .isBinaryExpression(BinaryExpressionTree.Operator.CONDITIONAL_AND)
    }

    @Test
    fun testUnmappedBinaryExpression() {
        val or3 = kotlinStatement("x or 3")
        val or3b = kotlinStatement("x or 3")
        val and3 = kotlinStatement("x and 3")
        TreeAssert.assertTree(or3).isInstanceOf(NativeTree::class.java)
        TreeAssert.assertTree(and3).isInstanceOf(NativeTree::class.java)
        TreeAssert.assertTree(or3).isEquivalentTo(or3b)
        TreeAssert.assertTree(or3).isNotEquivalentTo(and3)
    }

    @Test
    fun unaryExpressions() {
        TreeAssert.assertTree(kotlinStatement("!x")).isEquivalentTo(kotlinStatement("!x"))
        TreeAssert.assertTree(kotlinStatement("!x")).isEquivalentTo(slangStatement("!x;"))
        TreeAssert.assertTree(kotlinStatement("!!x")).isEquivalentTo(slangStatement("!!x;"))
        TreeAssert.assertTree(kotlinStatement("++x")).isEquivalentTo(slangStatement("++x;"))
        TreeAssert.assertTree(kotlinStatement("--x")).isEquivalentTo(slangStatement("--x;"))
        TreeAssert.assertTree(kotlinStatement("+1")).isEquivalentTo(kotlinStatement("+1"))
        TreeAssert.assertTree(kotlinStatement("+1")).isNotEquivalentTo(kotlinStatement("+2"))
        TreeAssert.assertTree(kotlinStatement("+1")).isNotEquivalentTo(kotlinStatement("-1"))
        TreeAssert.assertTree(kotlinStatement("++x")).isNotEquivalentTo(kotlinStatement("--x"))
    }

    @Test
    fun isExpressions() {
        TreeAssert.assertTree(kotlinStatement("a is b")).isEquivalentTo(kotlinStatement("a is b"))
        TreeAssert.assertTree(kotlinStatement("a !is b")).isEquivalentTo(kotlinStatement("a !is b"))
        TreeAssert.assertTree(kotlinStatement("a !is b")).isNotEquivalentTo(kotlinStatement("a is b"))
        TreeAssert.assertTree(kotlinStatement("a is b")).isNotEquivalentTo(kotlinStatement("a is c"))
    }

    @Test
    fun testNullParameterNames() {
        // In the following case, the '(a, b)' part is not a parameter with a name, but a 'KtDestructuringDeclaration'
        TreeAssert.assertTree(kotlinStatement("for ((a, b) in container) {a}"))
            .isNotEquivalentTo(kotlinStatement("for ((b, a) in container) {a}"))
        TreeAssert.assertTree(kotlinStatement("for ((a, b) in container) {a}"))
            .isEquivalentTo(kotlinStatement("for ((a, b) in container) {a}"))
    }

    @Test
    fun testVariableDeclaration() {
        val varX = kotlinStatement("var x : Int")
        val valY = kotlinStatement("val y : Int")
        TreeAssert.assertTree(varX).isInstanceOf(VariableDeclarationTree::class.java)
        TreeAssert.assertTree(valY).isInstanceOf(VariableDeclarationTree::class.java)
        TreeAssert.assertTree((varX as VariableDeclarationTree).identifier()).isIdentifier("x")
        assertThat(varX.isVal).isFalse
        TreeAssert.assertTree((valY as VariableDeclarationTree).identifier()).isIdentifier("y")
        assertThat(valY.isVal).isTrue
        TreeAssert.assertTree(varX).isEquivalentTo(kotlinStatement("var x: Int"))
        TreeAssert.assertTree(varX).isNotEquivalentTo(kotlinStatement("var y: Int"))
        TreeAssert.assertTree(varX).isNotEquivalentTo(kotlinStatement("val x: Int"))
        TreeAssert.assertTree(varX).isNotEquivalentTo(kotlinStatement("var x: Boolean"))
    }

    @Test
    fun testVariableDeclarationWithInitializer() {
        val varX = kotlinStatement("\nvar x : Int = 0")
        val valY = kotlinStatement("\nval x : Int = \"4\"")
        TreeAssert.assertTree(varX).isInstanceOf(VariableDeclarationTree::class.java)
        TreeAssert.assertTree(valY).isInstanceOf(VariableDeclarationTree::class.java)
        assertThat((varX as VariableDeclarationTree).initializer()).isInstanceOf(
            LiteralTree::class.java)
        assertThat((valY as VariableDeclarationTree).initializer()).isInstanceOf(
            StringLiteralTree::class.java)
        TreeAssert.assertTree(varX).isEquivalentTo(kotlinStatement("var x : Int = 0"))
        TreeAssert.assertTree(varX).isNotEquivalentTo(valY)
        TreeAssert.assertTree(varX).isNotEquivalentTo(kotlinStatement("var x: Int"))
        TreeAssert.assertTree(varX).isNotEquivalentTo(kotlinStatement("var x: Boolean = true"))
        TreeAssert.assertTree(varX.identifier()).hasTextRange(2, 4, 2, 5)
    }

    @Test
    fun testClassWithBody() {
        val treeA = kotlin("class A { private fun function(a : Int): Boolean { true; }}")
        TreeAssert.assertTree(treeA).isInstanceOf(ClassDeclarationTree::class.java)
        val classA = treeA as ClassDeclarationTree
        TreeAssert.assertTree(classA.identifier()).isIdentifier("A")
        assertThat(classA.children()).hasSize(1)
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { private fun function(a : Int): Boolean { true; false; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { private fun function(a : Int): Boolean { false; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { private fun function(a : Int): Int { true; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { private fun function(a : Boolean): Boolean { true; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { private fun function(b : Int): Boolean { true; }}"))
        TreeAssert.assertTree(treeA).isNotEquivalentTo(kotlin("class A { private fun foo(a : Int): Boolean { true; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { public fun function(a : Int): Boolean { true; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class B { private fun function(a : Int): Boolean { true; }}"))
        TreeAssert.assertTree(treeA)
            .isNotEquivalentTo(kotlin("class A { val x: Int; private fun function(a : Int): Boolean { true; }}"))
    }

    @Test
    fun testClassWithTwoAnnotation() {
        val twoAnnotations = kotlin("""
    @my.test.MyAnnotation("something")
    @MyAnnotation2
    class A {}
    """.trimIndent())
        val annotations = twoAnnotations.metaData().annotations()
        assertThat(annotations).hasSize(2)
        val firstAnnotation = annotations[0]
        assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation")
        assertThat(firstAnnotation.argumentsText()).containsExactly("\"something\"")
        val secondAnnotation = annotations[1]
        assertThat(secondAnnotation.shortName()).isEqualTo("MyAnnotation2")
        assertThat(secondAnnotation.argumentsText()).isEmpty()
    }

    @Test
    fun testClassWithComplexAnnotation() {
        val twoAnnotations = kotlin("""
    @my.test.MyAnnotation(value = "something", "somethingElse", otherValue = ["a", "b"])
    class A {}
    """.trimIndent())
        val annotations = twoAnnotations.metaData().annotations()
        assertThat(annotations).hasSize(1)
        val firstAnnotation = annotations[0]
        assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation")
        assertThat(firstAnnotation.argumentsText())
            .containsExactly("value = \"something\"", "\"somethingElse\"", "otherValue = [\"a\", \"b\"]")
    }

    @Test
    fun testClassWithAnnotatedMember() {
        val tree = kotlin("""
    class A {
    @MyAnnotation
    fun f(@MyAnnotation i: Int){ }}
    
    """.trimIndent())
        assertThat(tree.metaData().annotations()).isEmpty()
        val annotatedDescendants = tree.descendants().filter { d: Tree -> !d.metaData().annotations().isEmpty() }
            .collect(Collectors.toList())
        assertThat(annotatedDescendants).hasSize(2)
        annotatedDescendants.forEach(Consumer { descendant: Tree ->
            val annotations = descendant.metaData().annotations()
            assertThat(annotations).hasSize(1)
            val annotation = annotations[0]
            assertThat(annotation.shortName()).isEqualTo("MyAnnotation")
            assertThat(annotation.argumentsText()).isEmpty()
        })
    }

    @Test
    fun testClassWithoutBody() {
        val tree = kotlin("class A {}")
        TreeAssert.assertTree(tree).isInstanceOf(ClassDeclarationTree::class.java)
        val classA = tree as ClassDeclarationTree
        TreeAssert.assertTree(classA.identifier()).isIdentifier("A")
        assertThat(classA.descendants()
            .anyMatch { obj: Tree? -> IdentifierTree::class.java.isInstance(obj) }).isTrue
        RangeAssert.assertRange(classA.identifier()!!.textRange()).hasRange(1, 6, 1, 7)
        TreeAssert.assertTree(tree).isEquivalentTo(kotlin("class A {}"))
        TreeAssert.assertTree(tree).isNotEquivalentTo(kotlin("class A constructor(){}"))
        TreeAssert.assertTree(tree).isNotEquivalentTo(kotlin("class B {}"))
    }

    @Test
    fun testEnumClassEntries() {
        val tree = kotlin("enum class A { B, C, D }")
        TreeAssert.assertTree(tree).isInstanceOf(ClassDeclarationTree::class.java)
        assertThat(tree.descendants()
            .noneMatch { obj: Tree? -> ClassDeclarationTree::class.java.isInstance(obj) }).isTrue
    }

    @Test
    fun testNestedClasses() {
        val tree = kotlin("class A { class B { class C {} } }")
        TreeAssert.assertTree(tree).isInstanceOf(ClassDeclarationTree::class.java)
        assertThat(tree.descendants()
            .filter { obj: Tree? -> ClassDeclarationTree::class.java.isInstance(obj) }
            .count()).isEqualTo(2)
    }

    @Test
    fun testFunctionDeclaration() {
        val functionDeclarationTree =
            kotlin("private fun function1(a: Int, b: String): Boolean { true; }") as FunctionDeclarationTree
        TreeAssert.assertTree(functionDeclarationTree.name()).isIdentifier("function1").hasTextRange(1, 12, 1, 21)
        assertThat(functionDeclarationTree.modifiers()).hasSize(1)
        TreeAssert.assertTree(functionDeclarationTree.returnType()).isIdentifier("Boolean")
        assertThat(functionDeclarationTree.formalParameters()).hasSize(2)
        TreeAssert.assertTree(functionDeclarationTree).hasParameterNames("a", "b")
        TreeAssert.assertTree(functionDeclarationTree.body()).isBlock(LiteralTree::class.java)
        val functionWithInternalModifier =
            kotlin("internal fun function1(a: Int, c: String): Boolean = true") as FunctionDeclarationTree
        TreeAssert.assertTree(functionWithInternalModifier.body()).isNotNull
        assertThat(functionWithInternalModifier.modifiers()).hasSize(1)
        TreeAssert.assertTree(functionWithInternalModifier).hasParameterNames("a", "c")
        TreeAssert.assertTree(functionWithInternalModifier).isNotEquivalentTo(functionDeclarationTree)
        val functionWithProtectedModifier =
            kotlin("protected fun function1(a: Int, c: String): Boolean = true") as FunctionDeclarationTree
        assertThat(functionWithProtectedModifier.modifiers()).hasSize(1)
        TreeAssert.assertTree(functionWithProtectedModifier).isNotEquivalentTo(functionDeclarationTree)
        val functionWithOverride = kotlin("override fun function2() {}") as FunctionDeclarationTree
        val overriddenModifier = functionWithOverride.modifiers()[0] as ModifierTree
        assertThat(overriddenModifier.kind()).isEqualTo(ModifierTree.Kind.OVERRIDE)
        val functionWithPrivate = kotlin("private fun function2() {}") as FunctionDeclarationTree
        assertThat(functionWithPrivate.formalParameters()).isEmpty()
        val privateModifier = functionDeclarationTree.modifiers()[0]
        val internalModifier = functionWithInternalModifier.modifiers()[0]
        val protectedModifier = functionWithProtectedModifier.modifiers()[0]
        TreeAssert.assertTree(privateModifier).isNotEquivalentTo(internalModifier)
        TreeAssert.assertTree(privateModifier).isEquivalentTo(functionWithPrivate.modifiers()[0])
        TreeAssert.assertTree(privateModifier).isEquivalentTo(ModifierTreeImpl(null, ModifierTree.Kind.PRIVATE))
        TreeAssert.assertTree(privateModifier).isNotEquivalentTo(ModifierTreeImpl(null, ModifierTree.Kind.PUBLIC))
        TreeAssert.assertTree(internalModifier).isNotEquivalentTo(ModifierTreeImpl(null, ModifierTree.Kind.PRIVATE))
        TreeAssert.assertTree(internalModifier).isNotEquivalentTo(ModifierTreeImpl(null, ModifierTree.Kind.PUBLIC))
        TreeAssert.assertTree(internalModifier).isNotEquivalentTo(protectedModifier)
        val noModifierFunction = kotlin("fun function1(a: Int = 3, a: String) {}") as FunctionDeclarationTree
        assertThat(noModifierFunction.modifiers()).isEmpty()
        TreeAssert.assertTree(noModifierFunction.returnType()).isNull()
        assertThat(noModifierFunction.formalParameters()).hasSize(2)
        assertThat(noModifierFunction.formalParameters()[0]).isInstanceOf(
            ParameterTree::class.java)
        TreeAssert.assertTree(noModifierFunction.body()).isBlock()
        val emptyLambdaFunction = kotlinStatement("{ }").children()[0] as FunctionDeclarationTree
        TreeAssert.assertTree(emptyLambdaFunction.name()).isNull()
        assertThat(emptyLambdaFunction.modifiers()).isEmpty()
        TreeAssert.assertTree(emptyLambdaFunction.returnType()).isNull()
        assertThat(emptyLambdaFunction.formalParameters()).isEmpty()
        assertThat(emptyLambdaFunction.body()).isNull()
        val aIntParam1 = functionDeclarationTree.formalParameters()[0] as ParameterTree
        val bStringParam = functionDeclarationTree.formalParameters()[1]
        val aIntParam2 = functionWithInternalModifier.formalParameters()[0]
        val aIntParamWithInitializer = noModifierFunction.formalParameters()[0]
        val aStringParam = noModifierFunction.formalParameters()[1]
        TreeAssert.assertTree(aIntParam1).isNotEquivalentTo(bStringParam)
        TreeAssert.assertTree(aIntParam1).isEquivalentTo(aIntParam2)
        TreeAssert.assertTree(aIntParam1).isNotEquivalentTo(aStringParam)
        TreeAssert.assertTree(aIntParam1).isNotEquivalentTo(aIntParamWithInitializer)
        TreeAssert.assertTree(aStringParam).isNotEquivalentTo(bStringParam)
        TreeAssert.assertTree(aIntParam1).hasTextRange(1, 22, 1, 28)
        TreeAssert.assertTree(aIntParam1.identifier()).hasTextRange(1, 22, 1, 23)
        TreeAssert.assertTree(aStringParam).isInstanceOf(ParameterTree::class.java)
        TreeAssert.assertTree(aIntParamWithInitializer).hasTextRange(1, 14, 1, 24)
    }

    @Test
    fun testFunctionDeclarationWithDefaultValue() {
        val func = kotlin(
            "fun function1(p1: Int = 1, p2: String, p3: String = \"def\") {}") as FunctionDeclarationTree
        assertThat(func.formalParameters()).hasSize(3)
        TreeAssert.assertTree(func).hasParameterNames("p1", "p2", "p3")
        val p1 = func.formalParameters()[0] as ParameterTree
        val p2 = func.formalParameters()[1] as ParameterTree
        val p3 = func.formalParameters()[2] as ParameterTree
        TreeAssert.assertTree(p1.defaultValue()).isLiteral("1")
        TreeAssert.assertTree(p2.defaultValue()).isNull()
        TreeAssert.assertTree(p3.defaultValue()).isLiteral("\"def\"")
    }

    @Test
    fun testExtensionFunction() {
        TreeAssert.assertTree(kotlin("fun A.fun1() {}"))
            .isNotEquivalentTo(kotlin("fun B.fun1() {}"))
        TreeAssert.assertTree(kotlin("fun A.fun1() {}"))
            .isNotEquivalentTo(kotlin("fun fun1() {}"))
        TreeAssert.assertTree(kotlin("fun A.fun1() {}"))
            .isEquivalentTo(kotlin("fun A.fun1() {}"))
        TreeAssert.assertTree(kotlin("fun A.fun1() {}"))
            .isNotEquivalentTo(kotlin("class A { fun fun1() {} }"))
    }

    @Test
    fun testGenericFunctions() {
        TreeAssert.assertTree(kotlin("fun f1() {}"))
            .isNotEquivalentTo(kotlin("fun <A> f1() {}"))
        TreeAssert.assertTree(kotlin("fun <A> f1() {}"))
            .isEquivalentTo(kotlin("fun <A> f1() {}"))
        TreeAssert.assertTree(kotlin("fun <A, B> f1() {}"))
            .isEquivalentTo(kotlin("fun <A, B> f1() {}"))
        TreeAssert.assertTree(kotlin("fun <A, B> f1() {}"))
            .isNotEquivalentTo(kotlin("fun <A, B, C> f1() {}"))
    }

    @Test
    fun testUnmappedFunctionModifiers() {
        TreeAssert.assertTree(kotlin("fun f1() {}"))
            .isNotEquivalentTo(kotlin("inline fun f1() {}"))
        TreeAssert.assertTree(kotlin("tailrec fun f1() {}"))
            .isNotEquivalentTo(kotlin("inline fun f1() {}"))
        TreeAssert.assertTree(kotlin("inline fun f1() {}"))
            .isEquivalentTo(kotlin("inline fun f1() {}"))
    }

    @Test
    fun testGenericClasses() {
        TreeAssert.assertTree(kotlin("class A<T>() {}"))
            .isNotEquivalentTo(kotlin("class A() {}"))
        TreeAssert.assertTree(kotlin("class A<T>() {}"))
            .isEquivalentTo(kotlin("class A<T>() {}"))
    }

    @Test
    fun testConstructors() {
        TreeAssert.assertTree(kotlin("class A() {}"))
            .isEquivalentTo(kotlin("class A constructor() {}"))
        TreeAssert.assertTree(kotlin("class A(a: Int = 3) {}"))
            .isEquivalentTo(kotlin("class A constructor(a: Int = 3) {}"))
        TreeAssert.assertTree(kotlin("class A(a: Int = 3) {}"))
            .isNotEquivalentTo(kotlin("class A(a: Int) {}"))
        TreeAssert.assertTree(kotlin("class A(a: Int) { constructor() {} }"))
            .isEquivalentTo(kotlin("class A(a: Int) { constructor() {} }"))
        TreeAssert.assertTree(kotlin("class A(a: Int) { constructor(): this(1) {} }"))
            .isNotEquivalentTo(kotlin("class A(a: Int) { constructor(): this(2) {} }"))
        TreeAssert.assertTree(kotlin("class A(a: Int) { constructor(): this(0) {} }"))
            .isEquivalentTo(kotlin("class A(a: Int) { constructor(): this(0) {} }"))
    }

    @Test
    fun testFunctionInvocation() {
        val tree = kotlinStatement("foo(\"Hello world!\")")
        assertThat(tree).isInstanceOf(NativeTree::class.java)
    }

    @Test
    fun testLiterals() {
        TreesAssert.assertTrees(kotlinStatements("554; true; false; null; \"string\"; 'c';"))
            .isEquivalentTo(slangStatements("554; true; false; null; \"string\"; 'c';"))
    }

    @Test
    fun testSimpleStringLiterals() {
        TreeAssert.assertTree(kotlinStatement(createEscapedString('\\'))).isStringLiteral(createEscaped('\\'))
        TreeAssert.assertTree(kotlinStatement(createEscapedString('\''))).isStringLiteral(createEscaped('\''))
        TreeAssert.assertTree(kotlinStatement(createEscapedString('\"'))).isStringLiteral(createEscaped('\"'))
        TreeAssert.assertTree(kotlinStatement(createString(""))).isStringLiteral("")
    }

    @Test
    fun testStringWithIdentifier() {
        TreeAssert.assertTree(kotlinStatement("\"identifier \${x}\"")).isInstanceOf(NativeTree::class.java).hasChildren(
            NativeTree::class.java, NativeTree::class.java)
        TreeAssert.assertTree(kotlinStatement("\"identifier \${x}\""))
            .isEquivalentTo(kotlinStatement("\"identifier \${x}\""))
        TreeAssert.assertTree(kotlinStatement("\"identifier \${x}\""))
            .isNotEquivalentTo(kotlinStatement("\"identifier \${y}\""))
        TreeAssert.assertTree(kotlinStatement("\"identifier \${x}\""))
            .isNotEquivalentTo(kotlinStatement("\"id \${x}\""))
        TreeAssert.assertTree(kotlinStatement("\"identifier \${x}\""))
            .isNotEquivalentTo(kotlinStatement("\"identifier \""))
        TreeAssert.assertTree(kotlinStatement("\"identifier \${x}\"").children()[0])
            .isNotEquivalentTo(kotlinStatement("\"identifier \""))
    }

    @Test
    fun testStringWithBlock() {
        val stringWithBlock = kotlinStatement("\"block \${1 == 1}\"")
        TreeAssert.assertTree(stringWithBlock).isInstanceOf(NativeTree::class.java)
            .hasChildren(NativeTree::class.java, NativeTree::class.java)
        val blockExpressionContainer = stringWithBlock.children()[1]
        TreeAssert.assertTree(blockExpressionContainer).isInstanceOf(NativeTree::class.java)
        assertThat(blockExpressionContainer.children()).hasSize(1)
        TreeAssert.assertTree(blockExpressionContainer.children()[0])
            .isBinaryExpression(BinaryExpressionTree.Operator.EQUAL_TO)
        TreeAssert.assertTree(kotlinStatement("\"block \${1 == 1}\""))
            .isEquivalentTo(kotlinStatement("\"block \${1 == 1}\""))
        TreeAssert.assertTree(kotlinStatement("\"block \${1 == 1}\""))
            .isNotEquivalentTo(kotlinStatement("\"block \${1 == 0}\""))
        TreeAssert.assertTree(kotlinStatement("\"block \${1 == 1}\""))
            .isNotEquivalentTo(kotlinStatement("\"B \${1 == 1}\""))
        TreeAssert.assertTree(kotlinStatement("\"block \${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"block \""))
        TreeAssert.assertTree(kotlinStatement("\"block \${1 == 1}\"").children()[0])
            .isNotEquivalentTo(kotlinStatement("\"block \""))
    }

    @Test
    fun testMultilineString() {
        TreeAssert.assertTree(kotlinStatement("\"\"\"first\nsecond line\"\"\""))
            .isStringLiteral("\"\"first\nsecond line\"\"")
    }

    @Test
    fun testRange() {
        val tree = kotlin("fun function1(a: Int, b: String): Boolean\n{ true; }") as FunctionDeclarationTree
        TreeAssert.assertTree(tree).hasTextRange(1, 0, 2, 9)
    }

    @Test
    fun testIfExpressions() {
        TreesAssert.assertTrees(kotlinStatements("if (x == 0) { 3; x + 2;}"))
            .isEquivalentTo(slangStatements("if (x == 0) { 3; x + 2;};"))
        TreesAssert.assertTrees(kotlinStatements("if (x) 1 else 4"))
            .isEquivalentTo(slangStatements("if (x) 1 else 4;"))
        TreesAssert.assertTrees(kotlinStatements("if (x) 1 else if (x > 2) 4"))
            .isEquivalentTo(slangStatements("if (x) 1 else if (x > 2) 4;"))

        // In kotlin a null 'then' branch is valid code, so this if will be mapped to a native tree as it is not valid in Slang AST
        val ifStatementWithNullThenBranch = kotlinStatement("if (x) else 4") as NativeTree
        TreesAssert.assertTrees(listOf(ifStatementWithNullThenBranch))
            .isNotEquivalentTo(slangStatements("if (x) { } else 4;"))
        TreeAssert.assertTree(ifStatementWithNullThenBranch)
            .hasChildren(IdentifierTree::class.java, LiteralTree::class.java)
        val ifStatementWithNullBranches = kotlinStatement("if (x) else;") as NativeTree
        TreesAssert.assertTrees(listOf(ifStatementWithNullBranches))
            .isNotEquivalentTo(slangStatements("if (x) { } else { };"))
        TreeAssert.assertTree(ifStatementWithNullBranches).hasChildren(IdentifierTree::class.java)
        val tree = kotlinStatement("if (x) 1 else 4")
        TreeAssert.assertTree(tree).isInstanceOf(IfTree::class.java)
        val ifTree = tree as IfTree
        assertThat(ifTree.ifKeyword().text()).isEqualTo("if")
        assertThat(ifTree.elseKeyword()!!.text()).isEqualTo("else")
    }

    @Test
    fun testSimpleMatchExpression() {
        val kotlinStatement = kotlinStatement("when (x) { 1 -> true; 1 -> false; 2 -> true; else -> true;}")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(MatchTree::class.java)
        val matchTree = kotlinStatement as MatchTree
        TreeAssert.assertTree(matchTree.expression()).isIdentifier("x")
        val cases = matchTree.cases()
        assertThat(cases).hasSize(4)
        TreeAssert.assertTree(getCondition(cases, 0)).isEquivalentTo(getCondition(cases, 1))
        TreeAssert.assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 2))
        assertThat(getCondition(cases, 3)).isNull()
        assertThat(matchTree.keyword().text()).isEqualTo("when")
    }

    @Test
    fun testComplexMatchExpression() {
        val complexWhen = kotlinStatement("" +
            "when (x) { isBig() -> 1;1,2 -> x; in 5..10 -> y; !in 10..20 -> z; is String -> x; 1,2 -> y; }") as MatchTree
        val cases = complexWhen.cases()
        assertThat(cases).hasSize(6)
        TreeAssert.assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 1))
        TreeAssert.assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 2))
        TreeAssert.assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 3))
        TreeAssert.assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 4))
        TreeAssert.assertTree(getCondition(cases, 1)).isEquivalentTo(getCondition(cases, 5))
        val emptyWhen = kotlinStatement("when {}")
        TreeAssert.assertTree(emptyWhen).isInstanceOf(MatchTree::class.java)
        val emptyMatchTree = emptyWhen as MatchTree
        TreeAssert.assertTree(emptyMatchTree).hasChildren(0)
        TreeAssert.assertTree(emptyMatchTree.expression()).isNull()
        assertThat(emptyMatchTree.cases()).isEmpty()
        TreeAssert.assertTree(emptyMatchTree).isEquivalentTo(kotlinStatement("when {}"))
        TreeAssert.assertTree(emptyMatchTree).isNotEquivalentTo(kotlinStatement("when (x) {}"))
        TreeAssert.assertTree(emptyMatchTree).isNotEquivalentTo(kotlinStatement("when {1 -> true}"))
    }

    @Test
    fun testForLoop() {
        val kotlinStatement = kotlinStatement("for (item : Int in ints) { x = item; x = x + 1; }")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(LoopTree::class.java)
        val forLoop = kotlinStatement as LoopTree
        TreeAssert.assertTree(forLoop.condition()).isInstanceOf(NativeTree::class.java)
        assertThat(forLoop.condition()!!.children()).hasSize(2)
        TreeAssert.assertTree(forLoop.condition()!!.children()[0]).hasParameterName("item")
        TreeAssert.assertTree(forLoop.condition()!!.children()[1]).isIdentifier("ints")
        TreeAssert.assertTree(forLoop.body())
            .isBlock(AssignmentExpressionTree::class.java, AssignmentExpressionTree::class.java)
        assertThat(forLoop.kind()).isEqualTo(LoopKind.FOR)
        assertThat(forLoop.keyword().text()).isEqualTo("for")
        TreeAssert.assertTree(forLoop)
            .isEquivalentTo(kotlinStatement("for (item : Int in ints) { x = item; x = x + 1; }"))
        TreeAssert.assertTree(forLoop)
            .isNotEquivalentTo(kotlinStatement("for (item : String in ints) { x = item; x = x + 1; }"))
        TreeAssert.assertTree(forLoop)
            .isNotEquivalentTo(kotlinStatement("for (it : Int in ints) { x = item; x = x + 1; }"))
        TreeAssert.assertTree(forLoop)
            .isNotEquivalentTo(kotlinStatement("for (item : Int in floats) { x = item; x = x + 1; }"))
    }

    @Test
    fun testWhileLoop() {
        val kotlinStatement = kotlinStatement("while (x < j) { item = i; i = i + 1; }")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(LoopTree::class.java)
        val whileLoop = kotlinStatement as LoopTree
        TreeAssert.assertTree(whileLoop.condition()).isBinaryExpression(BinaryExpressionTree.Operator.LESS_THAN)
        TreeAssert.assertTree(whileLoop.body())
            .isBlock(AssignmentExpressionTree::class.java, AssignmentExpressionTree::class.java)
        assertThat(whileLoop.kind()).isEqualTo(LoopKind.WHILE)
        assertThat(whileLoop.keyword().text()).isEqualTo("while")
        TreeAssert.assertTree(whileLoop).isEquivalentTo(slangStatement("while (x < j) { item = i; i = i + 1; };"))
        TreeAssert.assertTree(whileLoop).isEquivalentTo(kotlinStatement("while (x < j) { item = i; i = i + 1; }"))
        TreeAssert.assertTree(whileLoop).isNotEquivalentTo(kotlinStatement("while (x < k) { item = i; i = i + 1; }"))
    }

    @Test
    fun testDoWhileLoop() {
        val kotlinStatement = kotlinStatement("do { item = i; i = i + 1; } while (x < j)")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(LoopTree::class.java)
        val doWhileLoop = kotlinStatement as LoopTree
        TreeAssert.assertTree(doWhileLoop.condition()).isBinaryExpression(BinaryExpressionTree.Operator.LESS_THAN)
        TreeAssert.assertTree(doWhileLoop.body())
            .isBlock(AssignmentExpressionTree::class.java, AssignmentExpressionTree::class.java)
        assertThat(doWhileLoop.kind()).isEqualTo(LoopKind.DOWHILE)
        assertThat(doWhileLoop.keyword().text()).isEqualTo("do")
        TreeAssert.assertTree(doWhileLoop).isEquivalentTo(kotlinStatement("do { item = i; i = i + 1; } while (x < j)"))
        TreeAssert.assertTree(doWhileLoop).isEquivalentTo(slangStatement("do { item = i; i = i + 1; } while (x < j);"))
        TreeAssert.assertTree(doWhileLoop)
            .isNotEquivalentTo(kotlinStatement("do { item = i; i = i + 1; } while (x < k)"))
        TreeAssert.assertTree(doWhileLoop).isNotEquivalentTo(kotlinStatement("while (x < j) { item = i; i = i + 1; }"))
    }

    @Test
    fun testTryCatch() {
        val kotlinStatement = kotlinStatement("try { 1 } catch (e: SomeException) { }")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(ExceptionHandlingTree::class.java)
        val exceptionHandlingTree = kotlinStatement as ExceptionHandlingTree
        TreeAssert.assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree::class.java)
        val catchTreeList = exceptionHandlingTree.catchBlocks()
        assertThat(catchTreeList).hasSize(1)
        assertThat(catchTreeList[0].keyword().text()).isEqualTo("catch")
        TreeAssert.assertTree(catchTreeList[0].catchParameter()).isInstanceOf(ParameterTree::class.java)
        val catchParameter = catchTreeList[0].catchParameter() as ParameterTree?
        TreeAssert.assertTree(catchParameter).hasParameterName("e")
        assertThat(catchParameter!!.type()).isNotNull
        TreeAssert.assertTree(catchTreeList[0].catchBlock()).isBlock()
        assertThat(exceptionHandlingTree.finallyBlock()).isNull()
    }

    @Test
    fun testTryFinally() {
        val kotlinStatement = kotlinStatement("try { 1 } finally { 2 }")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(ExceptionHandlingTree::class.java)
        val exceptionHandlingTree = kotlinStatement as ExceptionHandlingTree
        TreeAssert.assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree::class.java)
        val catchTreeList = exceptionHandlingTree.catchBlocks()
        assertThat(catchTreeList).isEmpty()
        assertThat(exceptionHandlingTree.finallyBlock()).isNotNull
        TreeAssert.assertTree(exceptionHandlingTree.finallyBlock()).isBlock(LiteralTree::class.java)
    }

    @Test
    fun testTryCatchFinally() {
        val kotlinStatement =
            kotlinStatement("try { 1 } catch (e: SomeException) { } catch (e: Exception) { } finally { 2 }")
        TreeAssert.assertTree(kotlinStatement).isInstanceOf(ExceptionHandlingTree::class.java)
        val exceptionHandlingTree = kotlinStatement as ExceptionHandlingTree
        TreeAssert.assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree::class.java)
        val catchTreeList = exceptionHandlingTree.catchBlocks()
        assertThat(catchTreeList).hasSize(2)
        TreeAssert.assertTree(catchTreeList[0].catchParameter()).isInstanceOf(ParameterTree::class.java)
        val catchParameterOne = catchTreeList[0].catchParameter() as ParameterTree?
        TreeAssert.assertTree(catchParameterOne).hasParameterName("e")
        assertThat(catchParameterOne!!.type()).isNotNull
        TreeAssert.assertTree(catchTreeList[0].catchBlock()).isBlock()
        assertThat(catchTreeList[1].catchParameter()).isNotNull
        TreeAssert.assertTree(catchTreeList[1].catchBlock()).isBlock()
        assertThat(exceptionHandlingTree.finallyBlock()).isNotNull
        TreeAssert.assertTree(exceptionHandlingTree.finallyBlock()).isBlock(LiteralTree::class.java)
    }

    @Test
    fun testComments() {
        val parent: Tree =
            converter.parse("#! Shebang comment\n/** Doc comment \n*/\nfun function1(a: /* Block comment */Int, b: String): Boolean { // EOL comment\n true; }")
        TreeAssert.assertTree(parent).isInstanceOf(TopLevelTree::class.java)
        assertThat(parent.children()).hasSize(1)
        val topLevelTree = parent as TopLevelTree
        val comments = topLevelTree.allComments()
        assertThat(comments).hasSize(4)
        var comment = comments[1]
        RangeAssert.assertRange(comment.textRange()).hasRange(2, 0, 3, 2)
        RangeAssert.assertRange(comment.contentRange()).hasRange(2, 3, 3, 0)
        assertThat(comment.contentText()).isEqualTo(" Doc comment \n")
        assertThat(comment.text()).isEqualTo("/** Doc comment \n*/")
        val tree = topLevelTree.declarations()[0] as FunctionDeclarationTree
        val commentsInsideFunction = tree.metaData().commentsInside()
        // Kotlin doc is considered part of the function
        assertThat(commentsInsideFunction).hasSize(3)
        comment = commentsInsideFunction[2]
        RangeAssert.assertRange(comment.textRange()).hasRange(4, 63, 4, 77)
        RangeAssert.assertRange(comment.contentRange()).hasRange(4, 65, 4, 77)
        assertThat(comment.text()).isEqualTo("// EOL comment")
    }

    @Test
    fun testLambdas() {
        val lambdaWithDestructor = kotlinStatement("{ (a, b) -> a.length < b.length }")
        val lambdaWithoutDestructor = kotlinStatement("{ a, b -> a.length < b.length }")
        TreeAssert.assertTree(lambdaWithDestructor).hasChildren(FunctionDeclarationTree::class.java)
        TreeAssert.assertTree(lambdaWithoutDestructor).hasChildren(FunctionDeclarationTree::class.java)
        val emptyLambda = kotlinStatement("{ }").children()[0] as FunctionDeclarationTree
        assertThat(emptyLambda.body()).isNull()
    }

    @Test
    fun testEquivalenceWithComments() {
        TreesAssert.assertTrees(kotlinStatements("x + 2; // EOL comment\n"))
            .isEquivalentTo(slangStatements("x + 2;"))
    }

    @Test
    fun testMappedComments() {
        val kotlinTree = converter
            .parse("/** 1st comment */\n// comment 2\nfun function() = /* Block comment */ 3;") as TopLevelTree
        val slangTree = SLangConverter()
            .parse("/** 1st comment */\n// comment 2\nvoid fun function() { /* Block comment */ 3; }") as TopLevelTree
        assertThat(kotlinTree.allComments()).hasSize(3)
        assertThat(kotlinTree.allComments())
            .isNotEqualTo(slangTree.allComments()) // Kotlin considers the '/**' delimiter as separate comments
        val slangCommentsWithDelimiters = slangTree.allComments().stream().map { obj: Comment -> obj.text() }
            .collect(Collectors.toList())
        assertThat(kotlinTree.allComments()).extracting { obj: Comment -> obj.text() }
            .isEqualTo(slangCommentsWithDelimiters)
    }

    @Test
    fun testAssignments() {
        TreesAssert.assertTrees(kotlinStatements("x = 3\nx += y + 3\n"))
            .isEquivalentTo(slangStatements("x = 3; x += y + 3;"))
    }

    @Test
    fun testBreakContinue() {
        var tree = kotlinStatement("while(true)\nbreak;") as LoopTree
        assertThat(tree.body()).isInstanceOf(JumpTree::class.java)
        var jumpTree = tree.body() as JumpTree
        assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK)
        assertThat(jumpTree.keyword().text()).isEqualTo("break")
        assertThat(jumpTree.label()).isNull()
        tree = kotlinStatement("while(true)\nbreak@foo;") as LoopTree
        assertThat(tree.body()).isInstanceOf(JumpTree::class.java)
        jumpTree = tree.body() as JumpTree
        assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK)
        assertThat(jumpTree.keyword().text()).isEqualTo("break")
        assertThat(jumpTree.label()!!.name()).isEqualTo("foo")
        assertThat(jumpTree.metaData().tokens()).extracting { obj: Token -> obj.text() }
            .containsExactly("break", "@", "foo")
        assertThat(jumpTree.label()!!.metaData().tokens())
            .extracting { obj: Token -> obj.text() }
            .containsExactly("foo")
        tree = kotlinStatement("while(true)\ncontinue;") as LoopTree
        assertThat(tree.body()).isInstanceOf(JumpTree::class.java)
        jumpTree = tree.body() as JumpTree
        assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE)
        assertThat(jumpTree.keyword().text()).isEqualTo("continue")
        assertThat(jumpTree.label()).isNull()
        tree = kotlinStatement("while(true)\ncontinue@foo;") as LoopTree
        assertThat(tree.body()).isInstanceOf(JumpTree::class.java)
        jumpTree = tree.body() as JumpTree
        assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE)
        assertThat(jumpTree.keyword().text()).isEqualTo("continue")
        assertThat(jumpTree.label()!!.name()).isEqualTo("foo")
        assertThat(jumpTree.metaData().tokens()).extracting { obj: Token -> obj.text() }
            .containsExactly("continue", "@", "foo")
        assertThat(jumpTree.label()!!.metaData().tokens())
            .extracting { obj: Token -> obj.text() }
            .containsExactly("foo")
        TreesAssert.assertTrees(kotlinStatements("while(true)\nbreak;"))
            .isEquivalentTo(slangStatements("while(true)\nbreak;"))
        TreesAssert.assertTrees(kotlinStatements("while(true)\ncontinue;"))
            .isEquivalentTo(slangStatements("while(true)\ncontinue;"))
        TreesAssert.assertTrees(kotlinStatements("while(true)\nbreak@foo;"))
            .isEquivalentTo(slangStatements("while(true)\nbreak foo;"))
        TreesAssert.assertTrees(kotlinStatements("while(true)\ncontinue@foo;"))
            .isEquivalentTo(slangStatements("while(true)\ncontinue foo;"))
    }

    @Test
    fun testReturn() {
        var tree = kotlinStatement("return 2;")
        assertThat(tree).isInstanceOf(ReturnTree::class.java)
        var returnTree = tree as ReturnTree
        assertThat(returnTree.keyword().text()).isEqualTo("return")
        assertThat(returnTree.body()).isInstanceOf(LiteralTree::class.java)
        tree = kotlinStatement("return;")
        assertThat<Tree>(tree).isInstanceOf(ReturnTree::class.java)
        returnTree = tree as ReturnTree
        assertThat(returnTree.keyword().text()).isEqualTo("return")
        assertThat(returnTree.body()).isNull()
        tree = kotlinStatement("return@foo 2;")
        assertThat<Tree>(tree).isInstanceOf(NativeTree::class.java)
        TreeAssert.assertTree(kotlinStatement("return 2;"))
            .isEquivalentTo(slangStatement("return 2;"))
        TreeAssert.assertTree(kotlinStatement("return;"))
            .isEquivalentTo(slangStatement("return;"))
        TreeAssert.assertTree(kotlinStatement("return@foo;"))
            .isNotEquivalentTo(slangStatement("return;"))
        TreeAssert.assertTree(kotlinStatement("return@foo;"))
            .isNotEquivalentTo(kotlinStatement("return@bar;"))
    }

    @Test
    fun testThrow() {
        val tree = kotlinStatement("throw Exception();")
        assertThat(tree).isInstanceOf(ThrowTree::class.java)
        val throwTree = tree as ThrowTree
        assertThat(throwTree.keyword().text()).isEqualTo("throw")
        TreeAssert.assertTree(throwTree.body()).isInstanceOf(NativeTree::class.java)
        TreeAssert.assertTree(throwTree.body()).isEquivalentTo(kotlinStatement("Exception();"))
    }

    @Test
    fun testTokens() {
        val tokens = kotlin("private fun foo() { 42 + \"a\" }").metaData().tokens()
        assertThat(tokens).extracting { obj: Token -> obj.text() }
            .containsExactly(
                "private", "fun", "foo", "(", ")", "{", "42", "+", "\"", "a", "\"", "}")
        assertThat(tokens).extracting { obj: Token -> obj.type() }
            .containsExactly(Token.Type.KEYWORD,
                Token.Type.KEYWORD,
                Token.Type.OTHER,
                Token.Type.OTHER,
                Token.Type.OTHER,
                Token.Type.OTHER,
                Token.Type.OTHER,
                Token.Type.OTHER,
                Token.Type.OTHER,
                Token.Type.STRING_LITERAL,
                Token.Type.OTHER,
                Token.Type.OTHER)
    }

    @Test
    fun testIntegerLiterals() {
        val literal0 = kotlinStatement("0Xaa") as IntegerLiteralTree
        TreeAssert.assertTree(literal0).isLiteral("0Xaa")
        assertThat(literal0.base).isEqualTo(IntegerLiteralTree.Base.HEXADECIMAL)
        assertThat(literal0.integerValue.toInt()).isEqualTo(170)
        assertThat(literal0.numericPart).isEqualTo("aa")
        val literal2 = kotlinStatement("123") as IntegerLiteralTree
        TreeAssert.assertTree(literal2).isLiteral("123")
        assertThat(literal2.base).isEqualTo(IntegerLiteralTree.Base.DECIMAL)
        assertThat(literal2.integerValue.toInt()).isEqualTo(123)
        assertThat(literal2.numericPart).isEqualTo("123")
        val literal3 = kotlinStatement("0b101") as IntegerLiteralTree
        TreeAssert.assertTree(literal3).isLiteral("0b101")
        assertThat(literal3.base).isEqualTo(IntegerLiteralTree.Base.BINARY)
        assertThat(literal3.integerValue.toInt()).isEqualTo(5)
        assertThat(literal3.numericPart).isEqualTo("101")
    }

    private fun slangStatement(innerCode: String): Tree {
        val slangStatements = slangStatements(innerCode)
        assertThat(slangStatements).hasSize(1)
        return slangStatements[0]
    }

    private fun slangStatements(innerCode: String): List<Tree> {
        val tree = SLangConverter().parse(innerCode)
        assertThat(tree).isInstanceOf(TopLevelTree::class.java)
        return tree.children()
    }

    private fun kotlinStatement(innerCode: String): Tree {
        val kotlinStatements = kotlinStatements(innerCode)
        assertThat(kotlinStatements).hasSize(1)
        return kotlinStatements[0]
    }

    private fun kotlin(innerCode: String): Tree {
        val tree: Tree = converter.parse(innerCode)
        assertThat(tree).isInstanceOf(TopLevelTree::class.java)
        assertThat(tree.children()).hasSize(1)
        return tree.children()[0]
    }

    private fun kotlinStatements(innerCode: String): List<Tree> {
        val functionDeclarationTree = kotlin("fun function1() { $innerCode }") as FunctionDeclarationTree
        assertThat(functionDeclarationTree.body()).isNotNull
        return functionDeclarationTree.body()!!.statementOrExpressions()
    }

}

private fun createString(s: String) = """"$s""""
private fun createEscaped(s: Char) = "\\$s"
private fun createEscapedString(s: Char) = createString(createEscaped(s))
private fun getCondition(cases: List<MatchCaseTree>, i: Int): Tree? = cases[i].expression()
