package org.sonarsource.kotlin.externalreport

data class ExternalRule(
    val key: String,
    val name: String,
    val description: String?,
    val url: String?,
    val tags: Set<String>,
    val type: String,
    val severity: String? = null,
    val constantDebtMinutes: Long,
)
