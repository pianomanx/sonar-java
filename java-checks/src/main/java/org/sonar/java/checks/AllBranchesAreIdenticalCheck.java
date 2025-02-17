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
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3923")
public class AllBranchesAreIdenticalCheck extends IdenticalCasesInSwitchCheck {

  private static final String IF_SWITCH_MSG = "Remove this conditional structure or edit its code blocks so that they're not all the same.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION, Tree.Kind.IF_STATEMENT, Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION)) {
      SwitchTree switchTree = (SwitchTree) tree;
      Map<CaseGroupTree, Set<CaseGroupTree>> identicalBranches = checkSwitchStatement(switchTree);
      if (hasDefaultClause(switchTree) && allBranchesSame(identicalBranches, switchTree.cases().size())) {
        reportIssue(switchTree.switchKeyword(), IF_SWITCH_MSG);
      }
    } else if (tree.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatementTree = (IfStatementTree) tree;
      if (hasElseClause(ifStatementTree) && !tree.parent().is(Tree.Kind.IF_STATEMENT)) {
        IfElseChain ifElseChain = checkIfStatement(ifStatementTree);
        if (allBranchesSame(ifElseChain.branches, ifElseChain.totalBranchCount)) {
          reportIssue(ifStatementTree.ifKeyword(), IF_SWITCH_MSG);
        }
      }
    } else {
      checkConditionalExpression((ConditionalExpressionTree) tree);
    }
  }

  private void checkConditionalExpression(ConditionalExpressionTree node) {
    if (SyntacticEquivalence.areEquivalent(ExpressionUtils.skipParentheses(node.trueExpression()), ExpressionUtils.skipParentheses(node.falseExpression()))) {
      reportIssue(node.condition(), node.questionToken(), "This conditional operation returns the same value whether the condition is \"true\" or \"false\".");
    }
  }
}
