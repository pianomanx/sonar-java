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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3052")
public class DefaultInitializedFieldCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ((ClassTree) tree).members()
      .stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .forEach(this::checkVariable);
  }

  private void checkVariable(VariableTree member) {
    if (ModifiersUtils.hasModifier(member.modifiers(), Modifier.FINAL)) {
      return;
    }
    ExpressionTree initializer = member.initializer();
    if (initializer != null) {
      ExpressionTree cleanedInitializer = ExpressionUtils.skipParentheses(initializer);
      getIfDefault(cleanedInitializer, member.type().symbolType().isPrimitive())
        .ifPresent(value -> reportIssue(cleanedInitializer, String.format("Remove this initialization to \"%s\", the compiler will do that for you.", value)));
    }
  }

  private static Optional<String> getIfDefault(ExpressionTree expression, boolean isPrimitive) {
    if (!isPrimitive && !expression.is(Tree.Kind.TYPE_CAST)) {
      return expression.is(Tree.Kind.NULL_LITERAL) ? literalValue(expression) : Optional.empty();
    }
    switch (expression.kind()) {
      case CHAR_LITERAL:
        return literalValue(expression)
          .filter(charValue -> "'\\u0000'".equals(charValue) || "'\\0'".equals(charValue));
      case BOOLEAN_LITERAL:
        return literalValue(expression)
          .filter(booleanValue -> LiteralUtils.isFalse(expression));
      case INT_LITERAL,
        LONG_LITERAL:
        return Optional.ofNullable(LiteralUtils.longLiteralValue(expression))
          .filter(numericalValue -> numericalValue == 0)
          .flatMap(numericalValue -> literalValue(expression));
      case FLOAT_LITERAL,
        DOUBLE_LITERAL:
        return Optional.ofNullable(LiteralUtils.doubleLiteralValue(expression))
          .filter(numericalValue -> Double.doubleToLongBits(numericalValue) == 0)
          .flatMap(numericalValue -> literalValue(expression));
      case TYPE_CAST:
        return getIfDefault(((TypeCastTree) expression).expression(), isPrimitive);
      default:
        return Optional.empty();
    }
  }

  private static Optional<String> literalValue(ExpressionTree expression) {
    return Optional.of(((LiteralTree) expression).value());
  }

}
