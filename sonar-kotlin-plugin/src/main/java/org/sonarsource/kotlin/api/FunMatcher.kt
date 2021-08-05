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
package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

class FunMatcher(
    // In case of Top Level function there is no type there,
    // so you just need to specify the package
    var qualifier: String? = null,
    name: String? = null,
    arguments: List<List<ArgumentMatcher>> = mutableListOf(),
    var supertype: String? = null,
    private val matchConstructor: Boolean = false,
    var dynamic: Boolean? = null,
    var extensionFunction: Boolean? = null,
    var suspending: Boolean? = null,
    var returnType: String? = null,
    block: FunMatcher.() -> Unit = {},
) {
    private val arguments = arguments.toMutableList()
    private val names = mutableSetOf<String>()

    var name: String? = name
        set(value) {
            value?.let { names += it }
            field = value
        }

    init {
        block()
        name?.let { names += it }
    }

    fun matches(node: KtCallExpression, bindingContext: BindingContext): Boolean {
        val call = node.getCall(bindingContext)
        val functionDescriptor = bindingContext.get(BindingContext.RESOLVED_CALL, call)?.resultingDescriptor
        return matches(functionDescriptor)
    }

    fun matches(node: KtNamedFunction, bindingContext: BindingContext): Boolean {
        val functionDescriptor = bindingContext.get(BindingContext.FUNCTION, node)
        return matches(functionDescriptor)
    }

    fun matches(call: Call, bindingContext: BindingContext) = matches(bindingContext.get(RESOLVED_CALL, call))

    fun matches(call: ResolvedCall<*>?) = matches(call?.resultingDescriptor)

    fun matches(functionDescriptor: CallableDescriptor?) =
        functionDescriptor != null &&
            checkIsDynamic(functionDescriptor) &&
            checkIsExtensionFunction(functionDescriptor) &&
            checkIsSuspending(functionDescriptor) &&
            checkName(functionDescriptor) &&
            checkTypeOrSupertype(functionDescriptor) &&
            checkReturnType(functionDescriptor) &&
            checkCallParameters(functionDescriptor)

    private fun checkTypeOrSupertype(functionDescriptor: CallableDescriptor) =
        qualifier.isNullOrEmpty() && supertype.isNullOrEmpty() ||
            checkType(functionDescriptor) ||
            qualifier.isNullOrEmpty() && checkSubType(functionDescriptor)

    private fun checkType(functionDescriptor: CallableDescriptor): Boolean =
        (qualifier ?: supertype)?.let {
            when (functionDescriptor) {
                is ConstructorDescriptor ->
                    functionDescriptor.constructedClass.fqNameSafe.asString() == it
                else ->
                    functionDescriptor.fqNameSafe.asString().substringBeforeLast(".") == it
            }
        } ?: false

    private fun checkSubType(functionDescriptor: CallableDescriptor): Boolean =
        when (functionDescriptor) {
            is ConstructorDescriptor -> false
            else -> {
                functionDescriptor.overriddenDescriptors.any {
                    it?.fqNameSafe?.asString()?.substringBeforeLast(".") == supertype
                }
            }
        }

    private fun checkName(functionDescriptor: CallableDescriptor): Boolean =
        if (functionDescriptor is ConstructorDescriptor) {
            matchConstructor
        } else if (!matchConstructor) {
            names.isEmpty() || functionDescriptor.name.asString() in names
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
        dynamic?.let { it == descriptor.isDynamic() } ?: true

    private fun checkIsExtensionFunction(descriptor: CallableDescriptor): Boolean =
        extensionFunction?.let { it == descriptor.isExtension } ?: true

    private fun checkReturnType(descriptor: CallableDescriptor) =
        returnType?.let { it == descriptor.returnType?.getJetTypeFqName(false) } ?: true

    private fun checkIsSuspending(descriptor: CallableDescriptor) =
        suspending?.let { it == descriptor.isSuspend } ?: true

    fun withNames(vararg args: String) {
        names += listOf(*args)
    }

    fun withArguments(vararg args: String) {
        withArguments(args.map { ArgumentMatcher(it) })
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

fun ConstructorMatcher(typeName: String? = null, arguments: List<List<ArgumentMatcher>> = emptyList(), block: FunMatcher.() -> Unit = {}) =
    FunMatcher(qualifier = typeName, arguments = arguments, matchConstructor = true, block = block)

infix fun ResolvedCall<*>?.matches(funMatcher: FunMatcher): Boolean = funMatcher.matches(this)
