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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4433")
public class LDAPAuthenticatedConnectionCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofSubTypes("java.util.Map")
      .names("put")
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    if (methodTree.arguments().size() != 2) {
      return;
    }
    ExpressionTree putKey = methodTree.arguments().get(0);
    ExpressionTree putValue = methodTree.arguments().get(1);
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(putValue);
    ExpressionTree mechanismTree = defaultPropertyValue == null ? putValue : defaultPropertyValue;
    if (isSecurityAuthenticationConstant(putKey) && LiteralUtils.hasValue(mechanismTree, "none")) {
      reportIssue(putValue, "Change authentication to \"simple\" or stronger.");
    }
  }

  private static boolean isSecurityAuthenticationConstant(ExpressionTree tree) {
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree constantExpression = (MemberSelectExpressionTree) tree;
      return "javax.naming.Context".equals(constantExpression.expression().symbolType().fullyQualifiedName())
        && "SECURITY_AUTHENTICATION".equals(constantExpression.identifier().name());
    }
    return LiteralUtils.hasValue(tree, "java.naming.security.authentication");
  }
}
