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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.hasTestAnnotation;

@Rule(key = "S3578")
public class BadTestMethodNameCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_FORMAT = "^test[A-Z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression the test method names are checked against.",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    super.setContext(context);
  }


  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isNotOverridden(methodTree) && hasTestAnnotation(methodTree) && !pattern.matcher(methodTree.simpleName().name()).matches()) {
      reportIssue(methodTree.simpleName(), "Rename this method name to match the regular expression: '" + format + "'");
    }
  }

  private static boolean isNotOverridden(MethodTree methodTree) {
    return Boolean.FALSE.equals(methodTree.isOverriding());
  }

}
