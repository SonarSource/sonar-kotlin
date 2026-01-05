/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.DEFERRED_FQN
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6315")
class UnusedDeferredResultCheck : AbstractCheck() {

    private val deferredFqn = ClassId.fromString(DEFERRED_FQN.replace('.', '/'))

    override fun visitCallExpression(expression: KtCallExpression, context: KotlinFileContext) = withKaSession {
        if (expression.expressionType!!.isClassType(deferredFqn)
            && !expression.isUsedAsExpression) {
            context.reportIssue(expression.calleeExpression!!, """This function returns "Deferred", but its result is never used.""")
        }
    }
}
