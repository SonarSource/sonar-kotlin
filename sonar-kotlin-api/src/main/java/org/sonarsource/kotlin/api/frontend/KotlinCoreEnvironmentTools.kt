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
package org.sonarsource.kotlin.api.frontend

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

/**
 * @param disposable
 * manages objects requiring cleanup,
 * can be created using [Disposer.newDisposable] and cleanup must be done by [Disposer.dispose].
 * See [documentation](https://jetbrains.org/intellij/sdk/docs/basics/disposers.html) for more details.
 */
class Environment(
    val disposable: Disposable,
    val classpath: List<String>,
    kotlinLanguageVersion: LanguageVersion,
    javaLanguageVersion: JvmTarget = JvmTarget.JVM_1_8,
) {
    val configuration = compilerConfiguration(classpath, kotlinLanguageVersion, javaLanguageVersion)
    private val env = kotlinCoreEnvironment(configuration, disposable)
    val ktPsiFactory: KtPsiFactory = KtPsiFactory(env.project, false)
    // K2
    var k2session: StandaloneAnalysisAPISession? = null
}

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

fun compilerConfiguration(
    classpath: List<String>,
    languageVersion: LanguageVersion,
    jvmTarget: JvmTarget,
): CompilerConfiguration {
    val versionSettings = LanguageVersionSettingsImpl(
        languageVersion,
        ApiVersion.createByLanguageVersion(languageVersion),
    )

    return CompilerConfiguration().apply {
        put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, versionSettings)
        put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
        put(JVMConfigurationKeys.JDK_HOME, File(System.getProperty("java.home")))
        addJvmClasspathRoots(classpathRoots(classpath))
    }
}

/**
 * Non-JAR files are filtered-out to avoid invocation of [com.intellij.openapi.diagnostic.Logger.warn] in
 * [org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarHandler] and
 * [com.intellij.openapi.vfs.impl.ArchiveHandler.getEntriesMap].
 */
private fun classpathRoots(classpath: List<String>): List<File> =
    classpath.map(::File).filter { it.isDirectory || it.isJar() }

private fun File.isJar(): Boolean =
    this.isFile && inputStream().use {
        val header = (it.read() shl 24) or (it.read() shl 16) or (it.read() shl 8) or it.read()
        header == 0x504b0304
    }
