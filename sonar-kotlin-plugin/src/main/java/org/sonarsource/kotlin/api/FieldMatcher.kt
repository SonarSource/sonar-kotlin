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
package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

fun interface FieldMatcher {
    fun matches(node: KtNameReferenceExpression, bindingContext: BindingContext): Boolean
}

interface FieldMatcherBuilder {
    fun withNames(vararg args: String)
    fun withQualifiers(vararg args: String)
    fun withDefiningTypes(vararg args: String)
}

fun FieldMatcher(initializer: FieldMatcherBuilder.() -> Unit): FieldMatcher =
    FieldMatcherBuilderImpl().apply(initializer).build()

private class FieldMatcherBuilderImpl: FieldMatcherBuilder {

    private var names: Set<String> = emptySet()
    private var qualifiers: Set<String> = emptySet()
    private var definingTypes: Set<String> = emptySet()

    override fun withNames(vararg args: String) {
        names += listOf(*args)
    }

    override fun withDefiningTypes(vararg args: String) {
        definingTypes += listOf(*args)
    }

    override fun withQualifiers(vararg args: String) {
        qualifiers += listOf(*args)
    }

    fun build(): FieldMatcher = FieldMatcherImpl(names, qualifiers, definingTypes)
}

private class FieldMatcherImpl (
    private val names: Set<String>,
    private val qualifiers: Set<String>,
    private val definingTypes: Set<String>
): FieldMatcher {

    override fun matches(node: KtNameReferenceExpression, bindingContext: BindingContext) =
        (names.isEmpty() || names.contains(node.getReferencedName())) &&
            matchesQualifiers(node, bindingContext) &&
            matchesDefiningType(node, bindingContext)

    private fun matchesQualifiers(node: KtNameReferenceExpression, bindingContext: BindingContext): Boolean {
        if (qualifiers.isEmpty()) return true
        val receiverClassDescriptor = node.getContainingDeclaration(bindingContext) ?: return false
        return qualifiers.contains(receiverClassDescriptor.fqNameSafe.asString())
    }

    private fun matchesDefiningType(node: KtNameReferenceExpression, bindingContext: BindingContext): Boolean {
        if (definingTypes.isEmpty()) return true
        val receiverClassDescriptor = node.getContainingDeclaration(bindingContext) as? ClassifierDescriptor ?: return false
        return definingTypes.contains(receiverClassDescriptor.fqNameSafe.asString()) ||
            receiverClassDescriptor.getAllSuperClassifiers().toList().any { definingTypes.contains(it.fqNameSafe.asString()) }
    }
}
