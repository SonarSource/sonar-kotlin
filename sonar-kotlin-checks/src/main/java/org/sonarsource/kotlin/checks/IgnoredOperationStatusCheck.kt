/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.codegen.optimization.common.analyze
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.Variance
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.simpleName
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

@Rule(key = "S899")
class IgnoredOperationStatusCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher(qualifier = "java.io.File") {
            withNames("delete", "mkdir", "renameTo", "setReadOnly", "setLastModified", "setWritable", "setReadable", "setExecutable")
        },
        FunMatcher(qualifier = "java.util.Iterator", name = "hasNext") { withNoArguments() },
        FunMatcher(qualifier = "kotlin.collections.Iterator", name = "hasNext") { withNoArguments() },
        FunMatcher(qualifier = "kotlin.collections.MutableIterator", name = "hasNext") { withNoArguments() },
        FunMatcher(qualifier = "java.util.Enumeration", name = "hasMoreElements") { withNoArguments() },
        FunMatcher(qualifier = "java.util.concurrent.locks.Lock", name = "tryLock"),
        FunMatcher(qualifier = "java.util.concurrent.locks.Condition", name = "await") {
            withArguments("kotlin.Long", "java.util.concurrent.TimeUnit")
        },
        FunMatcher(qualifier = "java.util.concurrent.locks.Condition") {
            withNames("awaitNanos", "awaitUntil")
            /* different argument types */
        },
        FunMatcher(qualifier = "java.util.concurrent.CountDownLatch", name = "await") {
            withArguments("kotlin.Long", "java.util.concurrent.TimeUnit")
        },
        FunMatcher(qualifier = "java.util.concurrent.Semaphore", name = "tryAcquire"),
        FunMatcher(qualifier = "java.util.concurrent.BlockingQueue") {
            withNames("offer", "remove")
        },
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) = analyze {
        if (!callExpression.isUsedAsExpression) {
//            resolvedCall.resultingDescriptor?.let { resultingDescriptor ->
            val name = resolvedCall.partiallyAppliedSymbol.signature.symbol.name
            val returnType = resolvedCall.partiallyAppliedSymbol.signature.returnType.simpleName() ?: "this method"
//            val returnType = callExpression.expressionType?.simpleName() ?: "this method"
            // TODO below was call to ApiExtensions KotlinType.simpleName()
//                val returnType = resultingDescriptor.returnType?.simpleName() ?: "this method";
                val message = """Do something with the "$returnType" value returned by "${name}"."""
                kotlinFileContext.reportIssue(callExpression.calleeExpression!!, message)
//            }
        }
    }

    /**
     * Replacement for [org.sonarsource.kotlin.api.checks.simpleName] ?
     */
    @OptIn(KaExperimentalApi::class)
    private fun KaType.simpleName(): String? = analyze {
        this@simpleName.lowerBoundIfFlexible().symbol?.name?.asString()
//        this@simpleName.render(
//            KaTypeRendererForSource.WITH_SHORT_NAMES,
//            position = Variance.INVARIANT
//        )
    }

}
