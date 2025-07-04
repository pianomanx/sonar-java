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

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6830")
public class SpringBeanNamingConventionCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ANNOTATIONS_TO_CHECK = List.of(
    "org.springframework.beans.factory.annotation.Qualifier",
    SpringUtils.BEAN_ANNOTATION,
    SpringUtils.CONFIGURATION_ANNOTATION,
    SpringUtils.CONTROLLER_ANNOTATION,
    SpringUtils.COMPONENT_ANNOTATION,
    SpringUtils.REPOSITORY_ANNOTATION,
    SpringUtils.SERVICE_ANNOTATION,
    SpringUtils.REST_CONTROLLER_ANNOTATION);

  private static final Pattern NAMING_CONVENTION = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var annotation = (AnnotationTree) tree;
    ANNOTATIONS_TO_CHECK.stream().filter(a -> annotation.symbolType().is(a)).findFirst()
      .map(a -> getNoncompliantNameArgument(annotation))
      .ifPresent(n -> reportIssue(n, "Rename this bean to match the regular expression '" + NAMING_CONVENTION.pattern() + "'."));
  }

  @CheckForNull
  private static ExpressionTree getNoncompliantNameArgument(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .map(arg -> {
        if (breaksNamingConvention(getArgValue(arg))) {
          return arg;
        } else  {
          return null;
        }
      }).filter(Objects::nonNull).findFirst().orElse(null);
  }

  private static ExpressionTree getArgValue(ExpressionTree argument) {
    if (argument.is(Tree.Kind.ASSIGNMENT)) {
      var assignment = (AssignmentExpressionTree) argument;
      var argName = ((IdentifierTree) assignment.variable()).name();
      var argValue = assignment.expression();
      if (argName.equals("name") || argName.equals("value")) {
        return argValue;
      }
    } else {
      return argument;
    }
    return null;
  }

  private static boolean breaksNamingConvention(@Nullable ExpressionTree nameTree) {
    if (nameTree == null) {
      return false;
    } else {
      var name = ExpressionsHelper.getConstantValueAsString(nameTree).value();
      return name != null && !NAMING_CONVENTION.matcher(name).matches();
    }
  }
}
