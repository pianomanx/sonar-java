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
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Rule(key = "S2387")
public class ChildClassShadowFieldCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> IGNORED_FIELDS = Collections.singleton("serialVersionUID");

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeTree superClass = ((ClassTree) tree).superClass();
    if (superClass != null) {
      Symbol.TypeSymbol superclassSymbol = superClass.symbolType().symbol();
      ((ClassTree) tree).members().stream()
        .filter(m -> m.is(Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .map(VariableTree::simpleName)
        .forEach(fieldSimpleName -> {
          if (!IGNORED_FIELDS.contains(fieldSimpleName.name())) {
            checkForIssue(superclassSymbol, fieldSimpleName);
          }
        });
    }
  }

  private void checkForIssue(Symbol.TypeSymbol classSymbol, IdentifierTree fieldSimpleName) {
    for (Symbol.TypeSymbol symbol = classSymbol; symbol != null; symbol = getSuperclass(symbol)) {
      if (checkMembers(fieldSimpleName, symbol)) {
        return;
      }
    }
  }

  private boolean checkMembers(IdentifierTree fieldSimpleName, Symbol.TypeSymbol symbol) {
    for (Symbol member : symbol.memberSymbols()) {
      if (member.isVariableSymbol()
        && !member.isPrivate()
        && !member.isStatic()
        && member.name().equals(fieldSimpleName.name())) {
        reportIssue(fieldSimpleName, String.format("\"%s\" is the name of a field in \"%s\".", fieldSimpleName.name(), symbol.name()));
        return true;
      }
    }
    return false;
  }

  @CheckForNull
  private static Symbol.TypeSymbol getSuperclass(Symbol.TypeSymbol symbol) {
    Type superType = symbol.superClass();
    if (superType != null) {
      return superType.symbol();
    }
    return null;
  }

}
