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

import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ANY_TYPE
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.HASHCODE_METHOD_NAME
import org.sonarsource.kotlin.api.isAbstract
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
                hashCodeMethod == null && hashCodeMatcher.matches(it, ctx.bindingContext) -> hashCodeMethod = it
                equalsMethod == null && equalsMatcher.matches(it, ctx.bindingContext) -> equalsMethod = it
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
