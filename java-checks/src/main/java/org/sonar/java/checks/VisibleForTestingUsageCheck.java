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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5803")
public class VisibleForTestingUsageCheck extends IssuableSubscriptionVisitor {
  
  private final Set<Symbol> reportedSymbols = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    IdentifierTree identifier = (IdentifierTree) tree;

    Symbol symbol = identifier.symbol();
    if (symbol.isUnknown() || symbol.metadata().annotations().isEmpty() || reportedSymbols.contains(symbol)) {
      return;
    }
    if (isMisusedVisibleForTesting(symbol)) {
      List<JavaFileScannerContext.Location> locations = symbol.usages().stream()
        .filter(identifierTree -> !tree.equals(identifierTree))
        .map(identifierTree -> new JavaFileScannerContext.Location("usage of @VisibleForTesting in production", identifierTree))
        .toList();

      reportIssue(identifier, String.format("Remove this usage of \"%s\", it is annotated with @VisibleForTesting and should not be accessed from production code.",
        identifier.name()), locations, null);

      reportedSymbols.add(symbol);
    }
  }

  private static boolean isMisusedVisibleForTesting(Symbol symbol) {
    Symbol owner = Objects.requireNonNull(symbol.owner(), "Owner is never null if unknown symbols are filtered out");
    return isFieldMethodOrClass(symbol, owner) && !inTheSameFile(symbol)
      && symbol.metadata().annotations().stream().anyMatch(VisibleForTestingUsageCheck::isVisibleForTestingAnnotation);
  }

  private static boolean isVisibleForTestingAnnotation(AnnotationInstance annotationInstance) {
    return "VisibleForTesting".equals(annotationInstance.symbol().name())
      && !isOtherwiseProtected(annotationInstance);
  }

  private static boolean isOtherwiseProtected(AnnotationInstance annotationInstance) {
    List<SymbolMetadata.AnnotationValue> values = annotationInstance.values();
    for (SymbolMetadata.AnnotationValue value : values) {
      // Note: constant to support is androidx.annotation.VisibleForTesting.PROTECTED=4
      if ("otherwise".equals(value.name()) &&
        value.value() instanceof Integer &&
        Integer.valueOf(4).equals(value.value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean inTheSameFile(Symbol symbol) {
    return symbol.declaration() != null;
  }

  private static boolean isFieldMethodOrClass(Symbol symbol, Symbol owner) {
    return symbol.isTypeSymbol() || owner.isTypeSymbol();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    reportedSymbols.clear();
    super.leaveFile(context);
  }
}
