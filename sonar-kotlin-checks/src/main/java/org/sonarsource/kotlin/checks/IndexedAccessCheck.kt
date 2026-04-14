/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.asFqNameString
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6518")
class IndexedAccessCheck : CallAbstractCheck() {

    companion object {
        private val LIST_CLASS_ID = ClassId.fromString("kotlin/collections/List")
        private val MAP_CLASS_ID = ClassId.fromString("kotlin/collections/Map")

        /**
         * Java classes where indexed access operator syntax is idiomatic in Kotlin.
         * For these types, we still raise even though the operator comes from Java interop.
         *
         * Note: concrete List/Map implementations (ArrayList, HashMap, Vector, etc.) are handled
         * separately via a supertype check against kotlin.collections.List and kotlin.collections.Map.
         */
        private val JAVA_INTEROP_ALLOWED_TYPES = setOf(
            // NIO Buffer types - get(index) is natural indexed access
            "java.nio.ByteBuffer",
            "java.nio.CharBuffer",
            "java.nio.ShortBuffer",
            "java.nio.IntBuffer",
            "java.nio.LongBuffer",
            "java.nio.FloatBuffer",
            "java.nio.DoubleBuffer",
            // BitSet - get(index)/set(index, value) is natural indexed access
            "java.util.BitSet",
            // Atomic arrays - get/set by index is natural
            "java.util.concurrent.atomic.AtomicIntegerArray",
            "java.util.concurrent.atomic.AtomicLongArray",
            "java.util.concurrent.atomic.AtomicReferenceArray",
            // Android sparse arrays - get/set by key is natural
            "android.util.SparseArray",
            "android.util.SparseIntArray",
            "android.util.SparseBooleanArray",
            "android.util.SparseLongArray",
            "android.util.LongSparseArray",
        )
    }

    override val functionsToVisit = listOf(
        FunMatcher(isOperator = true) {
            withNames("get", "set")
        }
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val dotExpression = callExpression.parent as? KtDotQualifiedExpression ?: return
        if (dotExpression.receiverExpression is KtSuperExpression) return
        if (callExpression.typeArgumentList != null) return
        if(callExpression.valueArguments.any {  it.isNamed() }) return
        if (isJavaInteropOperator(resolvedCall) && !isAllowedJavaInteropType(resolvedCall)) return
        kotlinFileContext.reportIssue(callExpression.calleeExpression!!, "Replace function call with indexed accessor.")
    }

    private fun getReceiverTypeFqn(resolvedCall: KaFunctionCall<*>): String? = withKaSession {
        (resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver)?.type?.asFqNameString()
    }

    /**
     * Checks whether the resolved function is a Java interop operator (i.e., a Java method
     * that Kotlin treats as an operator via interop, rather than a function explicitly declared
     * with the `operator` keyword in Kotlin source).
     */
    private fun isJavaInteropOperator(resolvedCall: KaFunctionCall<*>): Boolean {
        return resolvedCall.symbol.origin == KaSymbolOrigin.JAVA_SOURCE
            || resolvedCall.symbol.origin == KaSymbolOrigin.JAVA_LIBRARY
    }

    /**
     * Checks whether the receiver type of the resolved function is in the allow-list
     * of Java types where indexed access operator syntax is idiomatic.
     */
    private fun isAllowedJavaInteropType(resolvedCall: KaFunctionCall<*>): Boolean = withKaSession {
        if (getReceiverTypeFqn(resolvedCall) in JAVA_INTEROP_ALLOWED_TYPES) return true
        // Any List or Map implementation supports idiomatic indexed access
        val type = (resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver)?.type ?: return false
        return type.isSubtypeOf(LIST_CLASS_ID) || type.isSubtypeOf(MAP_CLASS_ID)
    }
}
