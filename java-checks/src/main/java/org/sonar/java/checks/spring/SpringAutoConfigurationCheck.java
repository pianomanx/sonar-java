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
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4604")
public class SpringAutoConfigurationCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ANNOTATIONS = Arrays.asList(
    "org.springframework.boot.autoconfigure.SpringBootApplication",
    "org.springframework.boot.autoconfigure.EnableAutoConfiguration");

  private static final Set<String> EXCLUDE_ELEMENTS = new HashSet<>(Arrays.asList("exclude", "excludeName"));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ((ClassTree) tree).modifiers().annotations().stream()
      .filter(SpringAutoConfigurationCheck::isAutoConfiguration)
      .filter(annotation -> !hasExclude(annotation.arguments()))
      .forEach(annotation -> reportIssue(annotation, "Exclude from the auto-configuration mechanism the beans you don't need."));
  }

  private static boolean isAutoConfiguration(AnnotationTree annotationTree) {
    return ANNOTATIONS.stream().anyMatch(annotationTree.annotationType().symbolType()::is);
  }

  private static boolean hasExclude(Arguments arguments) {
    return arguments.stream()
      .filter(arg -> arg.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .anyMatch(SpringAutoConfigurationCheck::isExcludeElement);
  }

  private static boolean isExcludeElement(AssignmentExpressionTree assignment) {
    ExpressionTree expression = assignment.expression();
    boolean isExcludeElement = EXCLUDE_ELEMENTS.contains(assignment.variable().toString());
    boolean arrayNotEmpty = expression.is(Tree.Kind.NEW_ARRAY) && !((NewArrayTree) expression).initializers().isEmpty();
    return isExcludeElement && arrayNotEmpty;
  }
}
