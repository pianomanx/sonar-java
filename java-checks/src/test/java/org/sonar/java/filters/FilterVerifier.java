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

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Fail;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.Version;
import org.sonar.check.Rule;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.LineUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.test.classpath.TestClasspathUtils.DEFAULT_MODULE;

public class FilterVerifier {

  private FilterVerifier() {
  }

  public static FilterVerifier newInstance() {
    return new FilterVerifier();
  }

  private boolean withoutSemantic = false;

  public void verify(String filename, JavaIssueFilter filter, JavaCheck... extraJavaChecks) {
    IssueCollector issueCollector = new IssueCollector();
    List<JavaCheck> visitors = new ArrayList<>();
    visitors.add(filter);
    visitors.add(issueCollector);

    // instantiate the rules filtered by the filter
    visitors.addAll(instantiateRules(filter.filteredRules()));

    visitors.addAll(Arrays.asList(extraJavaChecks));

    List<File> projectClasspath = new ArrayList<>(DEFAULT_MODULE.getClassPath());
    projectClasspath.add(new File("target/test-classes"));

    InputFile inputFile = TestUtils.inputFile(filename);
    VisitorsBridgeForTests.Builder visitorsBridgeBuilder = new VisitorsBridgeForTests.Builder(visitors)
      .withSonarComponents(sonarComponents(inputFile));
    if (!this.withoutSemantic) {
      visitorsBridgeBuilder.enableSemanticWithProjectClasspath(projectClasspath);
    }
    VisitorsBridgeForTests visitorsBridge = visitorsBridgeBuilder.build();
    JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge);
    JavaFileScannerContextForTests testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();

    Map<Integer, Set<String>> issuesByLines = new HashMap<>();
    Set<AnalyzerMessage> issues = testJavaFileScannerContext.getIssues();
    for (AnalyzerMessage analyzerMessage : issues) {
      Integer issueLine = analyzerMessage.getLine();
      String ruleKey = AnnotationUtils.getAnnotation(analyzerMessage.getCheck().getClass(), Rule.class).key();
      FilterableIssue issue = mock(FilterableIssue.class);
      when(issue.ruleKey()).thenReturn(RuleKey.of("java", ruleKey));
      when(issue.componentKey()).thenReturn(inputFile.key());
      when(issue.line()).thenReturn(issueLine);

      if (issueCollector.rejectedIssuesLines.contains(issueLine)) {
        assertThat(filter.accept(issue))
          .overridingErrorMessage("Line #" + issueLine + " has been marked with 'NoIssue' but issue of rule '" + ruleKey + "' has been accepted!")
          .isFalse();
      } else if (issueCollector.acceptedIssuesLines.contains(issueLine)) {
        // force check on accepted issues
        assertThat(filter.accept(issue))
          .overridingErrorMessage("Line #" + issueLine + " has been marked with 'WithIssue' but no issue have been raised!")
          .isTrue();
      } else {
        issuesByLines.computeIfAbsent(issueLine, k -> new HashSet<>()).add(ruleKey);
      }
    }

    if (!issuesByLines.isEmpty()) {
      List<Integer> lines = new ArrayList<>(issuesByLines.keySet());
      Collections.sort(lines);
      StringBuilder builder = new StringBuilder();
      for (Integer line : lines) {
        builder.append("\n#" + line + ": " + issuesByLines.get(line).toString());
      }

      Fail.fail("The following lines have not been marked with 'WithIssue' or 'NoIssue' and raised issues:" + builder.toString());
    }
  }

  /**
   * Tells the verifier that no bytecode will be provided.
   * See {@link CheckVerifier#withoutSemantic()} for more.
   */
  public FilterVerifier withoutSemantic() {
    this.withoutSemantic = true;
    return this;
  }

  private static Set<JavaCheck> instantiateRules(Set<Class<? extends JavaCheck>> filteredRules) {
    Set<JavaCheck> rules = new HashSet<>();
    for (Class<? extends JavaCheck> rule : filteredRules) {
      try {
        rules.add(rule.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        Fail.fail("Unable to instantiate rule " + rule.getCanonicalName());
      }
    }
    return rules;
  }

  private static class IssueCollector extends SubscriptionVisitor {

    private final Set<Integer> rejectedIssuesLines = new HashSet<>();
    private final Set<Integer> acceptedIssuesLines = new HashSet<>();

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      String comment = syntaxTrivia.comment().trim();
      String[] lines = comment.split("\\r\\n|\\r|\\n");
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].endsWith("NoIssue")) {
          rejectedIssuesLines.add(LineUtils.startLine(syntaxTrivia) + i);
        } else if (lines[i].endsWith("WithIssue")) {
          acceptedIssuesLines.add(LineUtils.startLine(syntaxTrivia) + i);
        }
      }
    }
  }

  private static SonarComponents sonarComponents(InputFile inputFile) {
    SensorContextTester context = SensorContextTester.create(new File("")).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    context.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, true));
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        throw new AssertionError(String.format("Should not fail analysis (%s)", re.getMessage()));
      }
    };
    sonarComponents.setSensorContext(context);
    context.fileSystem().add(inputFile);
    return sonarComponents;
  }

}
