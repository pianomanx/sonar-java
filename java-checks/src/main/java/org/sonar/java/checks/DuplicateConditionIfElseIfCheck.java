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
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1862")
public class DuplicateConditionIfElseIfCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    ExpressionTree condition = tree.condition();

    StatementTree statement = tree.elseStatement();
    while (statement != null && statement.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) statement;
      if (areTriviallyEquivalent(condition, ifStatement.condition())) {
        context.reportIssue(
          this,
          ifStatement.condition(),
          "This branch can not be reached because the condition duplicates a previous condition in the same sequence of \"if/else if\" statements",
          Collections.singletonList(new JavaFileScannerContext.Location("Original", condition)),
          null
        );
      }
      statement = ifStatement.elseStatement();
    }

    super.visitIfStatement(tree);
  }

  private static boolean areTriviallyEquivalent(ExpressionTree condition1, ExpressionTree condition2) {
    ExpressionTree cleanCondition1 = ExpressionUtils.skipParentheses(condition1);
    ExpressionTree cleanCondition2 = ExpressionUtils.skipParentheses(condition2);
    if (cleanCondition1.is(Tree.Kind.EQUAL_TO) && cleanCondition2.is(Tree.Kind.EQUAL_TO)) {
      BinaryExpressionTree binary1 = (BinaryExpressionTree) cleanCondition1;
      BinaryExpressionTree binary2 = (BinaryExpressionTree) cleanCondition2;
      // a == b
      return (areTriviallyEquivalent(binary1.leftOperand(), binary2.leftOperand())
        && areTriviallyEquivalent(binary1.rightOperand(), binary2.rightOperand()))
        // b == a
        || (areTriviallyEquivalent(binary1.leftOperand(), binary2.rightOperand())
        && areTriviallyEquivalent(binary1.rightOperand(), binary2.leftOperand()));
    }
    return SyntacticEquivalence.areEquivalent(cleanCondition1, cleanCondition2);
  }

}
