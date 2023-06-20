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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

// NOTE! Rule "S6627: Users should not use internal APIs" is general, not gradle specific!
// This is a dummy implementation for a gradle only rule.
// Test Strategy:
//   1. Write a dummy unit test for it
//   2. That test uses a KotlinGradleVerifier instead of the KotlinVerifier
//      - Note: - KotlinGradleSensor rather than the KotlinSensor
//              - But: Unit Tests do not use sensors at all. Environment is provided by Verifier instead

@Rule(key = "S6622")
class ProviderGetOutsideTaskCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, ctx: KotlinFileContext) {
        ctx.reportIssue(expression, "This class is silly!")
    }
}