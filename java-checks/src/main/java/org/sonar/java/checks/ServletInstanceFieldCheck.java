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
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2226")
public class ServletInstanceFieldCheck extends IssuableSubscriptionVisitor {

  private final List<VariableTree> issuableVariables = new ArrayList<>();
  private final List<VariableTree> excludedVariables = new ArrayList<>();

  private static final MethodMatchers INIT_METHOD_WITH_PARAM_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("javax.servlet.Servlet")
      .names("init").addParametersMatcher("javax.servlet.ServletConfig").build(),
    MethodMatchers.create()
      .ofSubTypes("jakarta.servlet.Servlet")
      .names("init").addParametersMatcher("jakarta.servlet.ServletConfig").build());

  private static final MethodMatchers INIT_METHOD_NO_PARAMS_MATCHER = MethodMatchers.create()
    .ofSubTypes("javax.servlet.GenericServlet", "jakarta.servlet.GenericServlet")
    .names("init").addWithoutParametersMatcher().build();

  private static final List<String> ANNOTATIONS_EXCLUDING_FIELDS = Arrays.asList(
    "javax.inject.Inject",
    "jakarta.inject.Inject",
    "javax.ejb.EJB",
    "jakarta.ejb.EJB",
    "javax.annotation.Resource",
    "jakarta.annotation.Resource");

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.VARIABLE, Kind.METHOD);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    reportIssuesOnVariable();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.METHOD) && isServletInit((MethodTree) tree)) {
      tree.accept(new AssignmentVisitor());
    } else if (tree.is(Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      if (isOwnedByAServlet(variable) && !isExcluded(variable)) {
        issuableVariables.add(variable);
      }
    }
  }

  private static boolean isExcluded(VariableTree variable) {
    SymbolMetadata varMetadata = variable.symbol().metadata();
    return isStaticOrFinal(variable) || ANNOTATIONS_EXCLUDING_FIELDS.stream().anyMatch(varMetadata::isAnnotatedWith);
  }

  private static boolean isServletInit(MethodTree tree) {
    return INIT_METHOD_WITH_PARAM_MATCHER.matches(tree) || INIT_METHOD_NO_PARAMS_MATCHER.matches(tree);
  }

  private void reportIssuesOnVariable() {
    issuableVariables.removeAll(excludedVariables);
    for (VariableTree variable : issuableVariables) {
      reportIssue(variable.simpleName(), "Remove this misleading mutable servlet instance field or make it \"static\" and/or \"final\"");
    }
    issuableVariables.clear();
    excludedVariables.clear();
  }

  private class AssignmentVisitor extends BaseTreeVisitor {
    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      var variable = tree.variable();
      if (variable instanceof  IdentifierTree identifier) {
        // handles e.g. "second = this.first * 2;" assignments -> no "this" prefix to member "second"
        addVariableToExcluded(identifier.symbol().declaration());
      } else if (variable instanceof MemberSelectExpressionTree memberSelectTree
        && memberSelectTree.expression() instanceof IdentifierTree identifier
        && "this".equals(identifier.identifierToken().text())) {
        // handles e.g. "this.first = 42;" assignments -> member "first" is prefixed with "this."
        addVariableToExcluded(memberSelectTree.identifier().symbol().declaration());
      }
    }
    private void addVariableToExcluded(@Nullable Tree declaration) {
      // if declaration set, and a variable, then add to excluded
      if (declaration != null && declaration.is(Kind.VARIABLE)) {
        excludedVariables.add((VariableTree) declaration);
      }
    }
  }

  private static boolean isOwnedByAServlet(VariableTree variable) {
    Symbol owner = variable.symbol().owner();

    if (!owner.isTypeSymbol() || !variable.parent().is(Tree.Kind.CLASS)) {
      return false;
    }

    var ownerType = owner.type();
    return ownerType.isSubtypeOf("javax.servlet.http.HttpServlet")
      || ownerType.isSubtypeOf("jakarta.servlet.http.HttpServlet")
      || ownerType.isSubtypeOf("org.apache.struts.action.Action");
  }

  private static boolean isStaticOrFinal(VariableTree variable) {
    return ModifiersUtils.hasAnyOf(variable.modifiers(), Modifier.STATIC, Modifier.FINAL);
  }

}
