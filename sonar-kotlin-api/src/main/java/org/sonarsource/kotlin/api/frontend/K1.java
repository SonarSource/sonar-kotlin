/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.api.frontend;


import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.psi.ClassTypePointerFactory;
import com.intellij.psi.impl.smartPointers.PsiClassReferenceTypePointerFactory;
import org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade;
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade;
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10AnalysisHandlerExtension;
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory;
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory;
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService;
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinByModulesResolutionScopeProvider;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider;
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneGlobalModificationService;
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory;
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiSimpleServiceRegistrar;
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.PluginStructureProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.references.fe10.base.DummyKtFe10ReferenceResolutionHelper;
import org.jetbrains.kotlin.references.fe10.base.KtFe10ReferenceResolutionHelper;
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension;

import java.util.Collections;

@SuppressWarnings("ALL")
public class K1 {

  private K1() {
    // Utility class
  }

  public static void configureK1AnalysisApiServices(KotlinCoreEnvironment env) {
    MockApplication application = env.getProjectEnvironment().getEnvironment().getApplication();

    if (application.getServiceIfCreated(KtFe10ReferenceResolutionHelper.class) == null) {
      AnalysisApiFe10ServiceRegistrar.INSTANCE.registerApplicationServices(application);
    }
    final var project = env.getProjectEnvironment().getProject();
    AnalysisApiFe10ServiceRegistrar.INSTANCE.registerProjectServices(project);
    AnalysisApiFe10ServiceRegistrar.INSTANCE.registerProjectModelServices(
      project,
      env.getProjectEnvironment().getParentDisposable()
    );

    project.registerService(
      KotlinModificationTrackerFactory.class,
      KotlinStandaloneModificationTrackerFactory.class
    );
    project.registerService(
      KotlinGlobalModificationService.class,
      KotlinStandaloneGlobalModificationService.class
    );
    project.registerService(
      KotlinLifetimeTokenFactory.class,
      KotlinAlwaysAccessibleLifetimeTokenFactory.class
    );
    project.registerService(
      KotlinResolutionScopeProvider.class,
      KotlinByModulesResolutionScopeProvider.class
    );
    project.registerService(
      KotlinProjectStructureProvider.class,
      KtModuleProviderByCompilerConfiguration.build(
        env.getProjectEnvironment(),
        env.getConfiguration(),
        Collections.emptyList()
      )
    );
  }

  private static class AnalysisApiFe10ServiceRegistrar extends AnalysisApiSimpleServiceRegistrar {
    public static final AnalysisApiSimpleServiceRegistrar INSTANCE = new AnalysisApiFe10ServiceRegistrar();
    private static final String PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/analysis-api-fe10.xml";

    private AnalysisApiFe10ServiceRegistrar() {
    }

    @Override
    public void registerApplicationServices(MockApplication application) {
      PluginStructureProvider.INSTANCE.registerApplicationServices(application, PLUGIN_RELATIVE_PATH);
      application.registerService(
        KtFe10ReferenceResolutionHelper.class,
        DummyKtFe10ReferenceResolutionHelper.INSTANCE
      );

      final var applicationArea = application.getExtensionArea();
      if (!applicationArea.hasExtensionPoint(ClassTypePointerFactory.EP_NAME)) {
        CoreApplicationEnvironment.registerApplicationExtensionPoint(
          ClassTypePointerFactory.EP_NAME,
          ClassTypePointerFactory.class
        );
        applicationArea
          .getExtensionPoint(ClassTypePointerFactory.EP_NAME)
          .registerExtension(new PsiClassReferenceTypePointerFactory(), application);
      }
    }

    @Override
    public void registerProjectExtensionPoints(MockProject project) {
      AnalysisHandlerExtension.Companion.registerExtensionPoint(project);
      PluginStructureProvider.INSTANCE.registerProjectExtensionPoints(project, PLUGIN_RELATIVE_PATH);
    }

    @Override
    public void registerProjectServices(MockProject project) {
      PluginStructureProvider.INSTANCE.registerProjectServices(project, PLUGIN_RELATIVE_PATH);
      PluginStructureProvider.INSTANCE.registerProjectListeners(project, PLUGIN_RELATIVE_PATH);
    }

    @Override
    public void registerProjectModelServices(MockProject project, Disposable disposable) {
      project.registerService(Fe10AnalysisFacade.class, new CliFe10AnalysisFacade());
      AnalysisHandlerExtension.Companion.registerExtension(project, new KaFe10AnalysisHandlerExtension());
    }
  }
}
