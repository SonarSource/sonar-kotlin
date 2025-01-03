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

          // META-INF/analysis-api/analysis-api-impl-base.xml
          org.jetbrains.kotlin.analysis.api.impl.base.java.source.JavaElementSourceWithSmartPointerFactory.class,
          org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService.class,
          org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseModuleProvider.class,
          org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider.class,
          org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionChecker.class,
          org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker.class,
          // META-INF/analysis-api/analysis-api-fe10.xml
          org.jetbrains.kotlin.analysis.api.descriptors.KaFe10SessionProvider.class,
          org.jetbrains.kotlin.references.fe10.base.KtFe10KotlinReferenceProviderContributor.class,
          org.jetbrains.kotlin.analysis.api.descriptors.references.ReadWriteAccessCheckerDescriptorsImpl.class,

          /** Used to have proper named groups behavior in regular expressions */
          kotlin.internal.jdk8.JDK8PlatformImplementations.class
  );

  private WorkaroundForJarMinimization() {}
}
