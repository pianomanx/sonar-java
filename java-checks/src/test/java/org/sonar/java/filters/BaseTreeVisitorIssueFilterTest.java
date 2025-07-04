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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.check.Rule;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class BaseTreeVisitorIssueFilterTest {

  private static final InputFile INPUT_FILE = TestUtils.inputFile(mainCodeSourcesPath("filters/BaseTreeVisitorIssueFilter.java"));
  private static final String REPOSITORY_KEY = "octopus";
  private static final String RULE_KEY = "S42";
  private BaseTreeVisitorIssueFilter filter;
  private FilterableIssue issue;

  @BeforeEach
  void setup() {
    issue = mock(FilterableIssue.class);
    when(issue.componentKey()).thenReturn(INPUT_FILE.key());
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, RULE_KEY));

    filter = new FakeJavaIssueFilterOnClassAndVariable();

    scanFile(filter);
  }

  @Test
  void issues_by_targeted_rule_should_be_filtered() {
    // issue on file
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class ignored only if class is called "AllowedClassName"
    assertThatIssueWillBeAccepted(3).isFalse();
    assertThatIssueWillBeAccepted(14).isTrue();

    // issue on variable filtered
    assertThatIssueWillBeAccepted(4).isFalse();
    assertThatIssueWillBeAccepted(5).isFalse();
  }

  @Test
  void issues_from_non_targeted_rules_are_accepted() {
    // other rule
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule"));

    // issue on file accepted
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class accepted
    assertThatIssueWillBeAccepted(3).isTrue();
    assertThatIssueWillBeAccepted(14).isTrue();

    // issue on variable accepted
    assertThatIssueWillBeAccepted(4).isTrue();
    assertThatIssueWillBeAccepted(5).isTrue();
  }

  @Test
  void excluded_lines_are_correct() {
    Map<String, Set<Integer>> excludedLinesByRule = filter.excludedLinesByRule();
    assertThat(excludedLinesByRule)
      .isNotNull()
      .isNotEmpty()
      .containsOnlyKeys(RULE_KEY);
    assertThat(excludedLinesByRule.get(RULE_KEY)).containsOnly(3, 4, 5, 6, 7, 8, 9, 10, 11, 15);
  }

  @Test
  void excluded_lines_by_rule_never_returns_null() {
    // no effect filter
    filter = new BaseTreeVisitorIssueFilter() {
      @Override
      public Set<Class<? extends JavaCheck>> filteredRules() {
        return Collections.emptySet();
      }
    };
    // no component is set
    scanFile(filter);

    Map<String, Set<Integer>> excludedLinesByRule = filter.excludedLinesByRule();
    assertThat(excludedLinesByRule)
      .isNotNull()
      .isEmpty();
  }

  @Test
  void issues_from_other_component_are_accepted() {
    // targeted rule
    when(issue.componentKey()).thenReturn("UnknownComponent");

    // issue on file accepted
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class accepted
    assertThatIssueWillBeAccepted(3).isTrue();
    assertThatIssueWillBeAccepted(14).isTrue();

    // issue on variable accepted
    assertThatIssueWillBeAccepted(4).isTrue();
    assertThatIssueWillBeAccepted(5).isTrue();
  }

  private AbstractBooleanAssert<?> assertThatIssueWillBeAccepted(@Nullable Integer line) {
    when(issue.line()).thenReturn(line);
    return assertThat(filter.accept(issue));
  }

  private static class FakeJavaIssueFilterOnClassAndVariable extends BaseTreeVisitorIssueFilter {
    @Override
    public Set<Class<? extends JavaCheck>> filteredRules() {
      return Set.of(FakeRule.class, FakeRuleWithoutKey.class);
    }

    @Override
    public void visitVariable(VariableTree tree) {
      excludeLines(tree, FakeRule.class);
      super.visitVariable(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      IdentifierTree simpleName = tree.simpleName();
      if (simpleName == null) {
        // force check on null tree
        excludeLines(simpleName, FakeRuleWithoutKey.class);
      } else if ("AllowedClassName".equals(simpleName.name())) {
        excludeLinesIfTrue(false, simpleName, FakeRule.class);
      } else {
        excludeLines(simpleName, FakeRule.class);
      }
      super.visitClass(tree);
    }
  }

  @Rule(key = RULE_KEY)
  private static class FakeRule implements JavaCheck {
  }

  private static class FakeRuleWithoutKey implements JavaCheck {
  }

  private static void scanFile(JavaIssueFilter filter) {
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests.Builder(filter).build();
    JavaAstScanner.scanSingleFileForTests(INPUT_FILE, visitorsBridge);
  }
}
