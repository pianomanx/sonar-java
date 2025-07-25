/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource SA
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
package com.sonar.it.java.suite;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import org.junit.ClassRule;
import org.junit.Test;

import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsDouble;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationTest {

  private static final String DUPLICATION_PROJECT_KEY = "org.sonarsource.it.projects:test-duplications";

  @ClassRule
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void duplication_should_be_computed_by_SQ() {
    MavenBuild build = TestUtils.createMavenBuild()
      .setPom(TestUtils.projectPom("test-duplications")).setCleanPackageSonarGoals();

    orchestrator.executeBuild(build);

    assertThat(getMeasureAsDouble(DUPLICATION_PROJECT_KEY, "duplicated_lines_density")).isEqualTo(39.6);
    assertThat(getMeasureAsInteger(DUPLICATION_PROJECT_KEY, "duplicated_lines")).isEqualTo(36);
    assertThat(getMeasureAsInteger(DUPLICATION_PROJECT_KEY, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasureAsInteger(DUPLICATION_PROJECT_KEY, "duplicated_blocks")).isEqualTo(2);
  }

}
