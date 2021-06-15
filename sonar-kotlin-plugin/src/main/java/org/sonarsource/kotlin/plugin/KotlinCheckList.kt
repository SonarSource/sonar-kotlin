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
package org.sonarsource.kotlin.plugin

import org.sonarsource.kotlin.checks.AllBranchesIdenticalCheck
import org.sonarsource.kotlin.checks.BadClassNameCheck
import org.sonarsource.kotlin.checks.BadFunctionNameCheck
import org.sonarsource.kotlin.checks.BooleanInversionCheck
import org.sonarsource.kotlin.checks.BooleanLiteralCheck
import org.sonarsource.kotlin.checks.CipherBlockChainingCheck
import org.sonarsource.kotlin.checks.ClearTextProtocolCheck
import org.sonarsource.kotlin.checks.CodeAfterJumpCheck
import org.sonarsource.kotlin.checks.CollapsibleIfStatementsCheck
import org.sonarsource.kotlin.checks.CommentedCodeCheck
import org.sonarsource.kotlin.checks.DataHashingCheck
import org.sonarsource.kotlin.checks.DuplicateBranchCheck
import org.sonarsource.kotlin.checks.DuplicatedFunctionImplementationCheck
import org.sonarsource.kotlin.checks.ElseIfWithoutElseCheck
import org.sonarsource.kotlin.checks.EmptyBlockCheck
import org.sonarsource.kotlin.checks.EmptyCommentCheck
import org.sonarsource.kotlin.checks.EmptyFunctionCheck
import org.sonarsource.kotlin.checks.EncryptionAlgorithmCheck
import org.sonarsource.kotlin.checks.FileHeaderCheck
import org.sonarsource.kotlin.checks.FixMeCommentCheck
import org.sonarsource.kotlin.checks.FunctionCognitiveComplexityCheck
import org.sonarsource.kotlin.checks.HardcodedCredentialsCheck
import org.sonarsource.kotlin.checks.HardcodedIpCheck
import org.sonarsource.kotlin.checks.IdenticalBinaryOperandCheck
import org.sonarsource.kotlin.checks.IdenticalConditionsCheck
import org.sonarsource.kotlin.checks.IfConditionalAlwaysTrueOrFalseCheck
import org.sonarsource.kotlin.checks.MatchCaseTooBigCheck
import org.sonarsource.kotlin.checks.NestedMatchCheck
import org.sonarsource.kotlin.checks.OneStatementPerLineCheck
import org.sonarsource.kotlin.checks.PseudoRandomCheck
import org.sonarsource.kotlin.checks.RedundantParenthesesCheck
import org.sonarsource.kotlin.checks.RobustCryptographicKeysCheck
import org.sonarsource.kotlin.checks.SelfAssignmentCheck
import org.sonarsource.kotlin.checks.ServerCertificateCheck
import org.sonarsource.kotlin.checks.StringLiteralDuplicatedCheck
import org.sonarsource.kotlin.checks.StrongCipherAlgorithmCheck
import org.sonarsource.kotlin.checks.TabsCheck
import org.sonarsource.kotlin.checks.TodoCommentCheck
import org.sonarsource.kotlin.checks.TooComplexExpressionCheck
import org.sonarsource.kotlin.checks.TooDeeplyNestedStatementsCheck
import org.sonarsource.kotlin.checks.TooLongFunctionCheck
import org.sonarsource.kotlin.checks.TooLongLineCheck
import org.sonarsource.kotlin.checks.TooManyCasesCheck
import org.sonarsource.kotlin.checks.TooManyLinesOfCodeFileCheck
import org.sonarsource.kotlin.checks.TooManyParametersCheck
import org.sonarsource.kotlin.checks.UnusedFunctionParameterCheck
import org.sonarsource.kotlin.checks.UnusedLocalVariableCheck
import org.sonarsource.kotlin.checks.UnusedPrivateMethodCheck
import org.sonarsource.kotlin.checks.VariableAndParameterNameCheck
import org.sonarsource.kotlin.checks.VerifiedServerHostnamesCheck
import org.sonarsource.kotlin.checks.WeakSSLContextCheck
import org.sonarsource.kotlin.checks.WrongAssignmentOperatorCheck
import org.sonarsource.slang.checks.CheckList
import org.sonarsource.slang.checks.MatchWithoutElseCheck
import org.sonarsource.slang.checks.OctalValuesCheck

object KotlinCheckList {
    val SLANG_EXCLUDED_CHECKS =
        arrayOf<Class<*>>(
            // FP rate too high for now in Kotlin on 'when' statements due to enum/sealed class that have all branches covered
            MatchWithoutElseCheck::class.java,
            // Rule does not apply here as octal values do not exist in Kotlin
            OctalValuesCheck::class.java,

            org.sonarsource.slang.checks.AllBranchesIdenticalCheck::class.java,
            org.sonarsource.slang.checks.BadClassNameCheck::class.java,
            org.sonarsource.slang.checks.BadFunctionNameCheck::class.java,
            org.sonarsource.slang.checks.BooleanInversionCheck::class.java,
            org.sonarsource.slang.checks.BooleanLiteralCheck::class.java,
            org.sonarsource.slang.checks.CodeAfterJumpCheck::class.java,
            org.sonarsource.slang.checks.CollapsibleIfStatementsCheck::class.java,
            org.sonarsource.slang.checks.DuplicateBranchCheck::class.java,
            org.sonarsource.slang.checks.DuplicatedFunctionImplementationCheck::class.java,
            org.sonarsource.slang.checks.ElseIfWithoutElseCheck::class.java,
            org.sonarsource.slang.checks.EmptyBlockCheck::class.java,
            org.sonarsource.slang.checks.EmptyCommentCheck::class.java,
            org.sonarsource.slang.checks.EmptyFunctionCheck::class.java,
            org.sonarsource.slang.checks.FileHeaderCheck::class.java,
            org.sonarsource.slang.checks.FixMeCommentCheck::class.java,
            org.sonarsource.slang.checks.FunctionCognitiveComplexityCheck::class.java,
            org.sonarsource.slang.checks.HardcodedCredentialsCheck::class.java,
            org.sonarsource.slang.checks.HardcodedIpCheck::class.java,
            org.sonarsource.slang.checks.IdenticalBinaryOperandCheck::class.java,
            org.sonarsource.slang.checks.IdenticalConditionsCheck::class.java,
            org.sonarsource.slang.checks.IfConditionalAlwaysTrueOrFalseCheck::class.java,
            org.sonarsource.slang.checks.MatchCaseTooBigCheck::class.java,
            org.sonarsource.slang.checks.NestedMatchCheck::class.java,
            org.sonarsource.slang.checks.OneStatementPerLineCheck::class.java,
            org.sonarsource.slang.checks.RedundantParenthesesCheck::class.java,
            org.sonarsource.slang.checks.SelfAssignmentCheck::class.java,
            org.sonarsource.slang.checks.StringLiteralDuplicatedCheck::class.java,
            org.sonarsource.slang.checks.TabsCheck::class.java,
            org.sonarsource.slang.checks.TodoCommentCheck::class.java,
            org.sonarsource.slang.checks.TooComplexExpressionCheck::class.java,
            org.sonarsource.slang.checks.TooDeeplyNestedStatementsCheck::class.java,
            org.sonarsource.slang.checks.TooLongFunctionCheck::class.java,
            org.sonarsource.slang.checks.TooLongLineCheck::class.java,
            org.sonarsource.slang.checks.TooManyCasesCheck::class.java,
            org.sonarsource.slang.checks.TooManyLinesOfCodeFileCheck::class.java,
            org.sonarsource.slang.checks.TooManyParametersCheck::class.java,
            org.sonarsource.slang.checks.UnusedFunctionParameterCheck::class.java,
            org.sonarsource.slang.checks.UnusedLocalVariableCheck::class.java,
            org.sonarsource.slang.checks.UnusedPrivateMethodCheck::class.java,
            org.sonarsource.slang.checks.VariableAndParameterNameCheck::class.java,
            org.sonarsource.slang.checks.WrongAssignmentOperatorCheck::class.java,
        )

    private val KOTLIN_CHECKS = listOf(
        AllBranchesIdenticalCheck::class.java,
        BadClassNameCheck::class.java,
        BadFunctionNameCheck::class.java,
        BooleanInversionCheck::class.java,
        BooleanLiteralCheck::class.java,
        CipherBlockChainingCheck::class.java,
        ClearTextProtocolCheck::class.java,
        CodeAfterJumpCheck::class.java,
        CollapsibleIfStatementsCheck::class.java,
        CommentedCodeCheck::class.java,
        DataHashingCheck::class.java,
        DuplicateBranchCheck::class.java,
        DuplicatedFunctionImplementationCheck::class.java,
        ElseIfWithoutElseCheck::class.java,
        EmptyBlockCheck::class.java,
        EmptyCommentCheck::class.java,
        EmptyFunctionCheck::class.java,
        EncryptionAlgorithmCheck::class.java,
        FileHeaderCheck::class.java,
        FixMeCommentCheck::class.java,
        FunctionCognitiveComplexityCheck::class.java,
        HardcodedCredentialsCheck::class.java,
        HardcodedIpCheck::class.java,
        IdenticalBinaryOperandCheck::class.java,
        IdenticalConditionsCheck::class.java,
        IfConditionalAlwaysTrueOrFalseCheck::class.java,
        MatchCaseTooBigCheck::class.java,
        NestedMatchCheck::class.java,
        OneStatementPerLineCheck::class.java,
        RedundantParenthesesCheck::class.java,
        PseudoRandomCheck::class.java,
        RobustCryptographicKeysCheck::class.java,
        SelfAssignmentCheck::class.java,
        ServerCertificateCheck::class.java,
        StringLiteralDuplicatedCheck::class.java,
        StrongCipherAlgorithmCheck::class.java,
        TabsCheck::class.java,
        TodoCommentCheck::class.java,
        TooComplexExpressionCheck::class.java,
        TooDeeplyNestedStatementsCheck::class.java,
        TooLongFunctionCheck::class.java,
        TooLongLineCheck::class.java,
        TooManyCasesCheck::class.java,
        TooManyLinesOfCodeFileCheck::class.java,
        TooManyParametersCheck::class.java,
        UnusedFunctionParameterCheck::class.java,
        UnusedLocalVariableCheck::class.java,
        UnusedPrivateMethodCheck::class.java,
        VariableAndParameterNameCheck::class.java,
        WeakSSLContextCheck::class.java,
        WrongAssignmentOperatorCheck::class.java,
        VerifiedServerHostnamesCheck::class.java,
    )

    @Deprecated("Use Kotlin-native checks instead")
    fun legacyChecks() = CheckList.excludeChecks(SLANG_EXCLUDED_CHECKS)

    fun checks() = KOTLIN_CHECKS
}
