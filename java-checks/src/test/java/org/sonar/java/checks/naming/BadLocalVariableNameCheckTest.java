/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.naming;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class BadLocalVariableNameCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/naming/BadLocalVariableNameNoncompliant.java")
      .withCheck(new BadLocalVariableNameCheck())
      .verifyIssues();
  }

  @Test
  void test2() {
    BadLocalVariableNameCheck check = new BadLocalVariableNameCheck();
    check.format = "^[a-zA-Z0-9_][a-zA-Z0-9_][a-zA-Z0-9_][a-zA-Z0-9_]*$";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/naming/BadLocalVariableName.java")
      .withCheck(check)
      .verifyNoIssues();
  }

}
