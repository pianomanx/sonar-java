/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

class BooleanMethodReturnCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/BooleanMethodReturnCheckSample.java"))
      .withCheck(new BooleanMethodReturnCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/BooleanMethodReturnCheckSample.java"))
      .withCheck(new BooleanMethodReturnCheck())
      .verifyNoIssues();

  }

  @Test
  void test_jspecify_null_marked() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/jspecify/nullmarked/BooleanMethodReturnCheck.java"))
      .withCheck(new BooleanMethodReturnCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/jspecify/BooleanMethodReturnCheckNullMarked.java"))
      .withCheck(new BooleanMethodReturnCheck())
      .verifyIssues();
  }

}
