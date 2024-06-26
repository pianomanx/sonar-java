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
package org.sonar.java.checks.security;


import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.CommonConstants.AWS_CLASSPATH;
import static org.sonar.java.checks.CommonConstants.AWS_MODULE;

class HardCodedCredentialsShouldNotBeUsedCheckTest {
  @RegisterExtension
  final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void uses_empty_collection_when_methods_cannot_be_loaded() {
    var check = new HardCodedCredentialsShouldNotBeUsedCheck("non-existing-file.json");
    assertThat(check.getMethods()).isEmpty();
    List<String> logs = logTester.getLogs(Level.ERROR).stream()
      .map(LogAndArguments::getFormattedMsg)
      .toList();
    assertThat(logs)
      .containsOnly("Could not load methods from \"non-existing-file.json\".");
  }


  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPathInModule(AWS_MODULE, "checks/security/HardCodedCredentialsShouldNotBeUsedCheckSample.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .withClassPath(AWS_CLASSPATH)
      .verifyIssues();
  }

  @Test
  void test_non_compiling_code() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/security/HardCodedCredentialsShouldNotBeUsedCheckSample.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .withClassPath(AWS_CLASSPATH)
      .verifyIssues();
  }

}
