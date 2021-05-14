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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class FunMatcher(
    var type: String? = null,
    var names: List<String> = emptyList(),
    arguments: List<List<String>> = mutableListOf(),
    var supertype: String? = null,
    private val matchConstructor: Boolean = false
) {
    private val arguments: MutableList<List<String>> = arguments.toMutableList()

    fun matches(node: KtCallExpression, bindingContext: BindingContext): Boolean {
        val call = node.getCall(bindingContext)
        val functionDescriptor = bindingContext.get(BindingContext.RESOLVED_CALL, call)?.resultingDescriptor
        return checkFunctionDescriptor(functionDescriptor)
    }

    fun matches(node: KtNamedFunction, bindingContext: BindingContext): Boolean {
        val functionDescriptor = bindingContext.get(BindingContext.FUNCTION, node)
        return checkFunctionDescriptor(functionDescriptor)
    }

    fun matches(call: Call, bindingContext: BindingContext): Boolean {
        val functionDescriptor = bindingContext.get(BindingContext.RESOLVED_CALL, call)?.resultingDescriptor
        return checkFunctionDescriptor(functionDescriptor)
    }

    private fun checkFunctionDescriptor(functionDescriptor: CallableDescriptor?) =
        checkName(functionDescriptor) &&
            checkTypeOrSupertype(functionDescriptor) &&
            checkCallParameters(functionDescriptor)

    private fun checkTypeOrSupertype(functionDescriptor: CallableDescriptor?) =
        checkType(functionDescriptor) ||
            type.isNullOrEmpty() && supertype.isNullOrEmpty() ||
            type.isNullOrEmpty() && checkSubType(functionDescriptor)

    private fun checkType(functionDescriptor: CallableDescriptor?): Boolean =
        type?.let {
            when (functionDescriptor) {
                is ConstructorDescriptor ->
                    functionDescriptor.constructedClass.fqNameSafe.asString() == type
                else ->
                    functionDescriptor?.fqNameSafe?.asString()?.substringBeforeLast(".") == type
            }
        } ?: false

    private fun checkSubType(functionDescriptor: CallableDescriptor?): Boolean =
        when (functionDescriptor) {
            is ConstructorDescriptor -> false
            else -> {
                functionDescriptor?.overriddenDescriptors?.any {
                    it?.fqNameSafe?.asString()?.substringBeforeLast(".") == supertype
                }
            }
        } ?: false

    private fun checkName(functionDescriptor: CallableDescriptor?): Boolean =
        if (functionDescriptor is ConstructorDescriptor) {
            matchConstructor
        } else {
            names.any {
                it == functionDescriptor?.name?.asString()
            }
        }

    private fun checkCallParameters(descriptor: CallableDescriptor?): Boolean {
        val valueParameters: List<ValueParameterDescriptor> =
            descriptor?.valueParameters ?: emptyList()

        return arguments.isEmpty() || arguments.any {
            (valueParameters.size == it.size) &&
                it.foldRightIndexed(true) { i, argType, acc ->
                    acc && valueParameters[i].typeAsString() == argType
                }
        }
    }

    fun withArguments(vararg args: String) {
        arguments.add(listOf(*args))
    }

    fun withNoArguments() {
        arguments.add(emptyList())
    }

    private fun ValueParameterDescriptor.typeAsString() = this.type.toString()
}

fun FunMatcher(block: FunMatcher.() -> Unit) =
    FunMatcher().apply(block)

fun ConstructorMatcher(typeName: String? = null, arguments: List<List<String>> = emptyList(), block: FunMatcher.() -> Unit = {}) =
    FunMatcher(type = typeName, arguments = arguments, matchConstructor = true).apply(block)
