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
package org.sonarsource.kotlin.plugin

import org.sonar.api.Plugin
import org.sonar.api.SonarProduct
import org.sonar.api.config.PropertyDefinition
import org.sonar.api.resources.Qualifiers
import org.sonarsource.kotlin.api.common.KOTLIN_FILE_SUFFIXES_DEFAULT_VALUE
import org.sonarsource.kotlin.api.common.KOTLIN_FILE_SUFFIXES_KEY
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.kotlin.externalreport.androidlint.AndroidLintRulesDefinition
import org.sonarsource.kotlin.externalreport.androidlint.AndroidLintSensor
import org.sonarsource.kotlin.externalreport.detekt.DetektRulesDefinition
import org.sonarsource.kotlin.externalreport.detekt.DetektSensor
import org.sonarsource.kotlin.externalreport.ktlint.KtlintRulesDefinition
import org.sonarsource.kotlin.externalreport.ktlint.KtlintSensor
import org.sonarsource.kotlin.gradle.KotlinGradleSensor
import org.sonarsource.kotlin.surefire.KotlinResourcesLocator
import org.sonarsource.kotlin.surefire.KotlinSurefireParser
import org.sonarsource.kotlin.surefire.KotlinSurefireSensor

class KotlinPlugin : Plugin {

    companion object {
        // Subcategories
        private const val GENERAL = "General"
        private const val KOTLIN_CATEGORY = "Kotlin"
        private const val EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers"
        private const val ANDROID_SUBCATEGORY = "Android"
        private const val KOTLIN_SUBCATEGORY = "Kotlin"

        // Global constants
        const val REPOSITORY_NAME = "SonarAnalyzer"
        const val PROFILE_NAME = "Sonar way"
        const val SKIP_UNCHANGED_FILES_OVERRIDE = "sonar.kotlin.skipUnchanged"
    }

    override fun define(context: Plugin.Context) {

        context.addExtensions(
            KotlinLanguage::class.java,
            KotlinSensor::class.java,
            KotlinRulesDefinition::class.java,
            KotlinProfileDefinition::class.java,
        )

        context.addExtension(KotlinGradleSensor::class.java)

        if (context.runtime.product != SonarProduct.SONARLINT) {
            context.addExtensions(
                KotlinResourcesLocator::class.java,
                KotlinSurefireParser::class.java,
                KotlinSurefireSensor::class.java,
                DetektRulesDefinition::class.java,
                DetektSensor::class.java,
                AndroidLintRulesDefinition::class.java,
                AndroidLintSensor::class.java,
                KtlintRulesDefinition::class.java,
                KtlintSensor::class.java,
                PropertyDefinition.builder(KOTLIN_FILE_SUFFIXES_KEY)
                    .defaultValue(KOTLIN_FILE_SUFFIXES_DEFAULT_VALUE)
                    .name("File Suffixes")
                    .description("List of suffixes for files to analyze.")
                    .subCategory(GENERAL)
                    .category(KOTLIN_CATEGORY)
                    .multiValues(true)
                    .onQualifiers(Qualifiers.PROJECT)
                    .build(),
                PropertyDefinition.builder(DetektSensor.REPORT_PROPERTY_KEY)
                    .name("Detekt Report Files")
                    .description("Paths (absolute or relative) to checkstyle xml files with Detekt issues.")
                    .category(EXTERNAL_ANALYZERS_CATEGORY)
                    .subCategory(KOTLIN_SUBCATEGORY)
                    .onQualifiers(Qualifiers.PROJECT)
                    .multiValues(true)
                    .build(),
                PropertyDefinition.builder(AndroidLintSensor.REPORT_PROPERTY_KEY)
                    .name("Android Lint Report Files")
                    .description("Paths (absolute or relative) to xml files with Android Lint issues.")
                    .category(EXTERNAL_ANALYZERS_CATEGORY)
                    .subCategory(ANDROID_SUBCATEGORY)
                    .onQualifiers(Qualifiers.PROJECT)
                    .multiValues(true)
                    .build(),
                PropertyDefinition.builder(KtlintSensor.REPORT_PROPERTY_KEY)
                    .name("Ktlint Report Files")
                    .description("Paths (absolute or relative) to checkstyle xml or json files with Ktlint issues.")
                    .category(EXTERNAL_ANALYZERS_CATEGORY)
                    .subCategory(KOTLIN_SUBCATEGORY)
                    .onQualifiers(Qualifiers.PROJECT)
                    .multiValues(true)
                    .build()
            )
        }
    }
}
