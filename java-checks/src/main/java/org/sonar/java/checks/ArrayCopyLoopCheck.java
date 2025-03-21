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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S3012")
public class ArrayCopyLoopCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers COLLECTION_ADD = MethodMatchers.create().ofSubTypes("java.util.Collection").names("add").addParametersMatcher(ANY).build();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.FOR_STATEMENT, Kind.FOR_EACH_STATEMENT, Kind.WHILE_STATEMENT, Kind.DO_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    StatementTree statement;
    if (tree.is(Kind.FOR_STATEMENT)) {
      statement = checkFor((ForStatementTree) tree);
    } else if (tree.is(Kind.WHILE_STATEMENT)) {
      statement = checkDoOrWhile((WhileStatementTree) tree);
    } else if (tree.is(Kind.DO_STATEMENT)) {
      statement = checkDoOrWhile((DoWhileStatementTree) tree);
    } else {
      statement = checkForEach((ForEachStatement) tree);
    }
    if (statement != null) {
      reportIssue(statement, "Use \"Arrays.copyOf\", \"Arrays.asList\", \"Collections.addAll\" or \"System.arraycopy\" instead.");
    }
  }

  @CheckForNull
  private static StatementTree checkFor(ForStatementTree tree) {
    ListTree<StatementTree> updates = tree.update();
    if (updates.size() == 1) {
      StatementTree update = updates.get(0);
      Symbol counter = checkUpdate(update);
      if (counter != null) {
        ExpressionTree condition = tree.condition();
        if (condition != null && checkCondition(condition, counter)) {
          StatementTree statement = getStatement(tree);
          if (statement != null && checkStatement(statement, counter)) {
            return statement;
          }
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static StatementTree checkDoOrWhile(StatementTree tree) {
    StatementTree doOrWhile = statement(tree);
    if (doOrWhile.is(Kind.BLOCK)) {
      BlockTree block = (BlockTree) doOrWhile;
      List<StatementTree> body = block.body();
      if (body.size() == 2) {
        StatementTree update = body.get(1);
        Symbol counter = checkUpdate(update);
        if (counter != null) {
          ExpressionTree condition = condition(tree);
          if (checkCondition(condition, counter)) {
            StatementTree statement = body.get(0);
            if (checkStatement(statement, counter)) {
              return statement;
            }
          }
        }
      }
    }
    return null;
  }

  private static ExpressionTree condition(StatementTree tree) {
    return tree.is(Kind.WHILE_STATEMENT) ? ((WhileStatementTree) tree).condition() : ((DoWhileStatementTree) tree).condition();
  }

  private static StatementTree statement(StatementTree tree) {
    return tree.is(Kind.WHILE_STATEMENT) ? ((WhileStatementTree) tree).statement() : ((DoWhileStatementTree) tree).statement();
  }

  @CheckForNull
  private static StatementTree checkForEach(ForEachStatement tree) {
    ExpressionTree expression = tree.expression();
    if (expression.symbolType().isArray()) {
      StatementTree statement = getStatement(tree);
      if (statement != null && statement.is(Kind.EXPRESSION_STATEMENT)) {
        expression = ((ExpressionStatementTree) statement).expression();
        if (isArrayToListCopy(expression, tree.variable())) {
          return statement;
        }
      }
    }
    return null;
  }

  private static boolean checkCondition(ExpressionTree tree, Symbol counter) {
    if (tree.is(Kind.LESS_THAN, Kind.LESS_THAN_OR_EQUAL_TO, Kind.GREATER_THAN, Kind.GREATER_THAN_OR_EQUAL_TO, Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree comparison = (BinaryExpressionTree) tree;
      ExpressionTree lhs = comparison.leftOperand();
      ExpressionTree rhs = comparison.rightOperand();
      // the XOR makes sure we don't allow `i < i` as the loop termination condition
      return isCounter(lhs, counter) ^ isCounter(rhs, counter);
    }
    return false;
  }

  @CheckForNull
  private static Symbol checkUpdate(StatementTree tree) {
    Symbol counter = null;
    if (tree.is(Kind.EXPRESSION_STATEMENT)) {
      ExpressionStatementTree update = (ExpressionStatementTree) tree;
      counter = isIncrement(update);
      if (counter == null) {
        counter = isPlusAssignment(update);
        if (counter == null) {
          counter = isAssignment(update);
        }
      }
    }
    return counter;
  }

  private static boolean checkStatement(StatementTree tree, Symbol counter) {
    if (tree.is(Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) tree).expression();
      return isArrayToArrayCopy(expression, counter) || isArrayToListCopy(expression, counter);
    }
    return false;
  }

  @CheckForNull
  private static Symbol isIncrement(ExpressionStatementTree update) {
    if (update.expression().is(Kind.POSTFIX_INCREMENT, Kind.PREFIX_INCREMENT)) {
      UnaryExpressionTree increment = (UnaryExpressionTree) update.expression();
      if (increment.expression().is(Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) increment.expression();
        return identifier.symbol();
      }
    }
    return null;
  }

  @CheckForNull
  private static Symbol isPlusAssignment(ExpressionStatementTree update) {
    if (update.expression().is(Kind.PLUS_ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) update.expression();
      ExpressionTree lhs = assignment.variable();
      ExpressionTree rhs = assignment.expression();
      if (lhs.is(Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) lhs;
        if (isOne(rhs)) {
          return identifier.symbol();
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static Symbol isAssignment(ExpressionStatementTree update) {
    if (update.expression().is(Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) update.expression();
      ExpressionTree lhs = assignment.variable();
      ExpressionTree rhs = assignment.expression();
      if (lhs.is(Kind.IDENTIFIER) && isIntegerType(lhs)) {
        Symbol counter = ((IdentifierTree) lhs).symbol();
        if (rhs.is(Kind.PLUS)) {
          BinaryExpressionTree addition = (BinaryExpressionTree) rhs;
          lhs = addition.leftOperand();
          rhs = addition.rightOperand();
          if ((isCounter(lhs, counter) && isOne(rhs)) || (isOne(lhs) && isCounter(rhs, counter))) {
            return counter;
          }
        }
      }
    }
    return null;
  }

  private static boolean isArrayToArrayCopy(ExpressionTree expression, Symbol counter) {
    if (expression.is(Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      ExpressionTree lhs = assignment.variable();
      ExpressionTree rhs = assignment.expression();
      if (lhs.is(Kind.ARRAY_ACCESS_EXPRESSION) && rhs.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
        ArrayAccessExpressionTree src = (ArrayAccessExpressionTree) rhs;
        ArrayAccessExpressionTree dst = (ArrayAccessExpressionTree) lhs;
        // check they are not boxing or unboxing; this can happen if we have different types and one side is primitive
        if (!src.symbolType().equals(dst.symbolType()) && (src.symbolType().isPrimitive() || dst.symbolType().isPrimitive())) {
          return false;
        }
        // otherwise determine the expressions
        return isCounter(src.dimension().expression(), counter) && isCounter(dst.dimension().expression(), counter);
      }
    }
    return false;
  }

  private static boolean isArrayToListCopy(ExpressionTree expression, Symbol counter) {
    if (expression.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocation = (MethodInvocationTree) expression;
      if (COLLECTION_ADD.matches(invocation) && invocation.methodSelect().is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree select = (MemberSelectExpressionTree) invocation.methodSelect();
        if (select.expression().is(Kind.IDENTIFIER)) {
          ExpressionTree argument = invocation.arguments().get(0);
          if (argument.is(Kind.ARRAY_ACCESS_EXPRESSION) && !argument.symbolType().isPrimitive()) {
            ArrayAccessExpressionTree access = (ArrayAccessExpressionTree) argument;
            return access.expression().is(Kind.IDENTIFIER) && isCounter(access.dimension().expression(), counter);
          }
        }
      }
    }
    return false;
  }

  private static boolean isArrayToListCopy(ExpressionTree expression, VariableTree iterated) {
    if (expression.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocation = (MethodInvocationTree) expression;
      if (COLLECTION_ADD.matches(invocation)) {
        expression = invocation.methodSelect();
        if (expression.is(Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree select = (MemberSelectExpressionTree) expression;
          if (select.expression().is(Kind.IDENTIFIER)) {
            ExpressionTree argument = invocation.arguments().get(0);
            Symbol identifier = getIdentifier(argument);
            return identifier != null && identifier.equals(iterated.symbol()) && !argument.symbolType().isPrimitive();
          }
        }
      }
    }
    return false;
  }

  @CheckForNull
  private static Symbol getIdentifier(ExpressionTree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).symbol();
    }
    return null;
  }

  private static boolean isCounter(ExpressionTree tree, Symbol counter) {
    Symbol identifier = getIdentifier(tree);
    return identifier != null && identifier.equals(counter);
  }

  private static boolean isIntegerType(ExpressionTree tree) {
    return tree.symbolType().isPrimitive(Primitives.INT) || tree.symbolType().isPrimitive(Primitives.LONG);
  }

  private static boolean isOne(ExpressionTree tree) {
    return Long.valueOf(1L).equals(LiteralUtils.longLiteralValue(tree));
  }

  @CheckForNull
  private static StatementTree getStatement(Tree tree) {
    if (tree.is(Kind.FOR_STATEMENT)) {
      ForStatementTree loop = (ForStatementTree) tree;
      return getBody(loop.statement(), 1);
    }
    ForEachStatement loop = (ForEachStatement) tree;
    return getBody(loop.statement(), 1);
  }

  @CheckForNull
  private static StatementTree getBody(StatementTree tree, int expectedSize) {
    if (tree.is(Kind.BLOCK)) {
      BlockTree block = (BlockTree) tree;
      List<StatementTree> body = block.body();
      tree = body.size() == expectedSize ? body.get(0) : null;
    }
    return tree;
  }
}
