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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Helper class to be used to find the latest {@link ExpressionTree} used as initializer (for a {@link VariableTree}) 
 * or expression used in assignment (for a {@link AssignmentExpressionTree}) for a given variable.
 */
public final class ReassignmentFinder {

  private ReassignmentFinder() {
  }

  @CheckForNull
  public static ExpressionTree getClosestReassignmentOrDeclarationExpression(Tree startingPoint, Symbol referenceSymbol) {
    Tree result = referenceSymbol.declaration();
    List<IdentifierTree> usages = referenceSymbol.usages();
    if (usages.size() != 1) {
      List<AssignmentExpressionTree> reassignments = getReassignments(referenceSymbol.owner().declaration(), usages);

      SyntaxToken startPointToken = startingPoint.firstToken();
      Tree lastReassignment = getClosestReassignment(startPointToken, reassignments);
      if (lastReassignment != null) {
        result = lastReassignment;
      }
    }

    ExpressionTree initializerOrExpression = getInitializerOrExpression(result);
    if (initializerOrExpression == startingPoint) {
      return getClosestReassignmentOrDeclarationExpression(result, referenceSymbol);
    }
    return initializerOrExpression;
  }

  @CheckForNull
  public static ExpressionTree getInitializerOrExpression(@Nullable Tree tree) {
    if (tree == null) {
      return null;
    }
    if (tree.is(Tree.Kind.VARIABLE)) {
      return ((VariableTree) tree).initializer();
    } else if (tree.is(Tree.Kind.ENUM_CONSTANT)) {
      return ((EnumConstantTree) tree).initializer();
    } else if (tree instanceof AssignmentExpressionTree assignmentExpressionTree) {
      // All kinds of Assignment
      return assignmentExpressionTree.expression();
    }
    // Can be other declaration, like class
    return null;
  }

  public static List<AssignmentExpressionTree> getReassignments(@Nullable Tree ownerDeclaration, List<IdentifierTree> usages) {
    if (ownerDeclaration != null) {
      List<AssignmentExpressionTree> assignments = new ArrayList<>();
      for (IdentifierTree usage : usages) {
        checkAssignment(usage).ifPresent(assignments::add);
      }
      return assignments;
    }
    return new ArrayList<>();
  }

  private static Optional<AssignmentExpressionTree> checkAssignment(IdentifierTree usage) {
    Tree previousTree = usage;
    Tree nonParenthesisParent = previousTree.parent();

    while (nonParenthesisParent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      previousTree = nonParenthesisParent;
      nonParenthesisParent = previousTree.parent();
    }

    if (nonParenthesisParent instanceof AssignmentExpressionTree assignment && assignment.variable().equals(previousTree)) {
      return Optional.of(assignment);
    }
    return Optional.empty();
  }

  @CheckForNull
  private static Tree getClosestReassignment(SyntaxToken startToken, List<AssignmentExpressionTree> reassignments) {
    return reassignments.stream()
      .filter(a -> Position.startOf(a).isBefore(Position.startOf(startToken)))
      .max(Comparator.comparing(Position::startOf))
      .orElse(null);
  }

}
