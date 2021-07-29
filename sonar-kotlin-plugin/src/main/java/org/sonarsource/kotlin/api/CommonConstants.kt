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

val FUNS_ACCEPTING_DISPATCHERS = listOf(
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = WITH_CONTEXT),
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = ASYNC),
    FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = LAUNCH),
)
