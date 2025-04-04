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
package org.sonar.java.checks;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2133")
public class ObjectCreatedOnlyToCallGetClassCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofAnyType().names("getClass").addWithoutParametersMatcher().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.methodSelect()
      .is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expressionTree = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (expressionTree.is(Tree.Kind.NEW_CLASS, Tree.Kind.NEW_ARRAY)) {
        reportIssue(expressionTree);
      } else if (expressionTree.is(Tree.Kind.IDENTIFIER) && variableUsedOnlyToGetClass((IdentifierTree) expressionTree)) {
        reportIssue(getInitializer((IdentifierTree) expressionTree));
      }
    }
  }

  @CheckForNull
  private static ExpressionTree getInitializer(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol.isVariableSymbol()) {
      VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
      if (declaration != null) {
        return declaration.initializer();
      }
    }
    return null;
  }

  private static boolean variableUsedOnlyToGetClass(IdentifierTree tree) {
    if ("this".equals(tree.name()) || "super".equals(tree.name())) {
      return false;
    }
    Symbol symbol = tree.symbol();
    return symbol.usages().size() == 1 && hasBeenInitialized(tree);
  }

  private static boolean hasBeenInitialized(IdentifierTree tree) {
    ExpressionTree initializer = getInitializer(tree);
    return initializer != null && initializer.is(Tree.Kind.NEW_CLASS);
  }

  private void reportIssue(@Nullable ExpressionTree expressionTree) {
    if (expressionTree != null) {
      reportIssue(expressionTree, "Remove this object instantiation and use \"" + getTypeName(expressionTree) + ".class\" instead.");
    }
  }

  private static String getTypeName(ExpressionTree tree) {
    Type type = tree.symbolType();
    String name = getTypeName(type);
    if (name.isEmpty()) {
      name = getAnonymousClassTypeName(type.symbol());
    }
    return name;
  }

  private static String getAnonymousClassTypeName(Symbol.TypeSymbol symbol) {
    if (symbol.interfaces().isEmpty()) {
      return getTypeName(symbol.superClass());
    }
    return getTypeName(symbol.interfaces().get(0));
  }

  private static String getTypeName(Type type) {
    return type.symbol().name();
  }

}
