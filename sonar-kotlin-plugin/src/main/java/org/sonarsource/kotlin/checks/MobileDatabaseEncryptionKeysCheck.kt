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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext
    ) {
        val bindingContext = kotlinFileContext.bindingContext
        val functionName = resolvedCall.resultingDescriptor.name.asString()

        val valueArgumentsList = resolvedCall.valueArgumentsByIndex
        if (valueArgumentsList == null || (valueArgumentsList.size < 2 && functionName != ENCRYPTION_KEY)) return

        val arg = if (functionName in setOf(ENCRYPTION_KEY, CHANGE_PASSWORD)) {
            valueArgumentsList[0]
        } else valueArgumentsList[1]
        val argExpr = (arg as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression() ?: return

        val secondaries = mutableListOf<PsiElement>()
        val argValueExpr = argExpr.predictRuntimeValueExpression(bindingContext, secondaries)
        if (argValueExpr.isHardCoded(bindingContext, secondaries)) {
            val parameter = if (functionName == ENCRYPTION_KEY) ENCRYPTION_KEY else "password"

            kotlinFileContext.reportIssue(
                argExpr,
                """The "$parameter" parameter should not be hardcoded.""",
                secondaries.map { SecondaryLocation(kotlinFileContext.textRange(it)) },
            )
        }
    }
}

private fun KtElement.isHardCoded(bindingContext: BindingContext, secondaries: MutableList<PsiElement>): Boolean =
    when (this) {
        is KtStringTemplateExpression -> true
        is KtParenthesizedExpression ->
            deparenthesize().apply { secondaries.add(this) }.isHardCoded(bindingContext, secondaries)
        is KtDotQualifiedExpression ->
            (selectorExpression?.isHardCoded(bindingContext, secondaries) ?: false)
                || receiverExpression
                .predictRuntimeValueExpression(bindingContext, secondaries)
                .isHardCoded(bindingContext, secondaries)
        is KtCallExpression ->
            if (CREATE_CHAR_BYTE_ARRAY.matches(this, bindingContext)) {
                secondaries.add(calleeExpression!!)
                true
            } else returnsHardcoded(bindingContext, secondaries)
        else -> false
    }

fun KtCallExpression.returnsHardcoded(bindingContext: BindingContext, secondaries: MutableList<PsiElement>) : Boolean {
    val resultingDescriptor = this.getResolvedCall(bindingContext)?.resultingDescriptor ?: return false
    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(resultingDescriptor) as? KtNamedFunction ?: return false

    if (!declaration.hasBody()) return false
    return if (declaration.hasBlockBody()) {
        declaration.anyDescendantOfType<KtReturnExpression> {
            it.returnedExpression?.isHardCoded(bindingContext, secondaries) ?: false
        }
    } else declaration.bodyExpression?.isHardCoded(bindingContext, secondaries) ?: false
}
