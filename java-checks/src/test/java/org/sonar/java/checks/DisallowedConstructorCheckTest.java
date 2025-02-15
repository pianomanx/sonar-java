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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class DisallowedConstructorCheckTest {

  @Test
  void detected() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    disallowedConstructorCheck.setClassName("A");
    disallowedConstructorCheck.setArgumentTypes("int, long, java.lang.String[]");
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedConstructorCheck/detected.java")
      .withCheck(disallowedConstructorCheck)
      .verifyIssues();
  }

  @Test
  void all_overloads() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    disallowedConstructorCheck.setClassName("checks.DisallowedConstructorCheck.A");
    disallowedConstructorCheck.setAllOverloads(true);
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DisallowedConstructorCheck/detected_all_overload.java"))
      .withCheck(disallowedConstructorCheck)
      .verifyIssues();
  }

  @Test
  void empty_parameters() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    disallowedConstructorCheck.setClassName("A");
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedConstructorCheck/empty_parameters.java")
      .withCheck(disallowedConstructorCheck)
      .verifyIssues();
  }

  @Test
  void empty_type_definition() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedConstructorCheck/empty_type_definition.java")
      .withCheck(disallowedConstructorCheck)
      .verifyNoIssues();
  }

}
