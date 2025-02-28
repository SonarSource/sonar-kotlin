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
package org.sonarsource.kotlin.api.frontend;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.analysis.api.KaSession;
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi;
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken;
import org.jetbrains.kotlin.diagnostics.Diagnostic;

@SuppressWarnings("KotlinInternalInJava")
public final class K1internals {

  private K1internals() {
  }

  public static boolean isK1(KaSession kaSession) {
    return kaSession.getUseSiteSession() instanceof org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session;
  }

  static org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade createCliFe10AnalysisFacade() {
    return new org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade();
  }

  static org.jetbrains.kotlin.analysis.api.descriptors.KaFe10AnalysisHandlerExtension createKaFe10AnalysisHandlerExtension() {
    return new org.jetbrains.kotlin.analysis.api.descriptors.KaFe10AnalysisHandlerExtension();
  }

  static org.jetbrains.kotlin.references.fe10.base.DummyKtFe10ReferenceResolutionHelper dummyKtFe10ReferenceResolutionHelper() {
    return org.jetbrains.kotlin.references.fe10.base.DummyKtFe10ReferenceResolutionHelper.INSTANCE;
  }

  static KaDiagnosticWithPsi<PsiElement> kaFe10Diagnostic(Diagnostic diagnostic, KaLifetimeToken token) {
    return new org.jetbrains.kotlin.analysis.api.descriptors.components.KaFe10Diagnostic(diagnostic, token);
  }

}
