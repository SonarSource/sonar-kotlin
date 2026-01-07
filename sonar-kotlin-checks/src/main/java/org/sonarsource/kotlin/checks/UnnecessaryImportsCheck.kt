/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE_UNUSED = "Remove this unused import."
private const val MESSAGE_REDUNDANT = "Remove this redundant import."
private val DELEGATES_IMPORTED_NAMES = setOf("getValue", "setValue", "provideDelegate")
private val ARRAY_ACCESS_IMPORTED_NAMES = setOf("get", "set")

@Rule(key = "S1128")
class UnnecessaryImportsCheck : AbstractCheck() {

    @OptIn(KaIdeApi::class)
    override fun visitKtFile(file: KtFile, context: KotlinFileContext) = withKaSession {
        val importOptimizer = analyzeImportsToOptimize(file)
        file.importDirectives.asSequence().filter { importDirective ->
            // 1. Filter out & report all imports that import from kotlin.* or the same package as our file
            if (importDirective.isImportedImplicitlyAlready(file.packageDirective?.qualifiedName)) {
                importDirective.importedReference?.let { context.reportIssue(it, MESSAGE_REDUNDANT) }
                false
            } else true
        }.mapNotNull { importDirective: KtImportDirective ->
            importDirective.importedFqName?.let { importDirective to it }
        }.filter { (importDirective, importedFqName: FqName) ->
            if (importDirective.isAllUnder) return@filter false
            val importedName: Name =
                if (importDirective.aliasName != null)
                    Name.identifier(importDirective.aliasName!!)
                else
                    importedFqName.shortName()
            !importOptimizer.unresolvedNames.contains(importedName) &&
                    !OperatorConventions.isConventionName(importedName) &&
                    importedName.asString() != "provideDelegate" &&
                    importOptimizer.usedDeclarations[importedFqName].isNullOrEmpty()
        }.map { it.first }.forEach { importDirective ->
            // We could not find any usages for anything remaining at this point. Hence, report!
            importDirective.importedReference?.let { context.reportIssue(it, MESSAGE_UNUSED) }
        }
    }

}

private fun KtImportDirective.isImportedImplicitlyAlready(containingPackage: String?) =
    (this.importedName != null && this.importedFqName?.parent()?.asString()?.let { it == "kotlin" || it == containingPackage } ?: false) ||
        (this.importedName == null && this.importedFqName?.asString() == "kotlin")
