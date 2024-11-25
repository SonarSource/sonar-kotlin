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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny

internal const val REF_NAME_DEPENDENCIES = "dependencies"

internal val REF_NAME_DEPENDENCY_HANDLER_SCOPE_EXTENSIONS = setOf(
    "annotationProcessor",
    "compile",
    "compileClasspath",
    "compileOnly",
    "implementation",
    "runtime",
    "runtimeClasspath",
    "runtimeOnly",
    "testAnnotationProcessor",
    "testCompile",
    "testCompileClasspath",
    "testCompileOnly",
    "testImplementation",
    "testRuntime",
    "testRuntimeClasspath",
    "testRuntimeOnly",
)

internal fun getFunctionName(expression: KtCallExpression) =
    (expression.getCalleeExpressionIfAny() as? KtNameReferenceExpression)?.getReferencedName()

internal fun getLambdaBlock(expression: KtCallExpression): KtBlockExpression? {
    val lambdaArg = expression.valueArguments.lastOrNull() as? KtLambdaArgument
    return lambdaArg?.getLambdaExpression()?.bodyExpression
}
