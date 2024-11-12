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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

private const val SQLITE = "net.sqlcipher.database.SQLiteDatabase"
private const val ENCRYPTION_KEY = "encryptionKey"
private const val CHANGE_PASSWORD = "changePassword"
private val CREATE_CHAR_BYTE_ARRAY = FunMatcher(qualifier = "kotlin") { withNames("byteArrayOf", "charArrayOf") }

@Rule(key = "S6301")
class MobileDatabaseEncryptionKeysCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        ConstructorMatcher(typeName = SQLITE),
        FunMatcher(qualifier = SQLITE) {
            withNames(CHANGE_PASSWORD, "openDatabase", "openOrCreateDatabase", "create")
        },
        FunMatcher(qualifier = "io.realm.RealmConfiguration.Builder", name = ENCRYPTION_KEY)
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val symbol = resolvedCall.partiallyAppliedSymbol.symbol
        val functionName = if (symbol is KaConstructorSymbol) "<init>" else symbol.name?.asString() ?: return
        val valueArgumentsList = resolvedCall.argumentMapping.keys.toList()
        if (valueArgumentsList.size < 2 && functionName != ENCRYPTION_KEY) return

        val arg = if (functionName in setOf(ENCRYPTION_KEY, CHANGE_PASSWORD)) {
            valueArgumentsList[0]
        } else valueArgumentsList[1]

        val secondaries = mutableListOf<PsiElement>()
        val argValueExpr = arg.predictRuntimeValueExpression(secondaries)
        if (argValueExpr.isHardCoded(secondaries)) {
            val parameter = if (functionName == ENCRYPTION_KEY) ENCRYPTION_KEY else "password"

            kotlinFileContext.reportIssue(
                arg,
                """The "$parameter" parameter should not be hardcoded.""",
                secondaries.map { SecondaryLocation(kotlinFileContext.textRange(it)) },
            )
        }
    }
}

private fun KtElement.isHardCoded(secondaries: MutableList<PsiElement>): Boolean =
    when (this) {
        is KtStringTemplateExpression -> true
        is KtParenthesizedExpression ->
            deparenthesize().apply { secondaries.add(this) }.isHardCoded(secondaries)
        is KtDotQualifiedExpression ->
            (selectorExpression?.isHardCoded(secondaries) ?: false)
                || receiverExpression
                .predictRuntimeValueExpression(secondaries)
                .isHardCoded(secondaries)
        is KtCallExpression ->
            if (CREATE_CHAR_BYTE_ARRAY.matches(this)) {
                secondaries.add(calleeExpression!!)
                true
            } else returnsHardcoded(secondaries)
        else -> false
    }

fun KtCallExpression.returnsHardcoded(secondaries: MutableList<PsiElement>): Boolean =
    analyze {
        val resultingDescriptor = this@returnsHardcoded.resolveToCall()?.successfulFunctionCallOrNull() ?: return false
        val declaration = resultingDescriptor.partiallyAppliedSymbol.symbol.psi as? KtNamedFunction ?: return false

        if (!declaration.hasBody()) return false
        return if (declaration.hasBlockBody()) {
            declaration.anyDescendantOfType<KtReturnExpression> {
                it.returnedExpression?.isHardCoded(secondaries) ?: false
            }
        } else declaration.bodyExpression?.isHardCoded(secondaries) ?: false
    }
