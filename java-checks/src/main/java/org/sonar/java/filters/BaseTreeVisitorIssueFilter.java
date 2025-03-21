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
package org.sonar.java.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class BaseTreeVisitorIssueFilter extends BaseTreeVisitor implements JavaIssueFilter {

  private String componentKey;
  private final Map<String, Set<Integer>> excludedLinesByRule;
  private final Map<Class<? extends JavaCheck>, String> rulesKeysByRulesClass;

  protected BaseTreeVisitorIssueFilter() {
    excludedLinesByRule = new HashMap<>();
    rulesKeysByRulesClass = rulesKeysByRulesClass(filteredRules());
  }

  private static Map<Class<? extends JavaCheck>, String> rulesKeysByRulesClass(Set<Class<? extends JavaCheck>> rules) {
    Map<Class<? extends JavaCheck>, String> results = new HashMap<>();
    for (Class<? extends JavaCheck> ruleClass : rules) {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, Rule.class);
      if (ruleAnnotation != null) {
        results.put(ruleClass, ruleAnnotation.key());
      }
    }
    return results;
  }

  public String getComponentKey() {
    return componentKey;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    componentKey = context.getInputFile().key();
    excludedLinesByRule.clear();
    scan(context.getTree());
  }

  @Override
  public boolean accept(FilterableIssue issue) {
    return !(issue.componentKey().equals(componentKey) && excludedLinesByRule.getOrDefault(issue.ruleKey().rule(), new HashSet<>()).contains(issue.line()));
  }

  public Map<String, Set<Integer>> excludedLinesByRule() {
    return excludedLinesByRule;
  }

  final void excludeLines(Set<Integer> lines, String ruleKey) {
    computeFilteredLinesForRule(lines, ruleKey, true);
  }

  final void excludeLines(@Nullable Tree tree, Class<? extends JavaCheck> rule) {
    excludeLinesIfTrue(true, tree, rule);
  }

  @SafeVarargs
  final void excludeLines(@Nullable Tree tree, Class<? extends JavaCheck>... rules) {
    excludeLinesIfTrue(true, tree, rules);
  }

  @SafeVarargs
  final void excludeLinesIfTrue(boolean condition, @Nullable Tree tree, Class<? extends JavaCheck>... rules) {
    Arrays.stream(rules).forEach(rule -> excludeLinesIfTrue(condition, tree, rule));
  }

  final void excludeLinesIfTrue(boolean condition, @Nullable Tree tree, String ruleKey) {
    computeFilteredLinesForRule(tree, ruleKey, condition);
  }

  final void excludeLinesIfTrue(boolean condition, @Nullable Tree tree, Class<? extends JavaCheck> rule) {
    computeFilteredLinesForRule(tree, rulesKeysByRulesClass.get(rule), condition);
  }

  private void computeFilteredLinesForRule(@Nullable Tree tree, String ruleKey, boolean excludeLine) {
    if (tree == null) {
      return;
    }
    SyntaxToken firstSyntaxToken = tree.firstToken();
    SyntaxToken lastSyntaxToken = tree.lastToken();
    if (firstSyntaxToken != null && lastSyntaxToken != null) {
      Set<Integer> filteredLines = IntStream.rangeClosed(LineUtils.startLine(firstSyntaxToken), LineUtils.startLine(lastSyntaxToken))
        .boxed()
        .collect(Collectors.toSet());
      computeFilteredLinesForRule(filteredLines, ruleKey, excludeLine);
    }
  }

  private void computeFilteredLinesForRule(Set<Integer> lines, String ruleKey, boolean excludeLine) {
    if (excludeLine) {
      excludedLinesByRule.computeIfAbsent(ruleKey, k -> new HashSet<>()).addAll(lines);
    } else {
      excludedLinesByRule.getOrDefault(ruleKey, Collections.emptySet()).removeAll(lines);
    }
  }
}
