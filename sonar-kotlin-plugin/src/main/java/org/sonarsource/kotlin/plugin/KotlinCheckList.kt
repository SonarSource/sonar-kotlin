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
package org.sonarsource.kotlin.plugin

import org.sonarsource.kotlin.checks.AllBranchesIdenticalCheck
import org.sonarsource.kotlin.checks.AnchorPrecedenceCheck
import org.sonarsource.kotlin.checks.AndroidBroadcastingCheck
import org.sonarsource.kotlin.checks.ArrayHashCodeAndToStringCheck
import org.sonarsource.kotlin.checks.AuthorisingNonAuthenticatedUsersCheck
import org.sonarsource.kotlin.checks.BadClassNameCheck
import org.sonarsource.kotlin.checks.BadFunctionNameCheck
import org.sonarsource.kotlin.checks.BiometricAuthWithoutCryptoCheck
import org.sonarsource.kotlin.checks.BooleanInversionCheck
import org.sonarsource.kotlin.checks.BooleanLiteralCheck
import org.sonarsource.kotlin.checks.CipherBlockChainingCheck
import org.sonarsource.kotlin.checks.CipherModeOperationCheck
import org.sonarsource.kotlin.checks.ClearTextProtocolCheck
import org.sonarsource.kotlin.checks.CodeAfterJumpCheck
import org.sonarsource.kotlin.checks.CollapsibleIfStatementsCheck
import org.sonarsource.kotlin.checks.CollectionCallingItselfCheck
import org.sonarsource.kotlin.checks.CollectionInappropriateCallsCheck
import org.sonarsource.kotlin.checks.CollectionSizeAndArrayLengthCheck
import org.sonarsource.kotlin.checks.CommentedCodeCheck
import org.sonarsource.kotlin.checks.CoroutineScopeFunSuspendingCheck
import org.sonarsource.kotlin.checks.CoroutinesTimeoutApiUnusedCheck
import org.sonarsource.kotlin.checks.DataHashingCheck
import org.sonarsource.kotlin.checks.DebugFeatureEnabledCheck
import org.sonarsource.kotlin.checks.DeprecatedCodeCheck
import org.sonarsource.kotlin.checks.DeprecatedCodeUsedCheck
import org.sonarsource.kotlin.checks.DuplicateBranchCheck
import org.sonarsource.kotlin.checks.DuplicatedFunctionImplementationCheck
import org.sonarsource.kotlin.checks.DuplicatesInCharacterClassCheck
import org.sonarsource.kotlin.checks.ElseIfWithoutElseCheck
import org.sonarsource.kotlin.checks.EmptyBlockCheck
import org.sonarsource.kotlin.checks.EmptyCommentCheck
import org.sonarsource.kotlin.checks.EmptyFunctionCheck
import org.sonarsource.kotlin.checks.EmptyLineRegexCheck
import org.sonarsource.kotlin.checks.EmptyStringRepetitionCheck
import org.sonarsource.kotlin.checks.EncryptionAlgorithmCheck
import org.sonarsource.kotlin.checks.EqualsArgumentTypeCheck
import org.sonarsource.kotlin.checks.EqualsOverriddenWithArrayFieldCheck
import org.sonarsource.kotlin.checks.EqualsOverridenWithHashCodeCheck
import org.sonarsource.kotlin.checks.ExposedMutableFlowCheck
import org.sonarsource.kotlin.checks.ExternalAndroidStorageAccessCheck
import org.sonarsource.kotlin.checks.FileHeaderCheck
import org.sonarsource.kotlin.checks.FinalFlowOperationCheck
import org.sonarsource.kotlin.checks.FixMeCommentCheck
import org.sonarsource.kotlin.checks.FlowChannelReturningFunsNotSuspendingCheck
import org.sonarsource.kotlin.checks.FunctionCognitiveComplexityCheck
import org.sonarsource.kotlin.checks.GraphemeClustersInClassesCheck
import org.sonarsource.kotlin.checks.HardcodedCredentialsCheck
import org.sonarsource.kotlin.checks.HardcodedIpCheck
import org.sonarsource.kotlin.checks.IdenticalBinaryOperandCheck
import org.sonarsource.kotlin.checks.IdenticalConditionsCheck
import org.sonarsource.kotlin.checks.IfConditionalAlwaysTrueOrFalseCheck
import org.sonarsource.kotlin.checks.IgnoredOperationStatusCheck
import org.sonarsource.kotlin.checks.InjectableDispatchersCheck
import org.sonarsource.kotlin.checks.InvalidRegexCheck
import org.sonarsource.kotlin.checks.IsInstanceMethodCheck
import org.sonarsource.kotlin.checks.MainSafeCoroutinesCheck
import org.sonarsource.kotlin.checks.MatchCaseTooBigCheck
import org.sonarsource.kotlin.checks.MobileDatabaseEncryptionKeysCheck
import org.sonarsource.kotlin.checks.NestedMatchCheck
import org.sonarsource.kotlin.checks.OneStatementPerLineCheck
import org.sonarsource.kotlin.checks.ParsingErrorCheck
import org.sonarsource.kotlin.checks.PseudoRandomCheck
import org.sonarsource.kotlin.checks.ReceivingIntentsCheck
import org.sonarsource.kotlin.checks.RedundantParenthesesCheck
import org.sonarsource.kotlin.checks.RedundantSuspendModifierCheck
import org.sonarsource.kotlin.checks.JumpInFinallyCheck
import org.sonarsource.kotlin.checks.PreparedStatementAndResultSetCheck
import org.sonarsource.kotlin.checks.RegexComplexityCheck
import org.sonarsource.kotlin.checks.ReluctantQuantifierCheck
import org.sonarsource.kotlin.checks.ReplaceGuavaWithKotlinCheck
import org.sonarsource.kotlin.checks.RobustCryptographicKeysCheck
import org.sonarsource.kotlin.checks.RunFinalizersCheck
import org.sonarsource.kotlin.checks.ScheduledThreadPoolExecutorZeroCheck
import org.sonarsource.kotlin.checks.SelfAssignmentCheck
import org.sonarsource.kotlin.checks.ServerCertificateCheck
import org.sonarsource.kotlin.checks.StreamNotConsumedCheck
import org.sonarsource.kotlin.checks.StringLiteralDuplicatedCheck
import org.sonarsource.kotlin.checks.StrongCipherAlgorithmCheck
import org.sonarsource.kotlin.checks.StructuredConcurrencyPrinciplesCheck
import org.sonarsource.kotlin.checks.SuspendingFunCallerDispatcherCheck
import org.sonarsource.kotlin.checks.TabsCheck
import org.sonarsource.kotlin.checks.TodoCommentCheck
import org.sonarsource.kotlin.checks.TooComplexExpressionCheck
import org.sonarsource.kotlin.checks.TooDeeplyNestedStatementsCheck
import org.sonarsource.kotlin.checks.TooLongFunctionCheck
import org.sonarsource.kotlin.checks.TooLongLambdaCheck
import org.sonarsource.kotlin.checks.TooLongLineCheck
import org.sonarsource.kotlin.checks.TooManyCasesCheck
import org.sonarsource.kotlin.checks.TooManyLinesOfCodeFileCheck
import org.sonarsource.kotlin.checks.TooManyParametersCheck
import org.sonarsource.kotlin.checks.UnencryptedDatabaseOnMobileCheck
import org.sonarsource.kotlin.checks.UnencryptedFilesInMobileApplicationsCheck
import org.sonarsource.kotlin.checks.UnicodeAwareCharClassesCheck
import org.sonarsource.kotlin.checks.UnnecessaryImportsCheck
import org.sonarsource.kotlin.checks.UnpredictableHashSaltCheck
import org.sonarsource.kotlin.checks.UnpredictableSecureRandomSaltCheck
import org.sonarsource.kotlin.checks.UnusedDeferredResultCheck
import org.sonarsource.kotlin.checks.UnusedFunctionParameterCheck
import org.sonarsource.kotlin.checks.UnusedLocalVariableCheck
import org.sonarsource.kotlin.checks.UnusedPrivateMethodCheck
import org.sonarsource.kotlin.checks.UselessIncrementCheck
import org.sonarsource.kotlin.checks.VariableAndParameterNameCheck
import org.sonarsource.kotlin.checks.VerifiedServerHostnamesCheck
import org.sonarsource.kotlin.checks.ViewModelSuspendingFunctionsCheck
import org.sonarsource.kotlin.checks.WeakSSLContextCheck
import org.sonarsource.kotlin.checks.WebViewJavaScriptSupportCheck
import org.sonarsource.kotlin.checks.WebViewsFileAccessCheck
import org.sonarsource.kotlin.checks.WrongAssignmentOperatorCheck

val KOTLIN_CHECKS = listOf(
    AllBranchesIdenticalCheck::class.java,
    AnchorPrecedenceCheck::class.java,
    AndroidBroadcastingCheck::class.java,
    ArrayHashCodeAndToStringCheck::class.java,
    AuthorisingNonAuthenticatedUsersCheck::class.java,
    BadClassNameCheck::class.java,
    BadFunctionNameCheck::class.java,
    BiometricAuthWithoutCryptoCheck::class.java,
    BooleanInversionCheck::class.java,
    BooleanLiteralCheck::class.java,
    CipherBlockChainingCheck::class.java,
    CipherModeOperationCheck::class.java,
    ClearTextProtocolCheck::class.java,
    CodeAfterJumpCheck::class.java,
    CollapsibleIfStatementsCheck::class.java,
    CollectionCallingItselfCheck::class.java,
    CollectionSizeAndArrayLengthCheck::class.java,
    CollectionInappropriateCallsCheck::class.java,
    CoroutineScopeFunSuspendingCheck::class.java,
    CommentedCodeCheck::class.java,
    CoroutinesTimeoutApiUnusedCheck::class.java,
    DataHashingCheck::class.java,
    DebugFeatureEnabledCheck::class.java,
    DeprecatedCodeCheck::class.java,
    DeprecatedCodeUsedCheck::class.java,
    DuplicateBranchCheck::class.java,
    DuplicatedFunctionImplementationCheck::class.java,
    DuplicatesInCharacterClassCheck::class.java,
    ElseIfWithoutElseCheck::class.java,
    EmptyBlockCheck::class.java,
    EmptyCommentCheck::class.java,
    EmptyFunctionCheck::class.java,
    EmptyLineRegexCheck::class.java,
    EmptyStringRepetitionCheck::class.java,
    EncryptionAlgorithmCheck::class.java,
    EqualsArgumentTypeCheck::class.java,
    EqualsOverriddenWithArrayFieldCheck::class.java,
    EqualsOverridenWithHashCodeCheck::class.java,
    ExposedMutableFlowCheck::class.java,
    ExternalAndroidStorageAccessCheck::class.java,
    FileHeaderCheck::class.java,
    FinalFlowOperationCheck::class.java,
    FixMeCommentCheck::class.java,
    FlowChannelReturningFunsNotSuspendingCheck::class.java,
    FunctionCognitiveComplexityCheck::class.java,
    GraphemeClustersInClassesCheck::class.java,
    HardcodedCredentialsCheck::class.java,
    HardcodedIpCheck::class.java,
    IdenticalBinaryOperandCheck::class.java,
    IdenticalConditionsCheck::class.java,
    IfConditionalAlwaysTrueOrFalseCheck::class.java,
    IgnoredOperationStatusCheck::class.java,
    InjectableDispatchersCheck::class.java,
    InvalidRegexCheck::class.java,
    IsInstanceMethodCheck::class.java,
    JumpInFinallyCheck::class.java,
    MainSafeCoroutinesCheck::class.java,
    MatchCaseTooBigCheck::class.java,
    MobileDatabaseEncryptionKeysCheck::class.java,
    NestedMatchCheck::class.java,
    OneStatementPerLineCheck::class.java,
    ParsingErrorCheck::class.java,
    PreparedStatementAndResultSetCheck::class.java,
    PseudoRandomCheck::class.java,
    ReceivingIntentsCheck::class.java,
    RedundantParenthesesCheck::class.java,
    RedundantSuspendModifierCheck::class.java,
    RegexComplexityCheck::class.java,
    ReluctantQuantifierCheck::class.java,
    ReplaceGuavaWithKotlinCheck::class.java,
    RobustCryptographicKeysCheck::class.java,
    RunFinalizersCheck::class.java,
    ScheduledThreadPoolExecutorZeroCheck::class.java,
    SelfAssignmentCheck::class.java,
    ServerCertificateCheck::class.java,
    StreamNotConsumedCheck::class.java,
    StringLiteralDuplicatedCheck::class.java,
    StrongCipherAlgorithmCheck::class.java,
    StructuredConcurrencyPrinciplesCheck::class.java,
    SuspendingFunCallerDispatcherCheck::class.java,
    TabsCheck::class.java,
    TodoCommentCheck::class.java,
    TooComplexExpressionCheck::class.java,
    TooDeeplyNestedStatementsCheck::class.java,
    TooLongFunctionCheck::class.java,
    TooLongLambdaCheck::class.java,
    TooLongLineCheck::class.java,
    TooManyCasesCheck::class.java,
    TooManyLinesOfCodeFileCheck::class.java,
    TooManyParametersCheck::class.java,
    UnencryptedDatabaseOnMobileCheck::class.java,
    UnencryptedFilesInMobileApplicationsCheck::class.java,
    UnicodeAwareCharClassesCheck::class.java,
    UnnecessaryImportsCheck::class.java,
    UnpredictableHashSaltCheck::class.java,
    UnpredictableSecureRandomSaltCheck::class.java,
    UnusedDeferredResultCheck::class.java,
    UnusedFunctionParameterCheck::class.java,
    UnusedLocalVariableCheck::class.java,
    UnusedPrivateMethodCheck::class.java,
    UselessIncrementCheck::class.java,
    VariableAndParameterNameCheck::class.java,
    VerifiedServerHostnamesCheck::class.java,
    ViewModelSuspendingFunctionsCheck::class.java,
    WeakSSLContextCheck::class.java,
    WebViewJavaScriptSupportCheck::class.java,
    WebViewsFileAccessCheck::class.java,
    WrongAssignmentOperatorCheck::class.java,
)
