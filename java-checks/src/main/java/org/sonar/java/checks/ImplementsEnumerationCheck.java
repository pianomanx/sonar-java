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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1150")
public class ImplementsEnumerationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    for (TypeTree superInterface : classTree.superInterfaces()) {
      IdentifierTree identifierTree = null;
      if (superInterface.is(Tree.Kind.IDENTIFIER)) {
        identifierTree = (IdentifierTree) superInterface;
      } else if (superInterface.is(Tree.Kind.PARAMETERIZED_TYPE) && ((ParameterizedTypeTree) superInterface).type().is(Tree.Kind.IDENTIFIER)) {
        identifierTree = (IdentifierTree) ((ParameterizedTypeTree) superInterface).type();
      }
      if (isEnumeration(identifierTree)) {
        reportIssue(superInterface, "Implement Iterator rather than Enumeration.");
      }
    }
  }

  private static boolean isEnumeration(@Nullable IdentifierTree tree) {
    return tree != null && "Enumeration".equals(tree.name());
  }
}
