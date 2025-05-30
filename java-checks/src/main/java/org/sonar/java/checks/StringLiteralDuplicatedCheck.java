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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1192")
public class StringLiteralDuplicatedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_THRESHOLD = 3;

  // String literals include quotes, so this means length 5 as defined in RSPEC
  private static final int MINIMAL_LITERAL_LENGTH = 7;

  @RuleProperty(
    key = "threshold",
    description = "Number of times a literal must be duplicated to trigger an issue",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  private final Map<String, List<LiteralTree>> occurrences = new HashMap<>();
  private final Map<String, VariableTree> constants = new HashMap<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    occurrences.clear();
    constants.clear();
    scan(context.getTree());
    occurrences.forEach((key, literalTrees) -> {
      int literalOccurrence = literalTrees.size();
      // Do not consider `throw new Exception("repeated message")` for reporting duplicates,
      // but still report it if a constant is available.
      int triggeringOccurrences = (int) literalTrees.stream().filter(tree -> !isThrowableArgument(tree)).count();
      if (constants.containsKey(key)) {
        VariableTree constant = constants.get(key);
        List<LiteralTree> duplications = literalTrees.stream().filter(literal -> literal.parent() != constant).toList();
        context.reportIssue(this, duplications.iterator().next(),
          "Use already-defined constant '" + constant.simpleName() + "' instead of duplicating its value here.",
          secondaryLocations(duplications.subList(1, duplications.size())), literalOccurrence);
      } else if (triggeringOccurrences >= threshold) {
        LiteralTree literalTree = literalTrees.iterator().next();
        String message = literalTree.is(Tree.Kind.TEXT_BLOCK) ? ("Define a constant instead of duplicating this text block " + literalOccurrence + " times.")
          : ("Define a constant instead of duplicating this literal \"" + key + "\" " + literalOccurrence + " times.");
        context.reportIssue(
          this,
          literalTree,
          message,
          secondaryLocations(literalTrees), literalOccurrence);
      }
    });
  }

  private static List<JavaFileScannerContext.Location> secondaryLocations(Collection<LiteralTree> literalTrees) {
    return literalTrees.stream().map(element -> new JavaFileScannerContext.Location("Duplication", element)).toList();
  }

  /**
   * Verify that <code>literalTree</code> is an argument in
   * <code>throw new SomeException(arg1, arg2, ...)</code>,
   * or a part of an argument (to account for concatenation), for example,
   * <code>throw new SomeException(arg1, "literalTree" + stuff, ...)</code>,
   * For simplicity and to avoid surprises, we do not consider more complex cases.
   */
  private static boolean isThrowableArgument(LiteralTree literalTree) {
    Optional<Tree> tree = Optional.ofNullable(literalTree.parent());
    // If the literal is a part of string concatenation expression, move up
    // until the argument list.
    while(tree.filter(t -> t.is(Tree.Kind.PLUS)).isPresent()) {
      tree = tree.map(Tree::parent);
    }
    return tree
      .filter(t -> t.is(Tree.Kind.ARGUMENTS))
      .map(Tree::parent)
      .filter(t -> t.is(Tree.Kind.NEW_CLASS))
      .map(Tree::parent)
      .filter(t -> t.is(Tree.Kind.THROW_STATEMENT))
      .isPresent();
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK)) {
      String literal = tree.value();
      if (literal.length() >= MINIMAL_LITERAL_LENGTH && !isStringLiteralFragment(tree)) {
        String stringValue = LiteralUtils.getAsStringValue(tree).replace("\\n", "\n");
        occurrences.computeIfAbsent(stringValue, key -> new ArrayList<>()).add(tree);
      }
    }
  }

  private static boolean isStringLiteralFragment(ExpressionTree tree) {
    return isStringLiteral(tree) && (isStringLiteral(getNextOperand(tree)) || isStringLiteral(getPreviousOperand(tree)));
  }

  private static boolean isStringLiteral(@Nullable Tree tree) {
    return tree != null && tree.is(Tree.Kind.STRING_LITERAL);
  }

  @Nullable
  private static ExpressionTree getNextOperand(ExpressionTree tree) {
    var binary = asPlusExpression(tree.parent());
    if (binary == null) {
      return null;
    }
    if (tree == binary.leftOperand()) {
      return binary.rightOperand();
    } else {
      binary = asPlusExpression(binary.parent());
      return binary != null ? binary.rightOperand() : null;
    }
  }

  @Nullable
  private static ExpressionTree getPreviousOperand(ExpressionTree tree) {
    var binary = asPlusExpression(tree.parent());
    if (binary == null) {
      return null;
    }
    if (tree == binary.leftOperand()) {
      return null;
    } else {
      var left = binary.leftOperand();
      binary = asPlusExpression(left);
      return binary != null ? binary.rightOperand() : binary;
    }
  }

  @Nullable
  private static BinaryExpressionTree asPlusExpression(Tree tree) {
    return tree.is(Tree.Kind.PLUS) ? (BinaryExpressionTree) tree : null;
  }

  @Override
  public void visitVariable(VariableTree tree) {
    ExpressionTree initializer = tree.initializer();
    if (initializer != null && initializer.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK)
      && ModifiersUtils.hasAll(tree.modifiers(), Modifier.STATIC, Modifier.FINAL)) {
      String stringValue = LiteralUtils.getAsStringValue((LiteralTree) initializer).replace("\\n", "\n");
      constants.putIfAbsent(stringValue, tree);
      return;
    }
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (ModifiersUtils.hasModifier(tree.modifiers(), Modifier.DEFAULT)) {
      //Ignore default methods to avoid catch-22 with S1214
      return;
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    //Ignore literals within annotation
  }
}
