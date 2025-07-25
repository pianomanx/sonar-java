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
package org.sonar.plugins.surefire.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Strings;

public final class UnitTestClassReport {
  private int errors = 0;
  private int failures = 0;
  private int skipped = 0;
  private int tests = 0;
  private long durationMilliseconds = 0L;


  private long negativeTimeTestNumber = 0L;
  private List<UnitTestResult> results = null;

  public UnitTestClassReport add(UnitTestClassReport other) {
    for (UnitTestResult otherResult : other.getResults()) {
      add(otherResult);
    }
    return this;
  }

  public UnitTestClassReport add(UnitTestResult result) {
    initResults();
    boolean hasName = results.stream().map(UnitTestResult::getName).anyMatch(result.getName()::equals);
    if (hasName && Strings.CS.contains(result.getName(), "$")) {
      return this;
    }
    results.add(result);
    if (result.getStatus().equals(UnitTestResult.STATUS_SKIPPED)) {
      skipped += 1;

    } else if (result.getStatus().equals(UnitTestResult.STATUS_FAILURE)) {
      failures += 1;

    } else if (result.getStatus().equals(UnitTestResult.STATUS_ERROR)) {
      errors += 1;
    }
    tests += 1;
    if (result.getDurationMilliseconds() < 0) {
      negativeTimeTestNumber += 1;
    } else {
      durationMilliseconds += result.getDurationMilliseconds();
    }
    return this;
  }

  private void initResults() {
    if (results == null) {
      results = new ArrayList<>();
    }
  }

  public int getErrors() {
    return errors;
  }

  public int getFailures() {
    return failures;
  }

  public int getSkipped() {
    return skipped;
  }

  public int getTests() {
    return tests;
  }

  public long getDurationMilliseconds() {
    return durationMilliseconds;
  }

  public long getNegativeTimeTestNumber() {
    return negativeTimeTestNumber;
  }

  public List<UnitTestResult> getResults() {
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }
}
