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
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
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
        private val ANY_PARAMETER_TYPE: String? = null

        private data class JavaInteropMethodSignature(
            val name: String,
            val parameterTypes: List<String?>,
        ) {
            fun matches(name: String, parameterTypes: List<String?>): Boolean =
                this.name == name &&
                    this.parameterTypes.size == parameterTypes.size &&
                    this.parameterTypes.zip(parameterTypes).all { (expected, actual) -> expected == null || expected == actual }
        }

        private data class JavaInteropMethodGroup(
            val receiverTypes: Set<String>,
            val signatures: Set<JavaInteropMethodSignature>,
        ) {
            fun allows(receiverType: String?, name: String, parameterTypes: List<String?>): Boolean =
                receiverType in receiverTypes && signatures.any { it.matches(name, parameterTypes) }
        }

        private fun signature(name: String, vararg parameterTypes: String?) =
            JavaInteropMethodSignature(name, parameterTypes.toList())

        /** Java receiver types and the exact operator-shaped methods that are idiomatic indexed access in Kotlin. */
        private val JAVA_INTEROP_METHOD_ALLOW_LIST = listOf(
            JavaInteropMethodGroup(
                receiverTypes = setOf(
                    "java.nio.ByteBuffer",
                    "java.nio.CharBuffer",
                    "java.nio.ShortBuffer",
                    "java.nio.IntBuffer",
                    "java.nio.LongBuffer",
                    "java.nio.FloatBuffer",
                    "java.nio.DoubleBuffer",
                ),
                signatures = setOf(signature("get", "kotlin.Int")),
            ),
            JavaInteropMethodGroup(
                receiverTypes = setOf("java.util.BitSet"),
                signatures = setOf(
                    signature("get", "kotlin.Int"),
                    signature("set", "kotlin.Int", "kotlin.Boolean"),
                ),
            ),
            JavaInteropMethodGroup(
                receiverTypes = setOf(
                    "java.util.concurrent.atomic.AtomicIntegerArray",
                    "java.util.concurrent.atomic.AtomicLongArray",
                    "java.util.concurrent.atomic.AtomicReferenceArray",
                ),
                signatures = setOf(
                    signature("get", "kotlin.Int"),
                    signature("set", "kotlin.Int", ANY_PARAMETER_TYPE),
                ),
            ),
            JavaInteropMethodGroup(
                receiverTypes = setOf(
                    "android.util.SparseArray",
                    "android.util.SparseIntArray",
                    "android.util.SparseBooleanArray",
                    "android.util.SparseLongArray",
                ),
                signatures = setOf(
                    signature("get", "kotlin.Int"),
                    signature("set", "kotlin.Int", ANY_PARAMETER_TYPE),
                ),
            ),
            JavaInteropMethodGroup(
                receiverTypes = setOf("android.util.LongSparseArray"),
                signatures = setOf(
                    signature("get", "kotlin.Long"),
                    signature("set", "kotlin.Long", ANY_PARAMETER_TYPE),
                ),
            ),
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
        if (callExpression.valueArguments.any { it.isNamed() }) return
        if (isSetCallWhoseResultMustBePreserved(callExpression, dotExpression, resolvedCall)) return
        if (isJavaInteropOperator(resolvedCall) && !isAllowedJavaInteropMethod(resolvedCall)) return
        kotlinFileContext.reportIssue(callExpression.calleeExpression!!, "Replace function call with indexed accessor.")
    }

    /**
     * Indexed assignment evaluates to Unit, so a `set` call cannot be replaced when its return
     * value is used. The terminal call in a setter chain is suppressed as well: reporting only
     * that call would suggest a misleading mixture of fluent and indexed syntax.
     */
    private fun isSetCallWhoseResultMustBePreserved(
        callExpression: KtCallExpression,
        dotExpression: KtDotQualifiedExpression,
        resolvedCall: KaFunctionCall<*>,
    ): Boolean = withKaSession {
        if (resolvedCall.symbol.name?.asString() != "set") return false
        return callExpression.isUsedAsExpression || dotExpression.receiverExpression.containsSetCall()
    }

    private fun KtExpression.containsSetCall(): Boolean = when (val expression = deparenthesize(this)) {
        is KtBinaryExpressionWithTypeRHS -> expression.left.containsSetCall()
        is KtPostfixExpression ->
            expression.operationToken == KtTokens.EXCLEXCL && expression.baseExpression?.containsSetCall() == true
        is KtDotQualifiedExpression -> {
            val selectorCall = expression.selectorExpression as? KtCallExpression
            val selectorName = (selectorCall?.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
            selectorName == "set" || expression.receiverExpression.containsSetCall()
        }
        else -> false
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
     * Kotlin treats every suitably named Java `get` or `set` method as operator-shaped. Accept the
     * explicitly allow-listed element-access signatures, as well as any operator-shaped overload
     * on a List or Map subtype.
     */
    private fun isAllowedJavaInteropMethod(resolvedCall: KaFunctionCall<*>): Boolean = withKaSession {
        val type = (resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver)?.type ?: return false
        val receiverType = type.asFqNameString()
        val name = resolvedCall.symbol.name?.asString() ?: return false
        val parameterTypes = resolvedCall.signature.valueParameters.map { it.returnType.asFqNameString() }

        return JAVA_INTEROP_METHOD_ALLOW_LIST.any { it.allows(receiverType, name, parameterTypes) } ||
            // Any List or Map implementation supports idiomatic indexed access
            type.isSubtypeOf(LIST_CLASS_ID) || type.isSubtypeOf(MAP_CLASS_ID)
    }
}
