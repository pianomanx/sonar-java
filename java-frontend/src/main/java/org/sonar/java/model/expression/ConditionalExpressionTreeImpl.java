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
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class ConditionalExpressionTreeImpl extends AssessableExpressionTree implements ConditionalExpressionTree {

  private final ExpressionTree condition;
  private final InternalSyntaxToken queryToken;
  private final ExpressionTree trueExpression;
  private final InternalSyntaxToken colonToken;
  private final ExpressionTree falseExpression;

  public ConditionalExpressionTreeImpl(ExpressionTree condition, InternalSyntaxToken queryToken, ExpressionTree trueExpression, InternalSyntaxToken colonToken,
    ExpressionTree falseExpression) {
    this.condition = condition;
    this.queryToken = queryToken;
    this.trueExpression = trueExpression;
    this.colonToken = colonToken;
    this.falseExpression = falseExpression;
  }

  @Override
  public Kind kind() {
    return Kind.CONDITIONAL_EXPRESSION;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken questionToken() {
    return queryToken;
  }

  @Override
  public ExpressionTree trueExpression() {
    return trueExpression;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public ExpressionTree falseExpression() {
    return falseExpression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitConditionalExpression(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      condition,
      queryToken,
      trueExpression,
      colonToken,
      falseExpression
    );
  }
}
