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
package org.sonarsource.kotlin.surefire.data;

import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.sonarsource.kotlin.surefire.data.UnitTestResultTest.UnitTestResultAssert.assertThat;

class UnitTestResultTest {

  @Test
  void shouldBeError() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR);
    assertThat(result)
      .hasStatus(UnitTestResult.STATUS_ERROR)
      .isError()
      .isErrorOrFailure();
  }

  @Test
  void shouldBeFailure() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_FAILURE);
    
    assertThat(result)
      .hasStatus(UnitTestResult.STATUS_FAILURE)
      .isNotError()
      .isErrorOrFailure();
  }

  @Test
  void shouldBeSuccess() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_OK);

    assertThat(result)
      .hasStatus(UnitTestResult.STATUS_OK)
      .isNotError()
      .isNotErrorOrFailure();
  }

  @Test
  void shouldGetClassName() {
    UnitTestResult result = new UnitTestResult()
      .setTestSuiteClassName("MyClass");

    assertThat(result)
      .hasTestSuiteClassName("MyClass");
  }
  
  static class UnitTestResultAssert extends AbstractAssert<UnitTestResultAssert, UnitTestResult> {

    private UnitTestResultAssert(UnitTestResult actual) {
      super(actual, UnitTestResultAssert.class);
    }
    
    static UnitTestResultAssert assertThat(UnitTestResult actual) {
      return new UnitTestResultAssert(actual);
    }

    UnitTestResultAssert hasStatus(String status) {
      if (!actual.getStatus().equals(status)) {
        fail(String.format("Unexpected status. Expected: '%s', but was: '%s'.", status, actual.getStatus()));
      }
      return this;
    }
    
    UnitTestResultAssert isError() {
      if (!actual.isError()) {
        fail(String.format("Test status should be %s, but was: '%s'.", UnitTestResult.STATUS_ERROR, actual.getStatus()));
      }
      return this;
    }
    
    UnitTestResultAssert isNotError() {
      if (actual.isError()) {
        fail(String.format("Test status should not be %s.", UnitTestResult.STATUS_ERROR));
      }
      return this;
    }
    
    UnitTestResultAssert isErrorOrFailure() {
      if (!actual.isErrorOrFailure()) {
        fail(String.format("Test status should be %s or %s, but was: '%s'.", UnitTestResult.STATUS_ERROR, UnitTestResult.STATUS_FAILURE, actual.getStatus()));
      }
      return this;
    }    
    
    UnitTestResultAssert isNotErrorOrFailure() {
      if (actual.isErrorOrFailure()) {
        fail(String.format("Test status should not be either %s or %s.", UnitTestResult.STATUS_ERROR, UnitTestResult.STATUS_FAILURE, actual.getStatus()));
      }
      return this;
    }
    
    UnitTestResultAssert hasTestSuiteClassName(String testSuiteClassName) {
      if (!actual.getTestSuiteClassName().equals(testSuiteClassName)) {
        fail(String.format("Test suite class name should be %s, but was: '%s'.", testSuiteClassName, actual.getTestSuiteClassName()));
      }
      return this;
    }

  }
  
}
