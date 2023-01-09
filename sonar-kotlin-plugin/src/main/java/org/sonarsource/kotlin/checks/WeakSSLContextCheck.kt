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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S4423")
class WeakSSLContextCheck : AbstractCheck() {
    private val WEAK_FOR_OK_HTTP = setOf(
        "TLSv1",
        "TLSv1.1",
        "TLS_1_0",
        "TLS_1_1",
        "okhttp3.TlsVersion.TLS_1_0",
        "okhttp3.TlsVersion.TLS_1_1",
    )

    private val WEAK_FOR_SSL = setOf(
        "SSL",
        "TLS",
        "DTLS",
        "SSLv2",
        "SSLv3",
        "TLSv1",
        "TLSv1.1",
        "DTLSv1.0",
    )

    private val OKHTTP_MATCHER = FunMatcher {
        qualifier = "okhttp3.ConnectionSpec.Builder"
        name = "tlsVersions"
    }

    private val SSL_CONTEXT_MATCHER = FunMatcher {
        qualifier = "javax.net.ssl.SSLContext"
        name = "getInstance"
    }

    override fun visitCallExpression(node: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext
        when {
            SSL_CONTEXT_MATCHER.matches(node, bindingContext) ->
                handleSSL(node, bindingContext, kotlinFileContext)
            OKHTTP_MATCHER.matches(node, bindingContext) ->
                handleOkHttp(node, bindingContext, kotlinFileContext)
        }
    }

    private fun handleSSL(
        node: KtCallExpression,
        bindingContext: BindingContext,
        kotlinFileContext: KotlinFileContext,
    ) {
        node.valueArguments
            .firstOrNull()
            ?.getArgumentExpression()
            ?.let {
                if (WEAK_FOR_SSL.contains(it.value(bindingContext)))
                    reportUnsecureSSLContext(listOf(it), kotlinFileContext)
            }
    }

    private fun handleOkHttp(
        node: KtCallExpression,
        bindingContext: BindingContext,
        kotlinFileContext: KotlinFileContext,
    ) {
        val unsecureVersions = node.valueArguments
            .mapNotNull { it.getArgumentExpression() }
            .filter {
                WEAK_FOR_OK_HTTP.contains(it.value(bindingContext))
            }

        reportUnsecureSSLContext(unsecureVersions, kotlinFileContext)
    }

    private fun reportUnsecureSSLContext(
        unsecureVersions: List<KtExpression>,
        kotlinFileContext: KotlinFileContext,
    ) {
        if (unsecureVersions.isNotEmpty()) {
            val secondaries = unsecureVersions - unsecureVersions[0]
            kotlinFileContext.reportIssue(
                psiElement = unsecureVersions[0],
                message = "Change this code to use a stronger protocol.",
                secondaryLocations = secondaries.map {
                    SecondaryLocation(kotlinFileContext.textRange(it), "Other weak protocol.")
                }
            )
        }
    }

    private fun KtExpression.value(
        bindingContext: BindingContext,
    ): String? = when (this) {
        is KtStringTemplateExpression -> asConstant()
        is KtNameReferenceExpression -> {
            val descriptor = bindingContext.get(REFERENCE_TARGET, this)
            if (descriptor is PropertyDescriptor) descriptor.compileTimeInitializer?.boxedValue().toString()
            else null
        }
        is KtDotQualifiedExpression -> {
            val selectorExpression = selectorExpression
            if (selectorExpression is KtNameReferenceExpression)
                bindingContext.get(REFERENCE_TARGET, selectorExpression)?.fqNameOrNull()?.asString()
            else null
        }
        else -> null
    }
}
