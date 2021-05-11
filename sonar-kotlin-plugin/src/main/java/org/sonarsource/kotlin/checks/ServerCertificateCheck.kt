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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CLASS
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
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

        val extendsX509 = bindingContext.get(CLASS, node).getAllSuperTypesInterfaces().any {
            it.fqNameOrNull()?.asString() == "javax.net.ssl.X509TrustManager"
        }
        if (extendsX509) {
            node.body?.functions?.forEach { f ->
                if (methodNames.contains(f.name) 
                    && f.hasCompliantParameters(bindingContext)
                    && f.listStatements().none { it.throwsException(bindingContext) }) {
                    kotlinFileContext.reportIssue(f.nameIdentifier ?: f,
                        "Enable server certificate validation on this SSL/TLS connection.")
                }
            }
        }
    }

    private fun KtNamedFunction.hasCompliantParameters(bindingContext: BindingContext) =
        valueParameters.size in 2..3
            && valueParameters[0].typeAsString(bindingContext).matches(firstArgRegex)
            && valueParameters[1].typeAsString(bindingContext).matches(secondArgRegex)
}
