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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CLASS
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S4830")
class ServerCertificateCheck : AbstractCheck() {
    companion object {
        private val methodNames = setOf("checkClientTrusted", "checkServerTrusted")
        private val firstArgRegex = Regex("""Array<(out )?X509Certificate\??>\??""")
        private val secondArgRegex = Regex("""String\??""")
    }

    override fun visitClassOrObject(node: KtClassOrObject, kotlinFileContext: KotlinFileContext) {
        val (_, _, bindingContext) = kotlinFileContext

        val extendsX509 = bindingContext.get(CLASS, node)?.getAllSuperClassifiers()?.any {
            it.fqNameOrNull()?.asString() == "javax.net.ssl.X509TrustManager"
        } ?: return
        if (extendsX509) {
            node.body?.functions?.forEach { f ->
                if (methodNames.contains(f.name)
                    && f.hasCompliantParameters(bindingContext)
                // TODO: there is a test (line 87) that pass only when this is not commented out
                // && f.listStatements().none { it.throwsException(bindingContext) }
                ) {
                    val throwCatchVisitor = ThrowCatchVisitor()
                    f.acceptRecursively(throwCatchVisitor)
                    if (!throwCatchVisitor.foundThrow()) {
                        kotlinFileContext.reportIssue(f.nameIdentifier ?: f,
                            "Enable server certificate validation on this SSL/TLS connection.")
                    } else if (throwCatchVisitor.foundCatch()) {
                        kotlinFileContext.reportIssue(f.nameIdentifier ?: f,
                            "Enable server certificate validation on this SSL/TLS connection.")
                    }
                }
            }
        }
    }

    private fun KtNamedFunction.hasCompliantParameters(bindingContext: BindingContext) =
        valueParameters.size in 2..3
            && valueParameters[0].typeAsString(bindingContext).matches(firstArgRegex)
            && valueParameters[1].typeAsString(bindingContext).matches(secondArgRegex)

    private fun PsiElement.acceptRecursively(visitor: KtVisitorVoid) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }

    private class ThrowCatchVisitor : KtVisitorVoid() {
        private var throwFound: Boolean = false
        private var catchFound: Boolean = false

        override fun visitThrowExpression(expression: KtThrowExpression) {
            // TODO: check it is CertificateException
            throwFound = true
        }

        override fun visitCatchSection(catchClause: KtCatchClause) {
            // TODO check if CertificationException
            catchFound = true
        }

        fun foundThrow(): Boolean {
            return throwFound
        }

        fun foundCatch(): Boolean {
            return catchFound
        }
    }
}
