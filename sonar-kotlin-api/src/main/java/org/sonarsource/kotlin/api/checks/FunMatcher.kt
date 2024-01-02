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
package org.sonarsource.kotlin.api.checks

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence

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

    fun matches(node: KtCallExpression, bindingContext: BindingContext): Boolean {
        val call = node.getCall(bindingContext)
        return preCheckArgumentCount(call) &&
            matches(bindingContext[RESOLVED_CALL, call]?.resultingDescriptor)
    }

    fun matches(node: KtNamedFunction, bindingContext: BindingContext): Boolean {
        val functionDescriptor = bindingContext[BindingContext.FUNCTION, node]
        return matches(functionDescriptor)
    }

    fun matches(call: Call, bindingContext: BindingContext) = preCheckArgumentCount(call) &&
        matches(bindingContext[RESOLVED_CALL, call]?.resultingDescriptor)

    fun matches(resolvedCall: ResolvedCall<*>?) = preCheckArgumentCount(resolvedCall?.call) &&
        matches(resolvedCall?.resultingDescriptor)

    private fun preCheckArgumentCount(call: Call?) = call == null || call.valueArguments.size <= maxArgumentCount

    fun matches(functionDescriptor: CallableDescriptor?) =
        functionDescriptor != null &&
            checkIsDynamic(functionDescriptor) &&
            checkIsExtensionFunction(functionDescriptor) &&
            checkIsSuspending(functionDescriptor) &&
            checkIsOperator(functionDescriptor) &&
            checkName(functionDescriptor) &&
            checkTypeOrSupertype(functionDescriptor) &&
            checkReturnType(functionDescriptor) &&
            checkCallParameters(functionDescriptor)

    private fun checkTypeOrSupertype(functionDescriptor: CallableDescriptor) =
        qualifiersOrDefiningSupertypes.isEmpty() || qualifiersOrDefiningSupertypes.contains(getActualQualifier(functionDescriptor)) ||
            !definingSupertypes.isNullOrEmpty() && checkSubType(functionDescriptor)

    private fun getActualQualifier(functionDescriptor: CallableDescriptor) =
        if (functionDescriptor is ConstructorDescriptor) {
            functionDescriptor.constructedClass.fqNameSafe.asString()
        } else {
            functionDescriptor.fqNameSafe.asString().substringBeforeLast(".")
        }

    private fun checkSubType(functionDescriptor: CallableDescriptor): Boolean =
        when (functionDescriptor) {
            is ConstructorDescriptor -> false
            else -> {
                // Note: `overriddenTreeUniqueAsSequence()` will return a sequence of all overridden descriptors, even if thery are not
                // explicitly declared in the super types. In other words, if you have a class hierarchy of 10 levels, with a declaration
                // in the top-most class/interface and an overriding method in the bottom-most, it will return a sequence of 10 elements,
                // even if there are no overriding methods in the classes in-between. This can be desired but in most cases is somewhat
                // inefficient, as we mostly look for the top-most declaration here. The top-most declaration is the last element in the
                // sequence, however, so we need to iterate the entire sequence to get there. There are optimization possibilities if this
                // turns out to be a performance problem in the long run.
                functionDescriptor.overriddenTreeUniqueAsSequence(true).any {
                    definingSupertypes.contains(it.fqNameSafe.asString().substringBeforeLast("."))
                }
            }
        }

    private fun checkName(functionDescriptor: CallableDescriptor): Boolean =
        if (functionDescriptor is ConstructorDescriptor) {
            matchConstructor
        } else if (!matchConstructor) {
            val name = functionDescriptor.name.asString()
            (nameRegex == null && names.isEmpty()) || name in names || (nameRegex != null && nameRegex.matches(name))
        } else false

    private fun checkCallParameters(descriptor: CallableDescriptor): Boolean {
        val valueParameters: List<ValueParameterDescriptor> =
            descriptor.valueParameters

        return arguments.isEmpty() || arguments.any {
            (valueParameters.size == it.size) &&
                it.foldRightIndexed(true) { i, argType, acc ->
                    acc && argType.matches(valueParameters[i])
                }
        }
    }

    private fun checkIsDynamic(descriptor: CallableDescriptor): Boolean =
        isDynamic?.let { it == descriptor.isDynamic() } ?: true

    private fun checkIsExtensionFunction(descriptor: CallableDescriptor): Boolean =
        isExtensionFunction?.let { it == descriptor.isExtension } ?: true

    private fun checkReturnType(descriptor: CallableDescriptor) =
        returnType?.let {
            val kotlinType = descriptor.returnType
            if (kotlinType?.constructor?.declarationDescriptor != null) it == kotlinType.getKotlinTypeFqName(false)
            else null
        } ?: true

    private fun checkIsSuspending(descriptor: CallableDescriptor) =
        isSuspending?.let { it == descriptor.isSuspend } ?: true

    private fun checkIsOperator(descriptor: CallableDescriptor) =
        isOperator?.let { it == ((descriptor as? FunctionDescriptor)?.isOperator ?: false) } ?: true
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

infix fun ResolvedCall<*>?.matches(funMatcher: FunMatcherImpl): Boolean = funMatcher.matches(this)

private fun Set<String>.addIfNonEmpty(optionalString: String?): Set<String> =
    if (optionalString.isNullOrEmpty()) this else this + optionalString
