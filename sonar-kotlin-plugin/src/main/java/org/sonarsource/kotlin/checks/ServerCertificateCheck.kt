/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.determineType
import org.sonarsource.kotlin.plugin.KotlinFileContext


private const val CERTIFICATE_EXCEPTION = "java.security.cert.CertificateException"

private val funMatchers = listOf(
    FunMatcher {
        definingSupertype = "javax.net.ssl.X509TrustManager"
        withNames("checkClientTrusted", "checkServerTrusted")
    },
    FunMatcher {
        definingSupertype = "javax.net.ssl.X509ExtendedTrustManager"
        withNames("checkClientTrusted", "checkServerTrusted")
    })


@Rule(key = "S4830")
class ServerCertificateCheck : AbstractCheck() {
    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val (_, _, bindingContext) = kotlinFileContext

        if (function.belongsToTrustManagerClass(bindingContext)
            && !function.callsCheckTrusted(bindingContext)
            && !function.throwsCertificateExceptionWithoutCatching(bindingContext)
        ) {
            kotlinFileContext.reportIssue(function.nameIdentifier ?: function,
                "Enable server certificate validation on this SSL/TLS connection.")
        }
    }

    private fun KtNamedFunction.belongsToTrustManagerClass(bindingContext: BindingContext): Boolean =
        funMatchers.any { it.matches(this, bindingContext) }

    /*
     * Returns true if a function contains a call to "checkClientTrusted" or "checkServerTrusted".
     */
    private fun KtNamedFunction.callsCheckTrusted(bindingContext: BindingContext): Boolean {
        val visitor = object : KtVisitorVoid() {
            private var foundCheckTrustedCall: Boolean = false

            override fun visitCallExpression(expression: KtCallExpression) {
                foundCheckTrustedCall = foundCheckTrustedCall || funMatchers.any { it.matches(expression, bindingContext) }
            }

            fun callsCheckTrusted(): Boolean = foundCheckTrustedCall
        }
        this.acceptRecursively(visitor)
        return visitor.callsCheckTrusted()
    }

    /*
     * Returns true only when the function throws a CertificateException without a catch against it.
     */
    private fun KtNamedFunction.throwsCertificateExceptionWithoutCatching(bindingContext: BindingContext): Boolean {
        val visitor = ThrowCatchVisitor(bindingContext)
        this.acceptRecursively(visitor)
        return visitor.throwsCertificateExceptionWithoutCatching()
    }

    private class ThrowCatchVisitor(private val bindingContext: BindingContext) : KtVisitorVoid() {
        private var throwFound: Boolean = false
        private var catchFound: Boolean = false

        override fun visitThrowExpression(expression: KtThrowExpression) {
            throwFound =
                throwFound || CERTIFICATE_EXCEPTION == expression.thrownExpression.determineType(bindingContext)?.getJetTypeFqName(false)
        }

        override fun visitCatchSection(catchClause: KtCatchClause) {
            catchFound =
                catchFound || CERTIFICATE_EXCEPTION == catchClause.catchParameter.determineType(bindingContext)?.getJetTypeFqName(false)
        }

        fun throwsCertificateExceptionWithoutCatching(): Boolean {
            return throwFound && !catchFound
        }
    }

    private fun PsiElement.acceptRecursively(visitor: KtVisitorVoid) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }
}
