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
package org.sonarsource.kotlin.api.checks

const val INT_TYPE = "kotlin.Int"
const val STRING_TYPE = "kotlin.String"
const val BOOLEAN_TYPE = "kotlin.Boolean"
const val ANY_TYPE = "kotlin.Any"
const val GET_INSTANCE = "getInstance"
const val WITH_CONTEXT = "withContext"
const val ASYNC = "async"
const val LAUNCH = "launch"
const val KOTLINX_COROUTINES_PACKAGE = "kotlinx.coroutines"
const val DEFERRED_FQN = "kotlinx.coroutines.Deferred"
const val COROUTINES_FLOW = "kotlinx.coroutines.flow.Flow"
const val COROUTINES_CHANNEL = "kotlinx.coroutines.channels.Channel"
const val THROWS_FQN = "kotlin.jvm.Throws"
const val JAVA_STRING = "java.lang.String"
const val KOTLIN_TEXT = "kotlin.text"
const val JAVA_UTIL_PATTERN = "java.util.regex.Pattern"
const val HASHCODE_METHOD_NAME = "hashCode"
const val EQUALS_METHOD_NAME = "equals"

val BYTE_ARRAY_CONSTRUCTOR = ConstructorMatcher("kotlin.ByteArray")
val BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY = ConstructorMatcher("kotlin.ByteArray") { withArguments("kotlin.Int") }

val SECURE_RANDOM_FUNS = FunMatcher(qualifier = "java.security.SecureRandom")

val FUNS_ACCEPTING_DISPATCHERS = listOf(
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = WITH_CONTEXT),
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = ASYNC),
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = LAUNCH),
)
