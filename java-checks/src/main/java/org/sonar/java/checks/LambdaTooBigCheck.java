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
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5612")
public class LambdaTooBigCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAX = 10;

  @RuleProperty(key = "Max",
    description = "Maximum allowed lines in a lambda",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    int lines = getNumberOfLines(lambdaExpressionTree);
    if (lines > max) {
      SyntaxToken firstToken = lambdaExpressionTree.firstToken();
      SyntaxToken lastSyntaxToken = lambdaExpressionTree.lastToken();
      JavaFileScannerContext.Location lastTokenLocation = new JavaFileScannerContext.Location(lines + " lines", lastSyntaxToken);
      context.reportIssue(this, firstToken, lambdaExpressionTree.arrowToken(),
        "Reduce this lambda expression number of lines from " + lines + " to at most " + max + ".", Collections.singletonList(lastTokenLocation), null);
    }
    super.visitLambdaExpression(lambdaExpressionTree);
  }

  private static int getNumberOfLines(Tree tree) {
    return new LinesOfCodeVisitor().linesOfCode(tree);
  }
}
