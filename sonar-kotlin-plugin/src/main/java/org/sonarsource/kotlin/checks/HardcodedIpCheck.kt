/*
 * SonarSource SLang
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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val IPV4_ALONE = """(?<ipv4>(?:\d{1,3}\.){3}\d{1,3})"""
private const val IPV6_NO_PREFIX_COMPRESSION = """(\p{XDigit}{1,4}::?){1,7}\p{XDigit}{1,4}(::)?"""
private const val IPV6_PREFIX_COMPRESSION = """::((\p{XDigit}{1,4}:){0,6}\p{XDigit}{1,4})?"""
private const val IPV6_ALONE = "(?<ipv6>($IPV6_NO_PREFIX_COMPRESSION|$IPV6_PREFIX_COMPRESSION)??(:?$IPV4_ALONE)?)"
private const val IPV6_URL = """([^\d.]*/)?\[$IPV6_ALONE]((:\d{1,5})?(?!\d|\.))(/.*)?"""
private val IPV4_URL_REGEX = Regex("""([^\d.]*/)?$IPV4_ALONE((:\d{1,5})?(?!\d|\.))(/.*)?""")
private val IPV6_REGEX_LIST = listOf(Regex(IPV6_ALONE), Regex(IPV6_URL))
private val IPV6_LOOPBACK = Regex("[0:]++0*+1")
private val IPV6_NON_ROUTABLE = Regex("[0:]++")
private val INVALID_IPV4_PART_PATTERN = Regex("""^0\d{1,2}""")
private val IPV6_SPLIT_REGEX = Regex("::?")

private const val MESSAGE = "Make sure using this hardcoded IP address is safe here."

/**
 * Replacement for [org.sonarsource.slang.checks.HardcodedIpCheck]
 */
@Rule(key = "S1313")
class HardcodedIpCheck : AbstractCheck() {

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, context: KotlinFileContext) {
        if (expression.hasInterpolation()) return
        val content = expression.asConstant()
        val matcher = IPV4_URL_REGEX.matchEntire(content)
        if (matcher != null) {
            val ip = matcher.groups["ipv4"]!!.value
            if (isValidIPV4(ip) && !isIPV4Exception(ip)) {
                context.reportIssue(expression, MESSAGE)
            }
        } else {
            IPV6_REGEX_LIST.asSequence()
                .mapNotNull { pattern -> pattern.matchEntire(content) }
                .firstOrNull()
                ?.let { match ->
                    val ipv6 = match.groups["ipv6"]!!.value
                    val ipv4 = match.groups["ipv4"]?.value
                    if (isValidIPV6(ipv6, ipv4) && !isIPV6Exception(ipv6)) context.reportIssue(expression, MESSAGE)
                }
        }
    }
}

private fun isValidIPV4(ip: String) = ip.split(".").toTypedArray()
    .none { INVALID_IPV4_PART_PATTERN.matches(it) || Integer.valueOf(it) > 255 }

private fun isValidIPV6(ipv6: String, ipv4: String?): Boolean {
    val split = ipv6.split(IPV6_SPLIT_REGEX).toTypedArray()
    val partCount = split.size
    val compressionSeparatorCount = getCompressionSeparatorCount(ipv6)
    val validUncompressed: Boolean
    val validCompressed: Boolean
    if (ipv4 != null) {
        val hasValidIPV4 = isValidIPV4(ipv4)
        validUncompressed = hasValidIPV4 && compressionSeparatorCount == 0 && partCount == 7
        validCompressed = hasValidIPV4 && compressionSeparatorCount == 1 && partCount <= 6
    } else {
        validUncompressed = compressionSeparatorCount == 0 && partCount == 8
        validCompressed = compressionSeparatorCount == 1 && partCount <= 7
    }
    return validUncompressed || validCompressed
}

private fun isIPV4Exception(ip: String) = ip.startsWith("127.")
        || "255.255.255.255" == ip || "0.0.0.0" == ip || ip.startsWith("2.5.")

private fun isIPV6Exception(ip: String) = IPV6_LOOPBACK.matches(ip) || IPV6_NON_ROUTABLE.matches(ip)

private fun getCompressionSeparatorCount(str: String) = str.split("::").size - 1
