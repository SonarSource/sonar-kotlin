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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * @see [org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder.buildKtModuleProviderByCompilerConfiguration]
 */
fun createK2AnalysisSession(
    parentDisposable: Disposable,
    compilerConfiguration: CompilerConfiguration,
    virtualFiles: Collection<VirtualFile>,
): StandaloneAnalysisAPISession {
    return buildStandaloneAnalysisAPISession(
        projectDisposable = parentDisposable,
    ) {
        // https://github.com/JetBrains/kotlin/blob/a9ff22693479cabd201909a06e6764c00eddbf7b/analysis/analysis-api-fe10/tests/org/jetbrains/kotlin/analysis/api/fe10/test/configurator/AnalysisApiFe10TestServiceRegistrar.kt#L49
        registerProjectService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(enabled = true))

        // TODO language version, jvm target, etc
        val platform = JvmPlatforms.defaultJvmPlatform
        buildKtModuleProvider {
            this.platform = platform
            addModule(buildKtSourceModule {
                this.platform = platform
                moduleName = "module"
                addSourceVirtualFiles(virtualFiles)
                addRegularDependency(buildKtLibraryModule {
                    this.platform = platform
                    libraryName = "library"
                    addBinaryRoots(compilerConfiguration.jvmClasspathRoots.map { it.toPath() })
                })
                compilerConfiguration[JVMConfigurationKeys.JDK_HOME]?.let { jdkHome ->
                    addRegularDependency(buildKtSdkModule {
                        this.platform = platform
                        addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = false)
                        libraryName = "JDK"
                    })
                }
            })
        }
    }
}

class KotlinFileSystem : CoreLocalFileSystem() {
    /**
     * TODO return null if file does not exist - see [CoreLocalFileSystem.findFileByNioFile]
     */
    override fun findFileByPath(path: String): VirtualFile? =
        KotlinVirtualFile(this, File(path))
}

class KotlinVirtualFile(
    private val fileSystem: KotlinFileSystem,
    private val file: File,
    private val content: String? = null,
) : VirtualFile() {

    override fun getName(): String = file.name

    override fun getFileSystem(): VirtualFileSystem = fileSystem

    override fun getPath(): String = FileUtil.toSystemIndependentName(file.absolutePath)

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = file.isDirectory

    override fun isValid(): Boolean = true

    override fun getParent(): VirtualFile? {
        val parentFile = file.parentFile ?: return null
        return KotlinVirtualFile(fileSystem, parentFile)
    }

    override fun getChildren(): Array<VirtualFile> {
        if (file.isFile || !file.exists()) return emptyArray()
        throw UnsupportedOperationException("getChildren " + file.absolutePath)
    }

    override fun getOutputStream(p0: Any?, p1: Long, p2: Long): OutputStream =
        throw UnsupportedOperationException()

    override fun contentsToByteArray(): ByteArray {
        if (content != null) return content.toByteArray()
        return FileUtil.loadFileBytes(file)
    }

    override fun getTimeStamp(): Long =
        throw UnsupportedOperationException()

    override fun getLength(): Long = file.length()

    override fun refresh(p0: Boolean, p1: Boolean, p2: Runnable?) =
        throw UnsupportedOperationException()

    override fun getInputStream(): InputStream =
        throw UnsupportedOperationException()

}
