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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.plugin.KotlinCheckList.legacyChecks
import org.sonarsource.slang.checks.api.SlangCheck
import org.sonarsource.slang.testing.PackageScanner

internal class KotlinCheckListTest {
    @Test
    fun kotlin_specific_checks_are_added_to_check_list() {
        val languageImplementation = PackageScanner.findSlangChecksInPackage(KOTLIN_CHECKS_PACKAGE)
        val checkListNames = legacyChecks().map { obj: Class<*> -> obj.name }
        val kotlinSpecificCheckList = KotlinCheckList.SLANG_CHECKS.map { obj: Class<out SlangCheck?> -> obj.name }
        for (languageCheck in languageImplementation.filter {
            /** Replaced by [org.sonarsource.kotlin.checks.TooManyParametersCheck] */
            it != org.sonarsource.kotlin.checks.TooManyParametersKotlinCheck::class.java.name
        }) {
            assertThat(checkListNames).contains(languageCheck)
            assertThat(kotlinSpecificCheckList).contains(languageCheck)
            assertThat(languageCheck).endsWith("KotlinCheck")
        }
    }

    @Test
    fun kotlin_excluded_not_present() {
        val checks = legacyChecks()
        for (excluded in KotlinCheckList.SLANG_EXCLUDED_CHECKS) {
            assertThat(checks).doesNotContain(excluded)
        }
    }

    @Test
    fun kotlin_included_are_present() {
        val checks = legacyChecks()
        for (specificCheck in KotlinCheckList.SLANG_CHECKS) {
            assertThat(checks).contains(specificCheck)
        }
    }

    companion object {
        private const val KOTLIN_CHECKS_PACKAGE = "org.sonarsource.kotlin.checks"
    }
}
