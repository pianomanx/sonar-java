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
package org.sonar.java.checks.helpers;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;

public class JavaPropertiesHelper {

  private JavaPropertiesHelper() {
  }

  /**
   * If the provided expression is an {@link IdentifierTree} or a {@link MethodInvocationTree}, it will check if it used to retrieve 
   * a property with a default value provided (using {@link java.util.Properties#getProperty(String, String)}).
   * @param expression
   * @return null The default value of the getProperty method invocation, or null if the expression is not of the expected kind 
   * or if is not used to retrieve a property with a default value. 
   * . 
   */
  @CheckForNull
  public static ExpressionTree retrievedPropertyDefaultValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return retrievedPropertyDefaultValue((IdentifierTree) expression);
    } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      return retrievedPropertyDefaultValue((MethodInvocationTree) expression);
    }
    return null;
  }

  @CheckForNull
  private static ExpressionTree retrievedPropertyDefaultValue(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (symbol.usages().size() == 1) {
      VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
      if (declaration != null) {
        ExpressionTree initializer = declaration.initializer();
        if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION)) {
          return retrievedPropertyDefaultValue((MethodInvocationTree) initializer);
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static ExpressionTree retrievedPropertyDefaultValue(MethodInvocationTree mit) {
    if (isGetPropertyWithDefaultValue(mit)) {
      return mit.arguments().get(1);
    }
    return null;
  }

  private static boolean isGetPropertyWithDefaultValue(MethodInvocationTree mit) {
    Symbol symbol = mit.methodSymbol();
    if (symbol.isMethodSymbol() && symbol.owner().type().is("java.util.Properties")) {
      return "getProperty".equals(symbol.name()) && mit.arguments().size() == 2;
    }
    return false;
  }
}
