/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
