/*
 * SonarSource Kotlin
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
package org.sonarsource.kotlin.api

const val INT_TYPE = "kotlin.Int"
const val STRING_TYPE = "kotlin.String"
const val GET_INSTANCE = "getInstance"
const val WITH_CONTEXT = "withContext"
const val ASYNC = "async"
const val LAUNCH = "launch"
const val KOTLINX_COROUTINES_PACKAGE = "kotlinx.coroutines"
const val DEFERRED_FQN = "kotlinx.coroutines.Deferred"
const val COROUTINES_FLOW = "kotlinx.coroutines.flow.Flow"
const val COROUTINES_CHANNEL = "kotlinx.coroutines.channels.Channel"
const val THROWS_FQN = "kotlin.jvm.Throws"

val FUNS_ACCEPTING_DISPATCHERS = listOf(
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = WITH_CONTEXT),
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = ASYNC),
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = LAUNCH),
)
