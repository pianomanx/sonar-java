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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.deprecatedAnnotation;
import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.hasJavadocDeprecatedTag;

public abstract class AbstractMissingDeprecatedChecker extends IssuableSubscriptionVisitor {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();

  private final Deque<Boolean> classOrInterfaceIsDeprecated = new ArrayDeque<>();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(PublicApiChecker.apiKinds());
  }

  @Override
  public void visitNode(Tree tree) {
    boolean isLocalVar = tree.is(Tree.Kind.VARIABLE) && ((VariableTree) tree).symbol().isLocalVariable();
    AnnotationTree deprecatedAnnotation = deprecatedAnnotation(tree);
    boolean hasDeprecatedAnnotation = deprecatedAnnotation != null;
    boolean hasJavadocDeprecatedTag = hasJavadocDeprecatedTag(tree);
    if (currentClassNotDeprecated() && !isLocalVar) {
      handleDeprecatedElement(tree, deprecatedAnnotation, hasJavadocDeprecatedTag);
    }
    if (tree.is(CLASS_KINDS)) {
      classOrInterfaceIsDeprecated.push(hasDeprecatedAnnotation || hasJavadocDeprecatedTag);
    }
  }

  abstract void handleDeprecatedElement(Tree tree, @CheckForNull AnnotationTree deprecatedAnnotation, boolean hasJavadocDeprecatedTag);

  private boolean currentClassNotDeprecated() {
    return classOrInterfaceIsDeprecated.isEmpty() || !classOrInterfaceIsDeprecated.peek();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(CLASS_KINDS)) {
      classOrInterfaceIsDeprecated.pop();
    }
  }
}
