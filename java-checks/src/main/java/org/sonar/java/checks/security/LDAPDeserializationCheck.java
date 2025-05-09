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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4434")
public class LDAPDeserializationCheck extends AbstractMethodDetection {
  private static final String CONSTRUCTOR_NAME = "<init>";
  private static final String CLASS_NAME = "javax.naming.directory.SearchControls";
  private static final int RET_OBJ_INDEX = 4;

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofSubTypes(CLASS_NAME).names(CONSTRUCTOR_NAME).withAnyParameters().build(),
      MethodMatchers.create().ofSubTypes(CLASS_NAME).names("setReturningObjFlag").addParametersMatcher("boolean").build());
  }
  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    checkConstructorArguments(newClassTree.arguments());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    if (CONSTRUCTOR_NAME.equals(methodTree.methodSymbol().name())) {
      // when calling super() for classes extending SearchControls
      checkConstructorArguments(methodTree.arguments());
    } else {
      ExpressionTree setValue = methodTree.arguments().get(0);
      reportIfTrue(setValue);
    }
  }

  private void checkConstructorArguments(Arguments args) {
    if (args.size() <= RET_OBJ_INDEX) {
      return;
    }
    ExpressionTree retObjArgument = args.get(RET_OBJ_INDEX);
    reportIfTrue(retObjArgument);
  }

  private void reportIfTrue(ExpressionTree toUnderline) {
    if (LiteralUtils.isTrue(toUnderline)) {
      reportIssue(toUnderline, "Disable object deserialization.");
    }
  }
}
