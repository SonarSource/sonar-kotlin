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
package org.sonarsource.kotlin.plugin.linking;

import java.util.List;

@SuppressWarnings("KotlinInternalInJava")
public class WorkaroundForJarMinimization {

  /** Without this declaration JAR minimization can't detect that these classes should not be removed. */
  @SuppressWarnings("unused")
  static final List<Class<?>> CLASSES_TO_KEEP_WHEN_MINIMIZING_JAR = List.of(
          /** META-INF/services/org.jetbrains.kotlin.builtins.BuiltInsLoader */
          org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsLoaderImpl.class,
          /** META-INF/services/org.jetbrains.kotlin.util.ModuleVisibilityHelper */
          org.jetbrains.kotlin.cli.common.ModuleVisibilityHelperImpl.class,
          /** META-INF/services/org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition */
          org.jetbrains.kotlin.load.java.FieldOverridabilityCondition.class,
          org.jetbrains.kotlin.load.java.ErasedOverridabilityCondition.class,
          org.jetbrains.kotlin.load.java.JavaIncompatibilityRulesOverridabilityCondition.class,
          /** META-INF/services/org.jetbrains.kotlin.resolve.jvm.jvmSignature.KotlinToJvmSignatureMapper */
          org.jetbrains.kotlin.codegen.signature.KotlinToJvmSignatureMapperImpl.class,

          // all projectService.serviceImplementation from
          // META-INF/analysis-api/analysis-api-impl-base.xml
          org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade.class,
          org.jetbrains.kotlin.analysis.api.impl.base.java.source.JavaElementSourceWithSmartPointerFactory.class,
          org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService.class,
          org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseModuleProvider.class,
          org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider.class,
          org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionChecker.class,
          org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker.class,
          // all projectService.serviceImplementation from
          // META-INF/analysis-api/analysis-api-fe10.xml
          org.jetbrains.kotlin.analysis.api.descriptors.KaFe10SessionProvider.class,
          org.jetbrains.kotlin.analysis.api.descriptors.modification.KaFe10SourceModificationService.class,
          org.jetbrains.kotlin.references.fe10.base.KtFe10KotlinReferenceProviderContributor.class,
          org.jetbrains.kotlin.analysis.api.descriptors.references.ReadWriteAccessCheckerDescriptorsImpl.class,
          // all projectService.serviceImplementation from
          // META-INF/analysis-api/low-level-api-fir.xml
          org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLRealFirElementByPsiElementChooser.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirBuiltinsSessionFactory.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationEventPublisher.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService.class,
          org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirInBlockModificationTracker.class,
          // all projectService.serviceImplementation from
          // META-INF/analysis-api/symbol-light-classes.xml
          org.jetbrains.kotlin.light.classes.symbol.SymbolKotlinAsJavaSupport.class,
          // all projectService.serviceImplementation and applicationService.serviceImplementation from
          // META-INF/analysis-api/analysis-api-fir-standalone-base.xml
          org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneFirDirectInheritorsProvider.class,
          org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinSimpleGlobalSearchScopeMerger.class,
          org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderCliImpl.class,
          // all projectService.serviceImplementation from
          // META-INF/analysis-api/analysis-api-fir.xml
          org.jetbrains.kotlin.analysis.api.fir.KaFirSessionProvider.class,
          org.jetbrains.kotlin.analysis.api.fir.modification.KaFirSourceModificationService.class,
          org.jetbrains.kotlin.analysis.api.fir.references.KotlinFirReferenceContributor.class,
          org.jetbrains.kotlin.analysis.api.fir.references.ReadWriteAccessCheckerFirImpl.class,

          /** Used to have proper named groups behavior in regular expressions */
          kotlin.internal.jdk8.JDK8PlatformImplementations.class
  );

  private WorkaroundForJarMinimization() {}
}
