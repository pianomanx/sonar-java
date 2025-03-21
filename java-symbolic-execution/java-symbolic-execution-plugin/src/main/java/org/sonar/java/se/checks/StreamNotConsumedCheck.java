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
package org.sonar.java.se.checks;

import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.sonar.java.se.checks.StreamConsumedCheck.StreamPipelineConstraint.NOT_CONSUMED;

/**
 * This check is used just to report issues. Most of the check logic is implemented in {@link StreamConsumedCheck}
 */
@Rule(key = "S3958")
public class StreamNotConsumedCheck extends SECheck {

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    if (context.getState().exitValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      // don't report when exiting on exception
      return;
    }
    ProgramState state = context.getState();
    List<SymbolicValue> notConsumed = state.getValuesWithConstraints(NOT_CONSUMED);
    notConsumed.forEach(sv -> {
      Set<Flow> flows = FlowComputation.flow(context.getNode(), Collections.singleton(sv),
        NOT_CONSUMED::equals, NOT_CONSUMED::equals,
        Collections.singletonList(StreamConsumedCheck.StreamPipelineConstraint.class),
        Collections.emptySet(), FlowComputation.FIRST_FLOW);
      Flow flow = flows.iterator().next();
      var elements = flow.elements();
      JavaFileScannerContext.Location location = elements.get(elements.size() - 1);
      reportIssue(location.syntaxNode, "Refactor the code so this stream pipeline is used.");
    });
  }
}
