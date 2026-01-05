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

import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ANY_TYPE
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.HASHCODE_METHOD_NAME
import org.sonarsource.kotlin.api.checks.isAbstract
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val EQUALS_MESSAGE = """This class overrides "equals()" and should therefore also override "hashCode()".""";
private const val HASHCODE_MESSAGE = """This class overrides "hashCode()" and should therefore also override "equals()".""";

private val equalsMatcher = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
}
private val hashCodeMatcher = FunMatcher {
    name = HASHCODE_METHOD_NAME
    withNoArguments()
}

@Rule(key = "S1206")
class EqualsOverridenWithHashCodeCheck : AbstractCheck() {

    override fun visitClassBody(klass: KtClassBody, ctx: KotlinFileContext) {
        var equalsMethod: KtNamedFunction? = null
        var hashCodeMethod: KtNamedFunction? = null

        klass.functions.forEach {
            when {
                hashCodeMethod == null && hashCodeMatcher.matches(it) -> hashCodeMethod = it
                equalsMethod == null && equalsMatcher.matches(it) -> equalsMethod = it
            }
            if (hashCodeMethod != null && equalsMethod != null) return
        }

        equalsMethod?.let {
            if (!it.isAbstract()) ctx.reportIssue(it, EQUALS_MESSAGE)   
        }
        hashCodeMethod?.let {
            if (!it.isAbstract()) ctx.reportIssue(it, HASHCODE_MESSAGE)
        }
    }

}
