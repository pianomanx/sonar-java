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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5413")
public class SuspiciousListRemoveCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers LIST_REMOVE = MethodMatchers.create()
    .ofTypes("java.util.List")
    .names("remove")
    .addParametersMatcher("int")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForStatementTree forStatementTree = (ForStatementTree) tree;
    Symbol counter = findLoopCounter(forStatementTree);
    if (counter == null || !isIncrementingLoop(forStatementTree, counter)) {
      return;
    }
    StatementTree loopBody = forStatementTree.statement();
    LoopBodyVisitor loopBodyVisitor = new LoopBodyVisitor(counter);
    loopBody.accept(loopBodyVisitor);
    if (loopBodyVisitor.hasIssue()) {
      reportIssue(loopBodyVisitor.listRemove, "Verify that \"remove()\" is used correctly.");
    }
  }

  @CheckForNull
  private static Symbol findLoopCounter(ForStatementTree forStatementTree) {
    if (forStatementTree.initializer().size() != 1) {
      return null;
    }
    StatementTree initializer = forStatementTree.initializer().get(0);
    if (!initializer.is(Tree.Kind.VARIABLE)) {
      return null;
    }
    return  ((VariableTree) initializer).symbol();
  }

  private static boolean isIncrementingLoop(ForStatementTree forStatementTree, Symbol counter) {
    if (forStatementTree.update().size() != 1) {
      return false;
    }
    StatementTree loopUpdate = forStatementTree.update().get(0);
    if (loopUpdate.is(Tree.Kind.EXPRESSION_STATEMENT)
      && ((ExpressionStatementTree) loopUpdate).expression().is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_INCREMENT)) {
      ExpressionTree expression = ((UnaryExpressionTree) ((ExpressionStatementTree) loopUpdate).expression()).expression();
      return expression.is(Tree.Kind.IDENTIFIER) && counter.equals(((IdentifierTree) expression).symbol());
    }
    return false;
  }

  private static class LoopBodyVisitor extends BaseTreeVisitor {
    private final Symbol counter;
    private MethodInvocationTree listRemove;
    private boolean hasBreakOrContinueOrReturn;
    private boolean isCounterAssigned;

    public LoopBodyVisitor(Symbol counter) {
      this.counter = counter;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (LIST_REMOVE.matches(tree)) {
        listRemove = tree;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      hasBreakOrContinueOrReturn = true;
      super.visitBreakStatement(tree);
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      hasBreakOrContinueOrReturn = true;
      super.visitContinueStatement(tree);
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      hasBreakOrContinueOrReturn = true;
      super.visitReturnStatement(tree);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        isCounterAssigned |= counter.equals(((IdentifierTree) tree.variable()).symbol());
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.expression().is(Tree.Kind.IDENTIFIER)) {
        isCounterAssigned |= counter.equals(((IdentifierTree) tree.expression()).symbol());
      }
      super.visitUnaryExpression(tree);
    }

    boolean hasIssue() {
      return listRemove != null && !hasBreakOrContinueOrReturn && !isCounterAssigned;
    }
  }

}
