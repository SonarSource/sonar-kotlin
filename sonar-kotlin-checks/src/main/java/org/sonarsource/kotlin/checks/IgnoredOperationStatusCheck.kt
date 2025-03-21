/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.simpleName
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

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

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) = withKaSession {
        if (!callExpression.isUsedAsExpression) {
            val name = resolvedCall.partiallyAppliedSymbol.signature.symbol.name
            val returnType = resolvedCall.partiallyAppliedSymbol.signature.returnType.simpleName() ?: /* TODO improve message: */ "this method"
            val message = """Do something with the "$returnType" value returned by "${name}"."""
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, message)
        }
    }

}
