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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import static org.sonar.java.model.ExpressionUtils.skipParenthesesUpwards;

@Rule(key = "S1905")
public class RedundantTypeCastCheck extends IssuableSubscriptionVisitor {

  private List<JWarning> warnings;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings = ((JavaTree.CompilationUnitTreeImpl) tree).warnings(JProblem.Type.REDUNDANT_CAST);
      return;
    }

    TypeCastTree typeCastTree = (TypeCastTree) tree;
    Type cast = typeCastTree.type().symbolType();
    if (isUnnecessaryCast(typeCastTree)) {
      String newType = cast.erasure().name();
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onRange(typeCastTree.openParenToken(), typeCastTree.closeParenToken())
        .withMessage("Remove this unnecessary cast to \"%s\".", newType)
        .withQuickFix(() ->
          JavaQuickFix.newQuickFix("Remove the cast to \"%s\"", newType)
            .addTextEdit(JavaTextEdit.removeTextSpan(
              AnalyzerMessage.textSpanBetween(
                typeCastTree.openParenToken(), true,
                typeCastTree.expression(), false)))
            .build())
        .report();
    }
  }

  public static Tree skipParentheses(Tree tree) {
    if (tree instanceof ExpressionTree expressionTree) {
      return ExpressionUtils.skipParentheses(expressionTree);
    }
    return tree;
  }

  private boolean isUnnecessaryCast(TypeCastTree typeCastTree) {
    if (skipParentheses(typeCastTree.expression()).is(Tree.Kind.NULL_LITERAL)) {
      Tree parentTree = skipParentheses(typeCastTree.parent());
      return !parentTree.is(Tree.Kind.ARGUMENTS);
    }
    if (isMethodInvocationReceiverOfGetClass(typeCastTree)) {
      // java.lang.Object#getClass() is a very specific method declared to return Class<?> but in fact
      // returns Class<? extends (receiver type)> at compile time. To prevent false-positives, we ignore this case.
      return false;
    }
    return warnings.stream().anyMatch(warning -> matchesWarning(warning, typeCastTree));
  }

  private static boolean isMethodInvocationReceiverOfGetClass(TypeCastTree typeCastTree) {
    Tree parent = skipParenthesesUpwards( typeCastTree.parent());
    return parent instanceof MemberSelectExpressionTree memberSelect &&
      "getClass".equals(memberSelect.identifier().name()) &&
      memberSelect.parent() instanceof MethodInvocationTree methodInvocation &&
      methodInvocation.arguments().isEmpty();
  }

  private static boolean matchesWarning(JWarning warning, TypeCastTree tree) {
    Tree warningTree = warning.syntaxTree();
    if (warningTree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      // When a cast expression is nested inside one or more parenthesized expression, Eclipse raises the warning on
      // the outermost parenthesized expression rather than the cast expression, so we need to take that into account
      return tree.equals(skipParentheses(warningTree));
    }
    return tree.equals(warningTree);
  }

}
