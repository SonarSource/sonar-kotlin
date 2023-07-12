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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message

private const val REF_NAME_DEPENDENCIES = "dependencies"
private const val REF_NAME_CONSTRAINTS = "constraints"

private val REF_NAME_DEPENDENCY_HANDLER_SCOPE_EXTENSIONS by lazy {
    setOf(
        REF_NAME_CONSTRAINTS,
        "annotationProcessor",
        "antlr",
        "api",
        "apiElements",
        "archives",
        "checkstyle",
        "codenarc",
        "coffeeScriptBasePluginJs",
        "compile",
        "compileClasspath",
        "compileOnly",
        "default",
        "deploy",
        "earlib",
        "envJsPlugin",
        "findbugs",
        "findbugsPlugins",
        "implementation",
        "jacocoAgent",
        "jacocoAnt",
        "jdepend",
        "jsHintPlugin",
        "play",
        "playPlatform",
        "playRun",
        "playTest",
        "pmd",
        "providedCompile",
        "providedRuntime",
        "rhinoPluginRhinoClasspath",
        "runtime",
        "runtimeClasspath",
        "runtimeElements",
        "runtimeOnly",
        "signatures",
        "testAnnotationProcessor",
        "testCompile",
        "testCompileClasspath",
        "testCompileOnly",
        "testImplementation",
        "testRuntime",
        "testRuntimeClasspath",
        "testRuntimeOnly",
        "zinc",
    )
}

@Rule(key = "S6629")
class DependencyGroupedCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (getFunctionNameOrNull(expression) != REF_NAME_DEPENDENCIES) return
        checkDependencyHandlerScopeLambda(expression, kotlinFileContext)
    }

    private fun checkDependencyHandlerScopeLambda(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        getLambdaBlock(expression)?.acceptChildren(DependencyHandlerScopeLambdaCheck(kotlinFileContext))
    }

    private inner class DependencyHandlerScopeLambdaCheck(
        private val kotlinFileContext: KotlinFileContext
    ) : KtVisitorVoid() {

        private var lastNameInOrder: String? = null

        private val visitedNames = mutableSetOf<String>()

        override fun visitCallExpression(expression: KtCallExpression) {
            val functionReference = expression.getCalleeExpressionIfAny() as? KtNameReferenceExpression ?: return
            val functionName = functionReference.getReferencedName()
            if (!REF_NAME_DEPENDENCY_HANDLER_SCOPE_EXTENSIONS.contains(functionName)) return

            if (functionName == REF_NAME_CONSTRAINTS) {
                checkDependencyHandlerScopeLambda(expression, kotlinFileContext)
            } else {
                checkFunctionReferenceOrder(functionReference)
            }
        }

        fun checkFunctionReferenceOrder(functionReference: KtNameReferenceExpression) {
            val functionName = functionReference.getReferencedName()
            if (lastNameInOrder != null && functionName != lastNameInOrder && visitedNames.contains(functionName)) {
                kotlinFileContext.reportIssue(
                    functionReference,
                    message {
                        +"Group "
                        code(functionName)
                        +" dependencies"
                    }
                )
            } else {
                lastNameInOrder = functionName
            }
            visitedNames.add(functionName)
        }
    }
}

private fun getFunctionNameOrNull(expression: KtCallExpression) =
    (expression.getCalleeExpressionIfAny() as? KtNameReferenceExpression)?.getReferencedName()

private fun getLambdaBlock(expression: KtCallExpression): KtBlockExpression? {
    val lambdaArg = expression.valueArguments.lastOrNull() as? KtLambdaArgument
    return lambdaArg?.getLambdaExpression()?.bodyExpression
}
