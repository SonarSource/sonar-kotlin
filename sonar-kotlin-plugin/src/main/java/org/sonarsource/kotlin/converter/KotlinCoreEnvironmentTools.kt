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
package org.sonarsource.kotlin.converter

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.io.File

class Environment(
    val classpath: List<String>,
    kotlinLanguageVersion: LanguageVersion,
    javaLanguageVersion: JvmTarget = JvmTarget.JVM_1_8,
    numberOfThreads: Int? = null
) {
    val disposable = Disposer.newDisposable()
    val configuration = compilerConfiguration(classpath, kotlinLanguageVersion, javaLanguageVersion, numberOfThreads)
    val env = kotlinCoreEnvironment(configuration, disposable)
    val ktPsiFactory: KtPsiFactory = KtPsiFactory(env.project, false)
}

/** Without this declaration JAR minimization can't detect that these classes should not be removed. */
@Suppress("unused")
private val classesToKeepWhenMinimizingJar = arrayOf(
    /** META-INF/services/org.jetbrains.kotlin.builtins.BuiltInsLoader */
    org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsLoaderImpl::class.java,
    /** META-INF/services/org.jetbrains.kotlin.util.ModuleVisibilityHelper */
    org.jetbrains.kotlin.cli.common.ModuleVisibilityHelperImpl::class.java,
    /** META-INF/services/org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition */
    org.jetbrains.kotlin.load.java.FieldOverridabilityCondition::class.java,
    org.jetbrains.kotlin.load.java.ErasedOverridabilityCondition::class.java,
    org.jetbrains.kotlin.load.java.JavaIncompatibilityRulesOverridabilityCondition::class.java,
    /** META-INF/services/org.jetbrains.kotlin.resolve.jvm.jvmSignature.KotlinToJvmSignatureMapper */
    org.jetbrains.kotlin.codegen.signature.KotlinToJvmSignatureMapperImpl::class.java,
)

fun kotlinCoreEnvironment(
    configuration: CompilerConfiguration,
    disposable: Disposable,
): KotlinCoreEnvironment {
    setIdeaIoUseFallback()
    configuration.put(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        MessageCollector.NONE
    )
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "sonar-kotlin-ng")

    return KotlinCoreEnvironment.createForProduction(
        disposable,
        configuration,
        // FIXME Add support of Kotlin/JS Kotlin/Native
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )
}

fun bindingContext(
    environment: KotlinCoreEnvironment,
    classpath: List<String>,
    files: List<KtFile>,
): BindingContext =
    if (classpath.isEmpty())
        BindingContext.EMPTY
    else
        analyzeAndGetBindingContext(environment, files)

private fun analyzeAndGetBindingContext(
    env: KotlinCoreEnvironment,
    ktFiles: List<KtFile>,
): BindingContext {
    val analyzer = AnalyzerWithCompilerReport(
        MessageCollector.NONE,
        env.configuration.languageVersionSettings,
        false
    )
    analyzer.analyzeAndReport(ktFiles) {
        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            env.project,
            ktFiles,
            NoScopeRecordCliBindingTrace(),
            env.configuration,
            env::createPackagePartProvider,
            ::FileBasedDeclarationProviderFactory
        )
    }
    return analyzer.analysisResult.bindingContext
}

fun compilerConfiguration(
    classpath: List<String>,
    languageVersion: LanguageVersion,
    jvmTarget: JvmTarget,
    numberOfThreads: Int?,
): CompilerConfiguration {
    val classpathFiles = classpath.map(::File)
    val versionSettings = LanguageVersionSettingsImpl(
        languageVersion,
        ApiVersion.createByLanguageVersion(languageVersion),
    )

    return CompilerConfiguration().apply {
        put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, versionSettings)
        put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
        put(JVMConfigurationKeys.JDK_HOME, File(System.getProperty("java.home")))
        numberOfThreads?.let { put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, it) }
        addJvmClasspathRoots(classpathFiles)
    }
}

