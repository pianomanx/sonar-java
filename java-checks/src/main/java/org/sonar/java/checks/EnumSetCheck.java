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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1641")
public class EnumSetCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers COLLECTIONS_UNMODIFIABLE = MethodMatchers.create()
    .ofTypes("java.util.Collections")
    .names("unmodifiableSet")
    .withAnyParameters()
    .build();

  private static final MethodMatchers SET_CREATION_METHODS = MethodMatchers.or(
    // Java 9 factory methods
    MethodMatchers.create().ofTypes("java.util.Set").names("of").withAnyParameters().build(),
    // guava
    MethodMatchers.create().ofTypes("com.google.common.collect.ImmutableSet").names("of").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Sets").anyName().withAnyParameters().build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    ExpressionTree initializer = variableTree.initializer();
    if (initializer == null) {
      return;
    }
    if (initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) initializer;
      if (COLLECTIONS_UNMODIFIABLE.matches(mit)) {
        // check the collection used as parameter
        initializer = mit.arguments().get(0);
      } else if (!SET_CREATION_METHODS.matches(mit) || "immutableEnumSet".equals(mit.methodSymbol().name())) {
        // Methods from Guava 'Sets' except 'immutableEnumSet' should be checked,
        // but discard any other method invocations (killing the noise)
        return;
      }
    }
    if (!initializer.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS)) {
      // We are not trying to compute the exact type of more complex expressions
      return;
    }
    checkIssue(initializer.symbolType(), initializer);
  }

  private void checkIssue(Type type, Tree reportTree) {
    if (type.isSubtypeOf("java.util.Set") && !type.isSubtypeOf("java.util.EnumSet") && type.isParameterized()) {
      Type typeArgument = type.typeArguments().get(0);
      if (typeArgument != null && typeArgument.symbol().isEnum()) {
        reportIssue(reportTree, "Convert this Set to an EnumSet.");
      }
    }
  }

}
