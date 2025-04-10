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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Rule(key = "S2176")
public class InterfaceOrSuperclassShadowingCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    checkSuperType(classTree, classSymbol.superClass());
    for (Type interfaceType : classSymbol.interfaces()) {
      checkSuperType(classTree, interfaceType);
    }
  }

  private void checkSuperType(ClassTree tree, @Nullable Type superType) {
    if (superType != null && hasSameName(tree, superType) && !isInnerClass(tree)) {
      reportIssue(tree.simpleName(), "Rename this " + tree.kind().name().toLowerCase(Locale.ROOT) + ".");
    }
  }

  private static boolean hasSameName(ClassTree tree, Type superType) {
    return superType.symbol().name().equals(tree.symbol().name());
  }

  private static boolean isInnerClass(ClassTree tree) {
    Symbol owner = tree.symbol().owner();
    return owner != null && !owner.isUnknown() && owner.isTypeSymbol();
  }
}
