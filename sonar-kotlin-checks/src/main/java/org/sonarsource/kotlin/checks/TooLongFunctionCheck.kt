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

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val DEFAULT_MAX = 100

@Rule(key = "S138")
class TooLongFunctionCheck : AbstractTooLongFunctionRule() {

    @RuleProperty(
        key = "max",
        description = "Maximum authorized lines of code in a function",
        defaultValue = "" + DEFAULT_MAX,
    )
    override var max: Int = DEFAULT_MAX

    override val elementName = "function"

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, kotlinFileContext: KotlinFileContext) {
        check(constructor, kotlinFileContext)
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        check(function, kotlinFileContext)
    }

}
