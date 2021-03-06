/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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
package org.sonarsource.kotlin.api

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.utils.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.util.TreeMap

internal class ApiExtensionsKtTest {

    @Test
    fun `test isLocalVariable`() {
        val tree = parse("""
            const val a = "abc"
            val b = 1
            var c = true
            class A {
              const val d = ""
              var e = 7
              var f = 1.2
              fun foo(g: Int): List<Any?> {
                const val h = ""
                val i = 3
                var j = 4
                val k :(Int) -> Int = {
                  l -> {
                    var m = l
                    m + 1
                  }
                }
                val n :(Int) -> Int = { it + 1 }
                // reference all variables and fields
                return listOf(a, b, c, d, e, f, g, h, i, j, k, n)
              }
            }
            fun bar(): Int {
              val o = 0
              return o
            }
            """.trimIndent())
        val referencesMap: MutableMap<String, KtReferenceExpression> = TreeMap()
        walker(tree.psiFile) {
            if (it is KtNameReferenceExpression) {
                referencesMap[it.getReferencedName()] = it
            }
        }
        val ctx = tree.bindingContext
        assertThat((null as KtExpression?).isLocalVariable(ctx)).isFalse

        assertThat(referencesMap.keys).containsExactlyInAnyOrder("Any", "Int", "List",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "it", "j", "k", "l", "listOf", "m", "n", "o")
        assertThat(referencesMap.filter { it.value.isLocalVariable(ctx) }.map { it.key })
            .containsExactlyInAnyOrder("h", "i", "j", "k", "m", "n", "o")
    }

    @Test
    fun `test getterMatches and setterMatches`() {
        val tree = parse("""
        fun foo(thread: Thread): Int {
          thread.name = "1"
          thread.isDaemon = false
          var x = 0
          x = 1
          return thread.priority
        }
        """.trimIndent())

        val referencesMap: MutableMap<String, KtReferenceExpression> = TreeMap()
        walker(tree.psiFile) {
            if (it is KtNameReferenceExpression) {
                referencesMap[it.getReferencedName()] = it
            }
        }

        val getNameMatcher = FunMatcher(qualifier = "java.lang.Thread", name = "getName")
        val setNameMatcher = FunMatcher(qualifier = "java.lang.Thread", name = "setName") { withArguments("kotlin.String") }
        val isDaemonMatcher = FunMatcher(qualifier = "java.lang.Thread", name = "isDaemon")
        val setDaemonMatcher = FunMatcher(qualifier = "java.lang.Thread", name = "setDaemon") { withArguments("kotlin.Boolean") }

        val ctx = tree.bindingContext
        assertThat((null as KtExpression?).getterMatches(ctx, "name", getNameMatcher)).isFalse
        assertThat((null as KtExpression?).setterMatches(ctx, "name", setNameMatcher)).isFalse

        assertThat(referencesMap.keys).containsExactlyInAnyOrder("Int", "Thread", "isDaemon", "name", "priority", "thread", "x")
        assertThat(referencesMap.filter { it.value.getterMatches(ctx, "name", getNameMatcher) }.map { it.key })
            .containsExactlyInAnyOrder("name")
        assertThat(referencesMap.filter { it.value.setterMatches(ctx, "name", setNameMatcher) }.map { it.key })
            .containsExactlyInAnyOrder("name")
        assertThat(referencesMap.filter { it.value.getterMatches(ctx, "isDaemon", isDaemonMatcher) }.map { it.key })
            .containsExactlyInAnyOrder("isDaemon")
        assertThat(referencesMap.filter { it.value.setterMatches(ctx, "isDaemon", setDaemonMatcher) }.map { it.key })
            .containsExactlyInAnyOrder("isDaemon")

        // should not match
        assertThat(referencesMap.filter { it.value.getterMatches(ctx, "priority", getNameMatcher) }.map { it.key })
            .isEmpty()
        assertThat(referencesMap.filter { it.value.setterMatches(ctx, "priority", setNameMatcher) }.map { it.key })
            .isEmpty()
        assertThat(referencesMap.filter { it.value.getterMatches(ctx, "x", getNameMatcher) }.map { it.key })
            .isEmpty()
        assertThat(referencesMap.filter { it.value.setterMatches(ctx, "x", setNameMatcher) }.map { it.key })
            .isEmpty()
    }

    @Test
    fun `PsiElement getType()`() {
        assertThat((null as PsiElement?).getType(BindingContext.EMPTY)).isNull()
        assertThat(PsiWhiteSpaceImpl(" ").getType(BindingContext.EMPTY)).isNull()
    }

    private fun walker(node: PsiElement, action: (PsiElement) -> Unit) {
        action(node)
        node.allChildren.forEach { walker(it, action) }
    }
}

class ApiExtensionsKtDetermineTypeTest {
    private val bindingContext: BindingContext
    private val ktFile: KtFile

    init {
        val kotlinTree = parse(
            """
        package bar
        
        class Foo {
            val prop: Int = 0
        
            fun aFun(param: Float): Long {
                stringReturning()
                this.prop
                val localVal: Double
            }
            fun stringReturning(): String {}
            
            fun anotherFun(obj: Foo): Long {
                obj.prop
            }
        }
        """.trimIndent()
        )
        bindingContext = kotlinTree.bindingContext
        ktFile = kotlinTree.psiFile
    }

    @Test
    fun `determineType of KtCallExpression`() {
        val expr = ktFile.findDescendantOfType<KtCallExpression> { it.text == "stringReturning()" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.String")
    }

    @Test
    fun `determineType of KtParameter`() {
        val expr = ktFile.findDescendantOfType<KtParameter> { it.text == "param: Float" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.Float")
    }

    @Test
    fun `determineType of KtTypeReference`() {
        val expr = ktFile.findDescendantOfType<KtTypeReference> { it.text == "Int" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.Int")
    }

    @Test
    fun `determineType of KtProperty`() {
        val expr = ktFile.findDescendantOfType<KtProperty> { it.text == "val prop: Int = 0" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.Int")
    }

    @Test
    fun `determineType of KtDotQualifiedExpression`() {
        val expr = ktFile.findDescendantOfType<KtDotQualifiedExpression> { it.text == "this.prop" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.Int")
    }

    @Test
    fun `determineType of KtReferenceExpression`() {
        val expr = ktFile.findDescendantOfType<KtReferenceExpression> { it.text == "Int" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.Int")
    }

    @Test
    fun `determineType of KtFunction`() {
        val expr = ktFile.findDescendantOfType<KtFunction> { it.name == "stringReturning" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("kotlin.String")
    }

    @Test
    fun `determineType of KtClass`() {
        val expr = ktFile.findDescendantOfType<KtClass> { it.name == "Foo" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("bar.Foo")
    }

    @Test
    fun `determineType else`() {
        // Block - not supported for now and will return null
        val expr = ktFile.findDescendantOfType<KtBlockExpression>()!!

        assertThat(expr.determineType(bindingContext))
            .isNull()
    }

    @Test
    fun `determineType of declaration not supported`() {
        assertThat((null as FunctionDescriptor?).determineType())
            .isNull()
    }

    @Test
    fun `determineType of ValueDescriptor`() {
        val expr = ktFile.findDescendantOfType<KtReferenceExpression> { it.text == "obj" }!!

        assertThat(expr.determineType(bindingContext)!!.getJetTypeFqName(false))
            .isEqualTo("bar.Foo")
    }
}

class ApiExtensionsKtDetermineSignatureTest {
    private val bindingContext: BindingContext
    private val ktFile: KtFile

    init {
        val kotlinTree = parse(
            """
        package bar
        
        class Foo {
            val prop: Int = 0
        
            fun aFun(param: Float): Long {
                this.prop
            }
        }
        """.trimIndent()
        )
        bindingContext = kotlinTree.bindingContext
        ktFile = kotlinTree.psiFile
    }

    @Test
    fun `determineSignature of KtQualifiedExpression`() {
        val expr = ktFile.findDescendantOfType<KtQualifiedExpression> { it.text == "this.prop" }!!

        assertThat(expr.determineSignature(bindingContext)?.fqNameOrNull()?.asString()).isEqualTo("bar.Foo.prop")
    }

    @Test
    fun `determineSignature of a null KtQualifiedExpression`() {
        assertThat(null.determineSignature(bindingContext)).isNull()
    }
}

private fun parse(code: String) = kotlinTreeOf(
    code,
    Environment(listOf("build/classes/kotlin/main"), LanguageVersion.LATEST_STABLE),
    TestInputFileBuilder("moduleKey", "src/org/foo/kotlin.kt")
        .setCharset(StandardCharsets.UTF_8)
        .initMetadata(code)
        .build()
)
