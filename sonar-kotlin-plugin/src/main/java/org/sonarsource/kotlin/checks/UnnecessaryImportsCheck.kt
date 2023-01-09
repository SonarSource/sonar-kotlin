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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE_UNUSED = "Remove this unused import."
private const val MESSAGE_REDUNDANT = "Remove this redundant import."
private val DELEGATES_IMPORTED_NAMES = setOf("getValue", "setValue", "provideDelegate")
private val ARRAY_ACCESS_IMPORTED_NAMES = setOf("get", "set")

@Rule(key = "S1128")
class UnnecessaryImportsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {

        val (references, arrayAccesses, kDocLinks, delegateImports, calls) = collectReferences(file)

        val bindingContext = context.bindingContext

        val unresolvedImports = context.diagnostics
            .mapNotNull { it.psiElement.getParentOfType<KtImportDirective>(false) }

        val groupedReferences = references.groupBy { reference ->
            reference.importableSimpleName()
        }

        val arrayAccessesImportsFilter by lazy { getArrayAccessImportsFilter(arrayAccesses, bindingContext) }
        val delegatesImportsFilter by lazy { getDelegatesImportsFilter(delegateImports, bindingContext) }
        val invokeCallsFilter by lazy { getInvokeCallsImportsFilter(calls, bindingContext) }

        analyzeImports(file, groupedReferences, kDocLinks, unresolvedImports, arrayAccessesImportsFilter, delegatesImportsFilter, invokeCallsFilter, context)
    }

    private fun analyzeImports(
        file: KtFile,
        groupedReferences: Map<String?, List<KtReferenceExpression>>,
        kDocLinks: Collection<String>,
        unresolvedImports: List<KtImportDirective>,
        arrayAccessesImportsFilter: (KtImportDirective) -> Boolean,
        delegateImportsFilter: (KtImportDirective) -> Boolean,
        invokeCallsFilter: (KtImportDirective) -> Boolean,
        context: KotlinFileContext
    ) = file.importDirectives.filter {
        // 0. Filter out unresolved imports, to avoid FPs in case of incomplete semantic
        it !in unresolvedImports
    }.filter { imp ->
        // 1. Filter out & report all imports that import from kotlin.* or the same package as our file
        if (imp.isImportedImplicitlyAlready(file.packageDirective?.qualifiedName)) {
            imp.importedReference?.let { context.reportIssue(it, MESSAGE_REDUNDANT) }
            false
        } else true
    }.groupBy {
        it.importedName?.asString()
    }.filterKeys { simpleName ->
        // 2. Discard all imports that could be relevant for KDocs. This is done a bit fuzzy.
        simpleName != null && simpleName !in kDocLinks
    }.flatMap { (simpleName, importsWithSameName) ->
        // 3. Discard all imports that are used in references throughout the code. With binding context if possible,
        // otherwise we over-estimate
        groupedReferences[simpleName]?.let { relevantReferences ->
            filterImportsWithSameSimpleNameByReferences(importsWithSameName, relevantReferences, context)
        } ?: importsWithSameName
    }.filter {
        // 4. Filter 'get', 'set', 'invoke' and delegates imports
        arrayAccessesImportsFilter(it) && delegateImportsFilter(it) && invokeCallsFilter(it)
    }.forEach { importDirective ->
        // We could not find any usages for anything remaining at this point. Hence, report!
        importDirective.importedReference?.let { context.reportIssue(it, MESSAGE_UNUSED) }
    }

    private fun filterImportsWithSameSimpleNameByReferences(
        importsWithSameName: List<KtImportDirective>,
        relevantReferences: List<KtReferenceExpression>,
        context: KotlinFileContext
    ): List<KtImportDirective> {
        var relevantImports = importsWithSameName
        for (ref in relevantReferences) {
            val refDescriptor = context.bindingContext.get(BindingContext.REFERENCE_TARGET, ref)?.getImportableDescriptor()
                ?: return emptyList() // Discard all: over-estimate, resulting in less FPs and more FNs without binding ctx
            val refName = refDescriptor.fqNameOrNull() ?: return emptyList()

            relevantImports = relevantImports.filter {
                it.importedFqName != refName && !(refDescriptor.isCompanionObject() && it.importedFqName == refName.parent())
            }

            if (relevantImports.isEmpty()) break
        }
        return relevantImports
    }

    private fun collectReferences(file: KtFile) =
        file.children.asSequence().filter {
            it !is KtPackageDirective && it !is KtImportList
        }.let { relevantTopLevelChildren ->
            DataCollector(file).apply {
                relevantTopLevelChildren
                    .filterIsInstance<KtElement>()
                    .forEach { ktElement -> ktElement.accept(this) }

                collectFromKDocComments(relevantTopLevelChildren.flatMap { it.collectDescendantsOfType<KDocLink>().asSequence() })
            }
        }.result

    private fun getArrayAccessImportsFilter(arrayAccesses: Collection<KtArrayAccessExpression>, bindingContext: BindingContext) =
        if (bindingContext == BindingContext.EMPTY) {
            { imp: KtImportDirective -> imp.importedName?.asString() !in ARRAY_ACCESS_IMPORTED_NAMES }
        } else {
            arrayAccesses.map {
                it.getResolvedCall(bindingContext)?.resultingDescriptor?.fqNameOrNull()
            }.let { resolvedArrayAccesses ->
                { imp: KtImportDirective ->
                    imp.importedFqName !in resolvedArrayAccesses
                }
            }
        }

    private fun getDelegatesImportsFilter(propertyDelegates: Collection<KtPropertyDelegate>, bindingContext: BindingContext) =
        if (bindingContext == BindingContext.EMPTY) {
            { imp: KtImportDirective -> imp.importedName?.asString() !in DELEGATES_IMPORTED_NAMES }
        } else {
            propertyDelegates.flatMap { propDelegate ->
                bindingContext.get(BindingContext.DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, propDelegate.expression)
                    .getResolvedCall(bindingContext)
                    ?.resultingDescriptor
                    ?.fqNameOrNull()?.let { listOf(it) }
                    ?: (propDelegate.parent as? KtProperty)?.let { ktProperty ->
                        (bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, ktProperty) as? PropertyDescriptor)?.let { propDescriptor ->
                            propDescriptor
                                .accessors
                                .mapNotNull {
                                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, it)
                                        ?.resultingDescriptor
                                        ?.fqNameOrNull()
                                }
                        }
                    } ?: emptyList()
            }.let { delegateImports ->
                { imp: KtImportDirective ->
                    imp.importedFqName !in delegateImports
                }
            }
        }

    private fun getInvokeCallsImportsFilter(calls: Collection<KtCallExpression>, bindingContext: BindingContext) =
        if (bindingContext == BindingContext.EMPTY) {
            { imp: KtImportDirective -> imp.importedName?.asString() != "invoke" }
        } else {
            // Lazy because we don't want to do this operation unless we find at least one "invoke" import
            val callsFqn by lazy(LazyThreadSafetyMode.NONE) {
                calls.mapNotNull { it.getResolvedCall(bindingContext)?.resultingDescriptor?.fqNameOrNull() }
            }; // ';' is mandatory here
            { imp: KtImportDirective ->
                if (imp.importedName?.asString() != "invoke") true
                else {
                    imp.importedFqName !in callsFqn
                }
            }
        }
}

private class DataCollector(val file: KtFile) : KtTreeVisitorVoid() {

    data class DataCollectionResult(
        val references: Collection<KtReferenceExpression>,
        val arrayAccesses: Collection<KtArrayAccessExpression>,
        val kDocLinks: Collection<String>,
        val delegateImports: Collection<KtPropertyDelegate>,
        val calls: Collection<KtCallExpression>
    )

    private val references = mutableListOf<KtReferenceExpression>()
    private val arrayAccesses = mutableListOf<KtArrayAccessExpression>()
    private val kDocLinks = mutableSetOf<String>()
    private val delegateImports = mutableSetOf<KtPropertyDelegate>()
    private val calls = mutableSetOf<KtCallExpression>()

    val result: DataCollectionResult
        get() = DataCollectionResult(references, arrayAccesses, kDocLinks, delegateImports, calls)

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        expression.takeUnless {
            it.children.isNotEmpty() || it.isQualifiedUserType()
        }?.let {
            references.add(it)
        }
        recurse(expression)
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        arrayAccesses.add(expression)
        recurse(expression)
    }

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        delegateImports.add(delegate)
        recurse(delegate)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        calls.add(expression)
        recurse(expression)
    }

    private fun recurse(element: KtElement) = super.visitElement(element)

    fun collectFromKDocComments(comments: Sequence<KDocLink>) {
        comments.forEach { kDocLinks.add(it.getLinkText().substringBefore('.')) }
    }
}

private fun KtReferenceExpression.isQualifiedUserType() = (this.context as? KtUserType)?.qualifier != null

private fun KtImportDirective.isImportedImplicitlyAlready(containingPackage: String?) =
    (this.importedName != null && this.importedFqName?.parent()?.asString()?.let { it == "kotlin" || it == containingPackage } ?: false) ||
        (this.importedName == null && this.importedFqName?.asString() == "kotlin")

private fun KtReferenceExpression.importableSimpleName() =
    when (this) {
        is KtOperationReferenceExpression ->
            operationSignTokenType?.let { token ->
                if (deparenthesize().parent is KtPrefixExpression) OperatorConventions.UNARY_OPERATION_NAMES[token]
                else OperatorConventions.getNameForOperationSymbol(token)
            }
                ?.asString()
                ?: getReferencedName()
        is KtSimpleNameExpression -> getReferencedName()
        else -> null
    }
