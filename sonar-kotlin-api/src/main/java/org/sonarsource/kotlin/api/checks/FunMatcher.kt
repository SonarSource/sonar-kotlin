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
package org.sonarsource.kotlin.api.checks

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val VARARG_PREFIX = "vararg ";

class FunMatcherImpl(
    // In case of Top Level function there is no type there,
    // so you just need to specify the package
    val qualifiers: Set<String> = emptySet(),
    val names: Set<String> = emptySet(),
    // nameRegex is used when function name can be matched with regex,
    // and can be used together with names argument
    val nameRegex: Regex? = null,
    private val maxArgumentCount: Int = Int.MAX_VALUE,
    val arguments: List<List<ArgumentMatcher>> = emptyList(),
    val definingSupertypes: Set<String> = emptySet(),
    private val matchConstructor: Boolean = false,
    val isDynamic: Boolean? = null,
    val isExtensionFunction: Boolean? = null,
    val isSuspending: Boolean? = null,
    val isOperator: Boolean? = null,
    val returnType: String? = null,
) {
    private val qualifiersOrDefiningSupertypes: Set<String> =
        if (definingSupertypes.isEmpty()) qualifiers else
            if (qualifiers.isEmpty()) definingSupertypes else qualifiers + definingSupertypes

    fun matches(node: KtCallExpression): Boolean = withKaSession {
        val call = node.resolveToCall()?.successfulFunctionCallOrNull()
        return call != null && matches(call)
    }

    @OptIn(KaExperimentalApi::class)
    fun matches(node: KtNamedFunction): Boolean = withKaSession {
        val symbol = node.symbol
        return matches(null, symbol, symbol.asSignature())
    }

    @OptIn(KaExperimentalApi::class)
    fun matches(resolvedCall: KaCallableMemberCall<*, *>?): Boolean = withKaSession {
        when (resolvedCall) {
            null -> false
            is KaFunctionCall -> {
                val symbol = resolvedCall.partiallyAppliedSymbol
                matches(symbol.dispatchReceiver, symbol.signature.symbol, symbol.signature)
            }
            is KaSimpleVariableAccessCall -> {
                val propertySymbol = (resolvedCall.symbol as? KaPropertySymbol) ?: return false
                when (resolvedCall.simpleAccess) {
                    is KaSimpleVariableAccess.Read -> {
                        val symbolForNameCheck =
                            /**
                             * Note that this allows to use matcher with name `"getExample"`
                             * for both function call `getExample(...)` and read by property name `example`.
                             */
                            if (propertySymbol is KaSyntheticJavaPropertySymbol)
                                propertySymbol.javaGetterSymbol
                            else
                                propertySymbol
                        checkName(symbolForNameCheck) &&
                                /**
                                 * Note that unlike in K1 [KaPropertySymbol.getter] returns `null` for `MutableList.size` in K2.
                                 * if this inconsistency between K1 and K2 is not a bug, then maybe getter of
                                 * [org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider.fakeOverrideOriginal]
                                 * should be used instead, however here we can simply check arguments as following:
                                 */
                                (arguments.isEmpty() || arguments.any { argument -> argument.isEmpty() }) &&
                                checkReturnType(propertySymbol.asSignature()) &&
                                (checkTypeOrSupertype(null, propertySymbol) ||
                                        // TODO propertySymbol works only in K2 (see ExternalAndroidStorageAccessCheck):
                                        checkTypeOrSupertype(null, symbolForNameCheck))
                    }
                    is KaSimpleVariableAccess.Write -> {
                        val symbolForNameCheck =
                            /**
                             * Note that this allows to use matcher with name `"setExample"`
                             * for both function call `setExample(...)` and write by property name `example`.
                             */
                            if (propertySymbol is KaSyntheticJavaPropertySymbol)
                                propertySymbol.javaSetterSymbol ?: return false
                            else
                                propertySymbol
                        val setterSignature = propertySymbol.setter?.asSignature() ?: return false
                        checkName(symbolForNameCheck) &&
                                checkCallParameters(setterSignature) &&
                                checkReturnType(setterSignature) &&
                                checkTypeOrSupertype(
                                    null,
                                    // TODO propertySymbol works only in K2 (see ScheduledThreadPoolExecutorZeroCheck):
                                    symbolForNameCheck
                                )
                    }
                }
            }
        }
    }

    private fun matches(
        dispatchReceiver: KaReceiverValue?,
        symbol: KaCallableSymbol,
        callableSignature: KaCallableSignature<*>
    ): Boolean {
        if (isDynamic != null) TODO()
        return callableSignature != null &&
            checkIsExtensionFunction(callableSignature) &&
            checkIsSuspending(callableSignature) &&
            checkIsOperator(callableSignature) &&
            checkReturnType(callableSignature) &&
            checkName(symbol) &&
            checkTypeOrSupertype(dispatchReceiver, symbol) &&
            checkCallParameters(callableSignature)
    }

    private fun checkTypeOrSupertype(
        dispatchReceiver: KaReceiverValue?,
        symbol: KaCallableSymbol,
    ): Boolean {
        if (qualifiersOrDefiningSupertypes.isEmpty()) return true

        // TODO try to use only dispatchReceiver?
        // java.lang.Math.random has kotlin/Unit receiver type in K2 and null in K1?
        val receiverFQN = (dispatchReceiver?.type as? KaClassType)?.classId?.asFqNameString();
        if (qualifiersOrDefiningSupertypes.contains(receiverFQN)) return true
        if (qualifiersOrDefiningSupertypes.contains(getActualQualifier(symbol))) return true

        if (!definingSupertypes.isNullOrEmpty() && checkSubType(symbol)) return true
        return false
    }

    // TODO when null?
    /** @return dot-separated package name for top-level functions, class name otherwise */
    private fun getActualQualifier(symbol: KaCallableSymbol): String? {
        return if (symbol is KaConstructorSymbol) {
            symbol.returnType.asFqNameString() // callableId is null for ctors, containingClassId would return type aliases
        } else {
            symbol.callableId?.asSingleFqName()?.parent()?.asString()
        }
    }

    private fun checkSubType(symbol: KaCallableSymbol): Boolean = withKaSession {
        if (symbol is KaConstructorSymbol) return false
        return symbol.allOverriddenSymbols.any {
            val className: String? = it.callableId?.asSingleFqName()?.parent()?.asString()
            definingSupertypes.contains(className)
        }
    }

    private fun checkName(symbol: KaCallableSymbol): Boolean {
        if (matchConstructor) return symbol is KaConstructorSymbol
        val name = symbol.name?.asString() ?: return false
        return (nameRegex == null && names.isEmpty()) || name in names || (nameRegex != null && nameRegex.matches(name))
    }

    private fun checkCallParameters(callableSignature: KaCallableSignature<*>): Boolean {
        val symbol = callableSignature.symbol as? KaFunctionSymbol ?: return false
        return arguments.isEmpty() || arguments.any {
            (symbol.valueParameters.size == it.size) &&
                it.foldRightIndexed(true) { i, argType, acc ->
                    acc && argType.matches(symbol.valueParameters[i])
                }
        }
    }

    private fun checkIsExtensionFunction(callableSignature: KaCallableSignature<*>): Boolean =
        isExtensionFunction?.let { it == callableSignature.symbol.isExtension } ?: true

    /**
     * Note that [KaCallableSignature] carries use-site type information,
     * so for `fun <T> example(): T` matcher with [returnType] `"kotlin.String"`
     * matches calls of `example<String>()`.
     */
    private fun checkReturnType(callableSignature: KaCallableSignature<*>) =
       returnType?.let {
           it == callableSignature.returnType.asFqNameString()
       } ?: true

    private fun checkIsSuspending(callableSignature: KaCallableSignature<*>) =
        isSuspending?.let { it == (callableSignature.symbol as? KaNamedFunctionSymbol)?.isSuspend } ?: true

    private fun checkIsOperator(callableSignature: KaCallableSignature<*>) =
        isOperator?.let { it == (callableSignature.symbol as? KaNamedFunctionSymbol)?.isOperator } ?: true
}

class FunMatcherBuilderContext(
    var qualifier: String? = null,
    var name: String? = null,
    var nameRegex: Regex?,
    var definingSupertype: String? = null,
    var matchConstructor: Boolean = false,
    var isDynamic: Boolean? = null,
    var isExtensionFunction: Boolean? = null,
    var isSuspending: Boolean? = null,
    val isOperator: Boolean? = null,
    var returnType: String? = null,
) {
    var qualifiers: Set<String> = emptySet()
    var definingSupertypes: Set<String> = emptySet()
    var names: Set<String> = emptySet()
    var arguments: MutableList<List<ArgumentMatcher>> = mutableListOf()

    fun withQualifiers(vararg args: String) {
        qualifiers += listOf(*args)
    }

    fun withTypeNames(vararg args: String) {
        qualifiers += listOf(*args)
    }

    fun withDefiningSupertypes(vararg args: String) {
        definingSupertypes += listOf(*args)
    }

    fun withNames(vararg args: String) {
        names += listOf(*args)
    }

    fun withArguments(vararg args: String) {
        withArguments(args.map {
            if (it.startsWith(VARARG_PREFIX)) {
                ArgumentMatcher(typeName = it.substring(VARARG_PREFIX.length), isVararg = true)
            } else {
                ArgumentMatcher(typeName = it)
            }
        })
    }

    fun withArguments(vararg args: ArgumentMatcher) {
        withArguments(listOf(*args))
    }

    fun withArguments(args: List<ArgumentMatcher>) {
        arguments.add(args)
    }

    fun withNoArguments() {
        arguments.add(emptyList())
    }
}

fun FunMatcher(
    qualifier: String? = null,
    name: String? = null,
    nameRegex: Regex? = null,
    definingSupertype: String? = null,
    matchConstructor: Boolean = false,
    isDynamic: Boolean? = null,
    isExtensionFunction: Boolean? = null,
    isSuspending: Boolean? = null,
    isOperator: Boolean? = null,
    returnType: String? = null,
    block: FunMatcherBuilderContext.() -> Unit = {},
) = FunMatcherBuilderContext(
    qualifier,
    name,
    nameRegex,
    definingSupertype,
    matchConstructor,
    isDynamic,
    isExtensionFunction,
    isSuspending,
    isOperator,
    returnType,
).apply(block).run {
    val maxArgumentCount: Int =
        if (this.arguments.isEmpty()) Int.MAX_VALUE else this.arguments.maxOf { if (it.any { arg -> arg.isVararg }) Int.MAX_VALUE else it.size }
    FunMatcherImpl(
        this.qualifiers.addIfNonEmpty(this.qualifier),
        this.names.addIfNonEmpty(this.name),
        this.nameRegex,
        maxArgumentCount,
        this.arguments,
        this.definingSupertypes.addIfNonEmpty(this.definingSupertype),
        this.matchConstructor,
        this.isDynamic,
        this.isExtensionFunction,
        this.isSuspending,
        this.isOperator,
        this.returnType,
    )
}

fun ConstructorMatcher(
    typeName: String? = null,
    block: FunMatcherBuilderContext.() -> Unit = {}
) = FunMatcher(qualifier = typeName, matchConstructor = true, block = block)

infix fun KaCallableMemberCall<*, *>?.matches(funMatcher: FunMatcherImpl): Boolean {
    if (this == null) return false
    return funMatcher.matches(this)
}

private fun Set<String>.addIfNonEmpty(optionalString: String?): Set<String> =
    if (optionalString.isNullOrEmpty()) this else this + optionalString
