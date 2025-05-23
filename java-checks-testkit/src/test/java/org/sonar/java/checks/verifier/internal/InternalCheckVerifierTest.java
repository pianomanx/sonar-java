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
package org.sonar.java.checks.verifier.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.caching.DummyCache;
import org.sonar.java.caching.FileHashingUtils;
import org.sonar.java.caching.JavaReadCacheImpl;
import org.sonar.java.caching.JavaWriteCacheImpl;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FAILING_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FILE_ISSUE_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FILE_ISSUE_CHECK_IN_ANDROID;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.FILE_LINE_ISSUE_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.IssueWithQuickFix;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.MultipleIssuePerLineCheck;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.NO_EFFECT_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.NoEffectEndOfAnalysisCheck;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.PROJECT_ISSUE_CHECK;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_NONCOMPLIANT;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_PARSE_ERROR;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_WITH_NO_EXPECTED;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_WITH_QUICK_FIX;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE;
import static org.sonar.java.checks.verifier.internal.CheckVerifierTestUtils.TEST_FILE_WITH_TWO_QUICK_FIX;

class InternalCheckVerifierTest {

  @Nested
  class TestingCheckVerifierInitialConfiguration {

    @Test
    void failing_check_should_make_verifier_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(FAILING_CHECK)
        .onFile(TEST_FILE)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AnalysisException.class)
        .hasMessage("Failing check");
    }

    @Test
    void invalid_file_should_make_verifier_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(NO_EFFECT_CHECK)
        .onFile(TEST_FILE_PARSE_ERROR)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Should not fail analysis (Parse error at line 1 column 9: Syntax error, insert \"}\" to complete ClassBody)");
    }

    @Test
    void setting_check_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(11)
        .onFile(TEST_FILE)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Set check(s) before calling any verification method!");
    }

    @Test
    void setting_checks_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(11)
        .withChecks()
        .onFile(TEST_FILE)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Provide at least one check!");
    }

    @Test
    void setting_no_issues_without_semantic_should_fail_if_issue_is_raised() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(11)
        .onFile(TEST_FILE)
        .withCheck(FILE_LINE_ISSUE_CHECK)
        .withoutSemantic()
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("No issues expected but got 1 issue(s):")
        .hasMessageContaining("--> 'issueOnLine' in")
        .hasMessageEndingWith("Compliant.java:1");
    }

    @Test
    void setting_valid_file_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile("dummy.test")
        .withoutSemantic()
        .withCheck(NO_EFFECT_CHECK)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Unable to read file")
        .hasMessageEndingWith("dummy.test'");
    }

    @Test
    void setting_files_is_required() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withoutSemantic()
        .withCheck(NO_EFFECT_CHECK)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Set file(s) before calling any verification method!");
    }

    @Test
    void setting_multiple_times_java_version_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withJavaVersion(6)
        .withJavaVersion(7));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Do not set java version multiple times!");
    }

    @Test
    void preview_features_can_only_be_enabled_for_the_latest_java_version() {
      int desiredJavaVersion = JavaVersionImpl.MAX_SUPPORTED - 1;
      final CheckVerifier verifier = InternalCheckVerifier.newInstance();
      assertThatThrownBy(() -> verifier.withJavaVersion(desiredJavaVersion, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          String.format("Preview features can only be enabled when the version == latest supported Java version (%d != %d)", desiredJavaVersion, JavaVersionImpl.MAX_SUPPORTED)
        );
    }
    

/*    @Test
    void preview_features_disabled_by_default() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(NO_EFFECT_CHECK)
        .withJavaVersion(21)
        .onFile(TEST_FILE_WITH_PREVIEW_FEATURES)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("Should not fail analysis (Parse error at line");

      Throwable e2 = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(NO_EFFECT_CHECK)
        .withJavaVersion(21, false)
        .onFile(TEST_FILE_WITH_PREVIEW_FEATURES)
        .verifyNoIssues());

      assertThat(e2)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("Should not fail analysis (Parse error at line");

      Throwable noExceptionThrown = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .withCheck(NO_EFFECT_CHECK)
        .withJavaVersion(21, true)
        .onFile(TEST_FILE_WITH_PREVIEW_FEATURES)
        .verifyNoIssues());

      assertThat(noExceptionThrown).isNull();
    }*/


    @Test
    void setting_multiple_times_one_files_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .onFile(TEST_FILE));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Do not set file(s) multiple times!");
    }

    @Test
    void setting_multiple_times_multiple_files_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFiles(TEST_FILE)
        .onFile(TEST_FILE));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Do not set file(s) multiple times!");
    }

    @Test
    void setting_custom_verifier_which_fails() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(FILE_ISSUE_CHECK)
        .withCustomIssueVerifier(issues -> {
          throw new IllegalStateException("Rejected");
        })
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Rejected");
    }

    @Test
    void setting_custom_verifier_which_accepts() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(FILE_ISSUE_CHECK)
        .withCustomIssueVerifier(issues -> {
          /* do nothing */
        })
        .verifyIssueOnFile("issueOnFile");
    }
  }

  @Nested
  class TestingRuleMetadata {

    @Test
    void rule_without_annotation_should_fail() {
      class WithoutAnnotationCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "issueOnLine");
        }
      }

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new WithoutAnnotationCheck())
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Rules should be annotated with '@Rule(key = \"...\")' annotation (org.sonar.check.Rule).");
    }

    @Test
    void rule_with_constant_remediation_function_should_not_provide_cost() {
      @Rule(key = "ConstantJSON")
      class ConstantCostCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new ConstantCostCheck())
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Rule with constant remediation function shall not provide cost");
    }

    @Test
    void absent_rule_matadata_does_not_make_verifier_fail() {
      @Rule(key = "DoesntExists")
      class DoesntExistsMetadata implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new DoesntExistsMetadata())
        .verifyIssues();
    }

    @Test
    void borken_rule_metadata_does_not_make_verifier_fail() {
      @Rule(key = "BrokenJSON")
      class BorkenMetadata implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new BorkenMetadata())
        .verifyIssues();
    }

    @Test
    void rule_metadata_unknown_remediation_function() {
      @Rule(key = "ExponentialRemediationFunc")
      class ExponentialRemediationFunctionCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new ExponentialRemediationFunctionCheck())
        .verifyIssues();
    }

    @Test
    void rule_metadata_undefined_remediation_function() {
      @Rule(key = "UndefinedRemediationFunc")
      class UndefinedRemediationFunctionCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message", 42);
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new UndefinedRemediationFunctionCheck())
        .verifyIssues();
    }

    @Test
    void should_fail_when_no_cost() {
      @Rule(key = "LinearJSON")
      class LinearRemediationFunctionCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message");
        }
      }

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new LinearRemediationFunctionCheck())
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A cost should be provided for a rule with linear remediation function");
    }

    @Test
    void test_rspec_key_with_no_metadata_should_not_fail() {
      @Rule(key = "Dummy_fake_JSON")
      class DoesntExistsMetadataCheck implements JavaFileScanner {
        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "message");
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(new DoesntExistsMetadataCheck())
        .verifyIssues();
    }
  }

  @Nested
  class TestingProjectIssues {

    @Test
    void verify_should_work() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(PROJECT_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject");
    }

    @Test
    void not_raising_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(NO_EFFECT_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the project, but none has been raised");
    }

    @Test
    void raising_too_many_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(PROJECT_ISSUE_CHECK, PROJECT_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the project, but 2 issues have been raised");
    }

    @Test
    void raissing_a_different_message_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(PROJECT_ISSUE_CHECK)
        .verifyIssueOnProject("expected"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage(String.format("%s%n%s%n%s%n%s",
          "Expected the issue message to be:",
          "\t\"expected\"",
          "but was:",
          "\t\"issueOnProject\""));
    }

    @Test
    void raising_an_issue_line_instead_of_project_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_LINE_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected an issue directly on project but was raised on line 1");
    }

    @Test
    void raising_an_issue_on_file_instead_of_project_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_ISSUE_CHECK)
        .verifyIssueOnProject("issueOnProject"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected the issue to be raised at project level, not at file level");
    }
  }

  @Test
  void no_issue_if_not_in_android_context() {
    InternalCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(FILE_ISSUE_CHECK_IN_ANDROID)
      .withinAndroidContext(false)
      .verifyNoIssues();
  }

  @Test
  void issue_if_in_android_context() {
    Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
      .onFile(TEST_FILE)
      .withChecks(FILE_ISSUE_CHECK_IN_ANDROID)
      .withinAndroidContext(true)
      .verifyNoIssues());

    assertThat(e)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("No issues expected but got 1 issue(s):");
  }

  @Test
  void context_root_working_directory_not_supported() {
    String rootWorkDir = "rootDir";

    InternalCheckVerifier checkVerifier = InternalCheckVerifier.newInstance()
      .onFile(TEST_FILE);

    Throwable e = catchThrowable(() -> {
      checkVerifier
        .withProjectLevelWorkDir(rootWorkDir);
    });

    assertThat(e)
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("Method not implemented, feel free to implement.");
  }

  @Nested
  class TestingNoIssues {

    @Test
    void verify_should_work() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(NO_EFFECT_CHECK)
        .verifyNoIssues();
    }

    @Test
    void raising_issues_while_expecting_none_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(
          FILE_ISSUE_CHECK,
          PROJECT_ISSUE_CHECK,
          FILE_LINE_ISSUE_CHECK,
          FILE_LINE_ISSUE_CHECK)
        .verifyNoIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("No issues expected but got 4 issue(s):")
        .hasMessageContaining("--> 'issueOnLine'")
        .hasMessageContaining("--> 'issueOnProject'")
        .hasMessageContaining("--> 'issueOnFile'");
    }
  }

  @Nested
  class TestingFileIssues {

    @Test
    void verify_should_work() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(FILE_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile");
    }

    @Test
    void not_raising_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withCheck(NO_EFFECT_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the file, but none has been raised");
    }

    @Test
    void raising_too_many_issues_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_ISSUE_CHECK, FILE_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("A single issue is expected on the file, but 2 issues have been raised");
    }

    @Test
    void raising_a_different_message_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_ISSUE_CHECK)
        .verifyIssueOnFile("expected"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage(String.format("%s%n%s%n%s%n%s",
          "Expected the issue message to be:",
          "\t\"expected\"",
          "but was:",
          "\t\"issueOnFile\""));
    }

    @Test
    void raising_an_issue_line_instead_of_file_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(FILE_LINE_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected an issue directly on file but was raised on line 1");
    }

    @Test
    void raising_an_issue_on_project_instead_of_file_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(PROJECT_ISSUE_CHECK)
        .verifyIssueOnFile("issueOnFile"));

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Expected the issue to be raised at file level, not at project level");
    }
  }

  @Nested
  class TestingMulitpleFileIssues {

    @Test
    void raising_no_issue_while_expecting_some_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE)
        .withChecks(NO_EFFECT_CHECK)
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("No issue raised. At least one issue expected");
    }

    @Test
    void should_verify() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withChecks(FILE_LINE_ISSUE_CHECK)
        .verifyIssues();
    }

    @Test
    void multi_variable_declaration_should_create_only_one_expected_issue() {

      @Rule(key = "check")
      class Check implements JavaFileScanner {

        @Override
        public void scanFile(JavaFileScannerContext context) {
          context.addIssue(1, this, "issue");
        }
      }

      InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultiVariableDeclaration.java")
        .withChecks(new Check())
        .verifyIssues();
    }

    @Test
    void order_of_expected_issue_on_same_line_is_relevant() {
      InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultipleIssuesSameLine.java")
        .withChecks(new MultipleIssuePerLineCheck())
        .verifyIssues();
    }

    @Test
    void wrong_order_of_expected_issue_on_same_line_should_fail() {
      MultipleIssuePerLineCheck check = new MultipleIssuePerLineCheck();
      check.setFlipOrder(true);

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultipleIssuesSameLine.java")
        .withChecks(check)
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("line 7 attribute mismatch for 'MESSAGE'. Expected: 'msg 1', but was: 'msg 2'");
    }

    @Test
    void wrong_message_of_expected_issue_on_same_line_should_fail() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile("src/test/files/testing/MultipleIssuesSameLine.java")
        .withChecks(new MultipleIssuePerLineCheck("msg 1", "wrong message"))
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("line 4 attribute mismatch for 'MESSAGE'. Expected: 'msg 2', but was: 'wrong message'");
    }
  }

  @Nested
  class TestingQuickFix {

    @Test
    void test_one_quick_fix() {
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_one_quick_fix_wrong_description() {
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("wrong")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong description for issue on line 1.")
        .hasMessageContaining(  "Expected: {{Description}}")
        .hasMessageContaining(    "but was:     {{wrong}}");
    }

    @Test
    void test_one_quick_fix_wrong_number_of_edits() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit, edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong number of edits for issue on line 1.")
        .hasMessageContaining("Expected: {{1}}")
        .hasMessageContaining(    "but was:     {{2}}");
    }

    @Test
    void test_one_quick_fix_wrong_text_replacement() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Wrong");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong text replacement of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{Replacement}}")
        .hasMessageContaining( "but was:     {{Wrong}}");
    }

    @Test
    void test_one_quick_fix_wrong_edit_position() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(4, 2, 6, 5), "Replacement");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Wrong change location of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{(1:7)-(1:8)}}")
        .hasMessageContaining("but was:     {{(4:3)-(6:6)}}");
    }

    @Test
    void test_one_quick_fix_missing_from_actual() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(new IssueWithQuickFix(Collections::emptyList))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("[Quick Fix] Missing quick fix for issue on line 1");
    }

    @Test
    void test_no_quick_fix_expected() {
      JavaTextEdit edit = JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement");
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(edit)
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_NO_EXPECTED)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("[Quick Fix] Issue on line 1 contains quick fixes while none where expected");
    }

    @Test
    void test_no_quick_fix_expected_no_actual() {
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_NO_EXPECTED)
        .withCheck(new IssueWithQuickFix(Collections::emptyList))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_one_quick_fix_not_tested_is_accepted() {
      // One file with one Noncompliant comment but no QF specified in the comment is fine, we don't have to always test quick fixes
      Supplier<JavaQuickFix> quickFix = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_NONCOMPLIANT)
        .withCheck(IssueWithQuickFix.of(quickFix))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_two_quick_fix_for_one_issue() {
      Supplier<List<JavaQuickFix>> quickFixes = () -> Arrays.asList(
        JavaQuickFix.newQuickFix("Description")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
          .build(),
        JavaQuickFix.newQuickFix("Description2")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 1, 1, 2), "Replacement2"))
          .build()
      );

      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_TWO_QUICK_FIX)
        .withCheck(new IssueWithQuickFix(quickFixes))
        .withQuickFixes()
        .verifyIssues();
    }

    @Test
    void test_two_quick_fix_for_one_issue_1_actual_missing() {
      Supplier<JavaQuickFix> quickFix1 = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(
          new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_TWO_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFix1))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Number of quickfixes expected is not equal to the number of expected on line 1: expected: 2 , actual: 1");
    }

    @Test
    void test_one_quick_fix_two_reported() {
      Supplier<List<JavaQuickFix>> quickFixes = () -> Arrays.asList(
        JavaQuickFix.newQuickFix("Description")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
          .build(),
        JavaQuickFix.newQuickFix("Description2")
          .addTextEdit(JavaTextEdit.replaceTextSpan(
            new AnalyzerMessage.TextSpan(1, 1, 1, 2), "Replacement2"))
          .build());

      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(new IssueWithQuickFix(quickFixes))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("[Quick Fix] Number of quickfixes expected is not equal to the number of expected on line 1: expected: 1 , actual: 2");
    }

    @Test
    void test_warn_when_quick_fix_in_file_not_expected() {
      Throwable e = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(NO_EFFECT_CHECK)
        .verifyIssues());

      assertThat(e)
        .isInstanceOf(AssertionError.class)
        .hasMessage("Add \".withQuickFixes()\" to the verifier. Quick fixes are expected but the verifier is not configured to test them.");
    }

    @Test
    void test_quick_fix_supports_new_lines() {
      Supplier<JavaQuickFix> quickFixMultipleLine = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(new AnalyzerMessage.TextSpan(1, 6, 1, 7), "line1\n  line2;"))
        .build();
      Supplier<JavaQuickFix> quickFixSimple = () -> JavaQuickFix.newQuickFix("Description")
        .addTextEdit(JavaTextEdit.replaceTextSpan(new AnalyzerMessage.TextSpan(1, 6, 1, 7), "Replacement"))
        .build();

      Throwable e1 = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX)
        .withCheck(IssueWithQuickFix.of(quickFixMultipleLine))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e1)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("[Quick Fix] Wrong text replacement of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{Replacement}}")
        .hasMessageContaining("but was:     {{line1\n  line2;}}");

      Throwable e2 = catchThrowable(() -> InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE)
        .withCheck(IssueWithQuickFix.of(quickFixSimple))
        .withQuickFixes()
        .verifyIssues());

      assertThat(e2)
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("[Quick Fix] Wrong text replacement of edit 1 for issue on line 1.")
        .hasMessageContaining("Expected: {{line1\n  line2;}}")
        .hasMessageContaining("but was:     {{Replacement}}");

      // passes
      InternalCheckVerifier.newInstance()
        .onFile(TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE)
        .withCheck(IssueWithQuickFix.of(quickFixMultipleLine))
        .withQuickFixes()
        .verifyIssues();
    }

  }

  @Test
  void addFiles_registers_file_to_be_analyzed() {
    InternalCheckVerifier.newInstance()
      .addFiles(InputFile.Status.ADDED, TEST_FILE)
      .withCheck(NO_EFFECT_CHECK)
      .verifyNoIssues();

    InternalCheckVerifier.newInstance()
      .addFiles(InputFile.Status.ADDED, TEST_FILE)
      .addFiles(InputFile.Status.ADDED, TEST_FILE_NONCOMPLIANT)
      .withCheck(NO_EFFECT_CHECK)
      .verifyNoIssues();
  }

  @Test
  void addFiles_throws_an_IllegalArgumentException_if_file_added_before() {
    InternalCheckVerifier checkVerifier = InternalCheckVerifier.newInstance();
    checkVerifier.onFiles(TEST_FILE);
    assertThatThrownBy(() -> {
      checkVerifier.addFiles(InputFile.Status.ADDED, TEST_FILE);
    }).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("File %s was already added.", Path.of(TEST_FILE)));
  }

  @Test
  void withCache_effectively_sets_the_caches_for_scanWithoutParsing() throws IOException, NoSuchAlgorithmException {
    InputFile inputFile = InternalInputFile.inputFile("", new File(TEST_FILE), InputFile.Status.SAME);
    ReadCache readCache = new InternalReadCache().put("java:contentHash:MD5::" + TEST_FILE, FileHashingUtils.inputFileContentHash(inputFile));
    WriteCache writeCache = new InternalWriteCache().bind(readCache);
    CacheContext cacheContext = new InternalCacheContext(
      true,
      new JavaReadCacheImpl(readCache),
      new JavaWriteCacheImpl(writeCache)
    );

    var check = spy(new NoEffectEndOfAnalysisCheck());

    InternalCheckVerifier.newInstance()
      .withCache(readCache, writeCache)
      .onFile(TEST_FILE)
      .withCheck(check)
      .verifyNoIssues();

    verify(check, times(1)).scanWithoutParsing(argThat(context -> CheckVerifierTestUtils.equivalent(cacheContext, context.getCacheContext())));
    verify(check, times(1)).endOfAnalysis(argThat(context -> CheckVerifierTestUtils.equivalent(cacheContext, context.getCacheContext())));
  }

  @Test
  void withCache_can_handle_a_mix_of_caches_combination() {
    InternalCheckVerifier dummyReadDummyWrite = InternalCheckVerifier.newInstance();
    dummyReadDummyWrite.withCache(null, null);
    assertThat(dummyReadDummyWrite.cacheContext.getReadCache()).isInstanceOf(DummyCache.class);
    assertThat(dummyReadDummyWrite.cacheContext.getWriteCache()).isInstanceOf(DummyCache.class);

    InternalCheckVerifier internalReadDummyWrite = InternalCheckVerifier.newInstance();
    internalReadDummyWrite.withCache(new InternalReadCache(), null);
    assertThat(internalReadDummyWrite.cacheContext.getReadCache()).isInstanceOf(JavaReadCache.class);
    assertThat(internalReadDummyWrite.cacheContext.getWriteCache()).isInstanceOf(DummyCache.class);

    InternalCheckVerifier internalReadInternalWrite = InternalCheckVerifier.newInstance();
    internalReadInternalWrite.withCache(new InternalReadCache(), new InternalWriteCache());
    assertThat(internalReadInternalWrite.cacheContext.getReadCache()).isInstanceOf(JavaReadCache.class);
    assertThat(internalReadInternalWrite.cacheContext.getWriteCache()).isInstanceOf(JavaWriteCache.class);

    InternalCheckVerifier dummyReadInternalWrite = InternalCheckVerifier.newInstance();
    dummyReadInternalWrite.withCache(null, new InternalWriteCache());
    assertThat(dummyReadInternalWrite.cacheContext.getReadCache()).isInstanceOf(JavaReadCache.class);
    assertThat(dummyReadInternalWrite.cacheContext.getWriteCache()).isInstanceOf(JavaWriteCache.class);
  }

  @Test
  void compilationUnitModifier_not_supported() {

    InternalCheckVerifier checkVerifier = InternalCheckVerifier.newInstance()
      .onFile(TEST_FILE);

    Throwable e = catchThrowable(() -> {
      checkVerifier
        .withCompilationUnitModifier(tree -> {
        });
    });

    assertThat(e)
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("Method not implemented, feel free to implement.");
  }

}
