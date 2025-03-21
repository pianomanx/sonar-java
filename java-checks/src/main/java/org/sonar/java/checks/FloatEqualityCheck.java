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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1244")
public class FloatEqualityCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.Double", "java.lang.Float")
    .names("equals")
    .addParametersMatcher("java.lang.Object")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO, Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if(tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (EQUALS_MATCHER.matches(mit)) {
        reportIssue(mit.methodSelect(), "Equality tests should not be made with floating point values.");
      }
      return;
    }

    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    if (binaryExpressionTree.is(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR) && isIndirectEquality(binaryExpressionTree)) {
      binaryExpressionTree = (BinaryExpressionTree) binaryExpressionTree.leftOperand();
    }
    if ((hasFloatingType(binaryExpressionTree.leftOperand()) || hasFloatingType(binaryExpressionTree.rightOperand())) && !isNanTest(binaryExpressionTree)) {
      reportIssue(binaryExpressionTree.operatorToken(), "Equality tests should not be made with floating point values.");
    }
  }

  private static boolean isIndirectEquality(BinaryExpressionTree binaryExpressionTree) {
    return isIndirectEquality(binaryExpressionTree, Tree.Kind.CONDITIONAL_AND, Tree.Kind.GREATER_THAN_OR_EQUAL_TO, Tree.Kind.LESS_THAN_OR_EQUAL_TO)
      || isIndirectEquality(binaryExpressionTree, Tree.Kind.CONDITIONAL_OR, Tree.Kind.GREATER_THAN, Tree.Kind.LESS_THAN);
  }

  private static boolean isIndirectEquality(BinaryExpressionTree binaryExpressionTree, Tree.Kind indirectOperator, Tree.Kind comparator1, Tree.Kind comparator2) {
    if (binaryExpressionTree.is(indirectOperator) && binaryExpressionTree.leftOperand().is(comparator1, comparator2)) {
      BinaryExpressionTree leftOp = (BinaryExpressionTree) binaryExpressionTree.leftOperand();
      if (binaryExpressionTree.rightOperand().is(comparator1, comparator2)) {
        BinaryExpressionTree rightOp = (BinaryExpressionTree) binaryExpressionTree.rightOperand();
        if (leftOp.kind().equals(rightOp.kind())) {
          //same operator
          return SyntacticEquivalence.areEquivalent(leftOp.leftOperand(), rightOp.rightOperand())
            && SyntacticEquivalence.areEquivalent(leftOp.rightOperand(), rightOp.leftOperand());
        } else {
          //different operator
          return SyntacticEquivalence.areEquivalent(leftOp.leftOperand(), rightOp.leftOperand())
            && SyntacticEquivalence.areEquivalent(leftOp.rightOperand(), rightOp.rightOperand());
        }
      }
    }
    return false;
  }


  private static boolean isNanTest(BinaryExpressionTree binaryExpressionTree) {
    return SyntacticEquivalence.areEquivalent(binaryExpressionTree.leftOperand(), binaryExpressionTree.rightOperand());
  }

  private static boolean hasFloatingType(ExpressionTree expressionTree) {
    return expressionTree.symbolType().isPrimitive(Type.Primitives.FLOAT) || expressionTree.symbolType().isPrimitive(Type.Primitives.DOUBLE);
  }

}
