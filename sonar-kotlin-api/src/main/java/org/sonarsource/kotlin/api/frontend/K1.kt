/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.ClassTypePointerFactory
import com.intellij.psi.impl.smartPointers.PsiClassReferenceTypePointerFactory
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinByModulesResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneGlobalModificationService
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiSimpleServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.PluginStructureProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.references.fe10.base.KtFe10ReferenceResolutionHelper
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

@OptIn(KaPlatformInterface::class)
internal fun configureK1AnalysisApiServices(env: KotlinCoreEnvironment) {
    val application = env.projectEnvironment.environment.application
    if (application.getServiceIfCreated(KtFe10ReferenceResolutionHelper::class.java) == null) {
        AnalysisApiFe10ServiceRegistrar.registerApplicationServices(application)
    }
    val project = env.projectEnvironment.project
    AnalysisApiFe10ServiceRegistrar.registerProjectServices(project)
    AnalysisApiFe10ServiceRegistrar.registerProjectModelServices(
        project,
        env.projectEnvironment.parentDisposable
    )

    project.registerService(
        KotlinModificationTrackerFactory::class.java,
        KotlinStandaloneModificationTrackerFactory::class.java,
    )
    project.registerService(
        KotlinGlobalModificationService::class.java,
        KotlinStandaloneGlobalModificationService::class.java,
    )
    project.registerService(
        KotlinLifetimeTokenFactory::class.java,
        KotlinAlwaysAccessibleLifetimeTokenFactory::class.java,
    )
    project.registerService(
        KotlinResolutionScopeProvider::class.java,
        KotlinByModulesResolutionScopeProvider::class.java,
    );
    project.registerService(
        KotlinProjectStructureProvider::class.java,
        KtModuleProviderByCompilerConfiguration.build(
            env.projectEnvironment,
            env.configuration,
            listOf()
        )
    )
}

@OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
private object AnalysisApiFe10ServiceRegistrar : AnalysisApiSimpleServiceRegistrar() {
    private const val PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/analysis-api-fe10.xml"

    override fun registerApplicationServices(application: MockApplication) {
        PluginStructureProvider.registerApplicationServices(application, PLUGIN_RELATIVE_PATH)
        application.registerService(
            KtFe10ReferenceResolutionHelper::class.java,
            K1internals.dummyKtFe10ReferenceResolutionHelper(),
        )
        val applicationArea = application.extensionArea
        if (!applicationArea.hasExtensionPoint(ClassTypePointerFactory.EP_NAME)) {
            CoreApplicationEnvironment.registerApplicationExtensionPoint(
                ClassTypePointerFactory.EP_NAME,
                ClassTypePointerFactory::class.java,
            )
            applicationArea
                .getExtensionPoint(ClassTypePointerFactory.EP_NAME)
                .registerExtension(PsiClassReferenceTypePointerFactory(), application)
        }
    }

    override fun registerProjectExtensionPoints(project: MockProject) {
        AnalysisHandlerExtension.registerExtensionPoint(project)
        PluginStructureProvider.registerProjectExtensionPoints(project, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectServices(project: MockProject) {
        PluginStructureProvider.registerProjectServices(project, PLUGIN_RELATIVE_PATH)
        PluginStructureProvider.registerProjectListeners(project, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectModelServices(project: MockProject, disposable: Disposable) {
        project.apply { registerService(Fe10AnalysisFacade::class.java, K1internals.createCliFe10AnalysisFacade()) }
        AnalysisHandlerExtension.registerExtension(project, K1internals.createKaFe10AnalysisHandlerExtension())
    }
}
