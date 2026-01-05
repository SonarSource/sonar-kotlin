/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.visiting.withKaSession

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
        when {
            SSL_CONTEXT_MATCHER.matches(node) ->
                handleSSL(node, kotlinFileContext)
            OKHTTP_MATCHER.matches(node) ->
                handleOkHttp(node, kotlinFileContext)
        }
    }

    private fun handleSSL(
        node: KtCallExpression,
        kotlinFileContext: KotlinFileContext,
    ) {
        node.valueArguments
            .firstOrNull()
            ?.getArgumentExpression()
            ?.let {
                if (WEAK_FOR_SSL.contains(it.value()))
                    reportUnsecureSSLContext(listOf(it), kotlinFileContext)
            }
    }

    private fun handleOkHttp(
        node: KtCallExpression,
        kotlinFileContext: KotlinFileContext,
    ) {
        val unsecureVersions = node.valueArguments
            .mapNotNull { it.getArgumentExpression() }
            .filter {
                WEAK_FOR_OK_HTTP.contains(it.value())
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

    private fun KtExpression.value(): String? = withKaSession {
        when (this@value) {
            is KtStringTemplateExpression -> asConstant()
            is KtNameReferenceExpression -> predictRuntimeStringValue()
            is KtDotQualifiedExpression -> {
                (selectorExpression?.mainReference?.resolveToSymbol() as? KaEnumEntrySymbol)
                    ?.callableId?.asSingleFqName()?.asString()
            }
            else -> null
        }
    }
}
