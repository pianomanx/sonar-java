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
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2160")
public class EqualsNotOverriddenInSubclassCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create()
    .ofAnyType().names("equals").addParametersMatcher("java.lang.Object").build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (shouldImplementEquals(classTree)) {
      reportIssue(classTree.simpleName(), "Override the \"equals\" method in this class.");
    }
  }

  private static boolean shouldImplementEquals(ClassTree classTree) {
    return hasAtLeastOneField(classTree) && !hasEqualsMethod(classTree.symbol()) && parentClassImplementsEquals(classTree);
  }

  private static boolean hasAtLeastOneField(ClassTree classTree) {
    return classTree.members().stream().anyMatch(EqualsNotOverriddenInSubclassCheck::isField);
  }

  private static boolean isField(Tree tree) {
    return tree.is(Tree.Kind.VARIABLE) && !ModifiersUtils.hasModifier(((VariableTree) tree).modifiers(), Modifier.STATIC);
  }

  private static boolean parentClassImplementsEquals(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    if (superClass != null) {
      Type superClassType = superClass.symbolType();
      while (superClassType.symbol().isTypeSymbol() && !superClassType.is("java.lang.Object")) {
        Symbol.TypeSymbol superClassSymbol = superClassType.symbol();
        Optional<Symbol> equalsMethod = equalsMethod(superClassSymbol);
        if (equalsMethod.isPresent()) {
          Symbol equalsMethodSymbol = equalsMethod.get();
          return !equalsMethodSymbol.isFinal() && !equalsMethodSymbol.isAbstract();
        }
        superClassType = superClassSymbol.superClass();
      }
    }
    return false;
  }

  private static boolean hasEqualsMethod(Symbol.TypeSymbol type) {
    return equalsMethod(type).isPresent();
  }

  private static Optional<Symbol> equalsMethod(Symbol.TypeSymbol type) {
    return type.lookupSymbols("equals").stream().filter(EQUALS_MATCHER::matches).findFirst();
  }
}
