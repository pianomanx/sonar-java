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

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.Strings;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.MapBuilder;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1488")
public class ImmediatelyReturnedVariableCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<Kind, String> MESSAGE_KEYS = MapBuilder.<Kind, String>newMap()
    .put(Kind.THROW_STATEMENT, "throw")
    .put(Kind.RETURN_STATEMENT, "return")
    .build();

  private JavaFileScannerContext context;
  private String lastTypeForMessage;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBlock(BlockTree tree) {
    super.visitBlock(tree);
    List<StatementTree> statements = tree.body();
    int size = statements.size();
    if (size < 2) {
      return;
    }
    StatementTree butLastStatement = statements.get(size - 2);
    if (butLastStatement.is(Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) butLastStatement;
      if(!variableTree.modifiers().annotations().isEmpty()) {
        return;
      }
      StatementTree lastStatement = statements.get(size - 1);
      String lastStatementIdentifier = getReturnOrThrowIdentifier(lastStatement);
      if (lastStatementIdentifier != null) {
        String identifier = variableTree.simpleName().name();
        if (Strings.CS.equals(lastStatementIdentifier, identifier)) {
          ExpressionTree initializer = variableTree.initializer();
          if (initializer == null) {
            // Can only happen for non-compilable code, still, we should not report anything.
            return;
          }
          QuickFixHelper.newIssue(context)
            .forRule(this)
            .onTree(initializer)
            .withMessage("Immediately %s this expression instead of assigning it to the temporary variable \"%s\".", lastTypeForMessage, identifier)
            .withQuickFix(() -> quickFix(butLastStatement, lastStatement, variableTree, lastTypeForMessage))
            .report();
        }
      }
    }
  }

  private static JavaQuickFix quickFix(StatementTree butLastStatement, StatementTree lastStatement, VariableTree variableTree, String lastTypeForMessage) {
    // Equal token can not be null at this point, we checked before the presence of the initializer
    return JavaQuickFix.newQuickFix("Inline expression")
      .addTextEdit(
        JavaTextEdit.replaceTextSpan(textSpanBetween(variableTree.modifiers(), true, variableTree.initializer(), false), lastTypeForMessage + " "),
        JavaTextEdit.removeTextSpan(textSpanBetween(butLastStatement, false, lastStatement, true)))
      .build();
  }

  @CheckForNull
  private String getReturnOrThrowIdentifier(StatementTree lastStatementOfBlock) {
    lastTypeForMessage = null;
    ExpressionTree expr = null;
    if (lastStatementOfBlock.is(Kind.THROW_STATEMENT)) {
      lastTypeForMessage = MESSAGE_KEYS.get(Kind.THROW_STATEMENT);
      expr = ((ThrowStatementTree) lastStatementOfBlock).expression();
    } else if (lastStatementOfBlock.is(Kind.RETURN_STATEMENT)) {
      lastTypeForMessage = MESSAGE_KEYS.get(Kind.RETURN_STATEMENT);
      expr = ((ReturnStatementTree) lastStatementOfBlock).expression();
    }
    if (expr != null && expr.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) expr).name();
    }
    return null;
  }
}
