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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6809")
public class AsyncMethodsCalledViaThisCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, String> DISALLOWED_METHOD_ANNOTATIONS = Map.of(
    SpringUtils.ASYNC_ANNOTATION, "async",
    SpringUtils.TRANSACTIONAL_ANNOTATION, "transactional",
    "org.springframework.cache.annotation.Cacheable", "cacheable");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var mit = (MethodInvocationTree) tree;

    if (
      // If the call is not a member select, it must be an identifier, so it's a call to a local method, implicitly via 'this'
      !mit.methodSelect().is(Tree.Kind.MEMBER_SELECT) ||
        // On the other hand, if calls do have a qualifier, an explicit 'this' means we also want to raise an issue.
        ExpressionUtils.isThis(((MemberSelectExpressionTree) mit.methodSelect()).expression())
    ) {
      DISALLOWED_METHOD_ANNOTATIONS.entrySet().stream()
        .filter(entry -> mit.methodSymbol().metadata().isAnnotatedWith(entry.getKey()))
        .findFirst()
        .map(Map.Entry::getValue)
        .ifPresent(friendlyName -> reportIssue(mit, "Call " + friendlyName + " methods via an injected dependency instead of directly via 'this'."));
    }
  }
}
