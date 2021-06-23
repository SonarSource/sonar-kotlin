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
package org.sonarsource.kotlin.converter;

import org.sonarsource.slang.api.NativeKind;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;

public class KotlinNativeKind implements NativeKind {

  private final Class<? extends PsiElement> psiElementClass;
  private final List<Object> differentiators;

  public KotlinNativeKind(PsiElement element) {
    this(element.getClass());
  }

  public KotlinNativeKind(Class<? extends PsiElement> psiElementClass) {
    this.psiElementClass = psiElementClass;
    this.differentiators = Collections.emptyList();
  }

  public KotlinNativeKind(PsiElement element, Object... differentiatorObjs) {
    this(element.getClass(), differentiatorObjs);
  }

  public KotlinNativeKind(Class<? extends PsiElement> psiElementClass, Object... differentiatorObjs) {
    this.psiElementClass = psiElementClass;
    this.differentiators = Arrays.asList(differentiatorObjs);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KotlinNativeKind that = (KotlinNativeKind) o;
    return Objects.equals(psiElementClass, that.psiElementClass) &&
      Objects.equals(differentiators, that.differentiators);
  }

  @Override
  public int hashCode() {
    return Objects.hash(psiElementClass, differentiators);
  }

  @Override
  public String toString() {
    if (differentiators.isEmpty()) {
      return psiElementClass.getSimpleName();
    } else {
      return psiElementClass.getSimpleName()
        + differentiators.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }
  }

}
