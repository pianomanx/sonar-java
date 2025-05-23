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
package org.sonar.java.checks.verifier;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;
import org.sonar.java.checks.verifier.internal.JavaCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

/**
 * This interface defines how to use checks (rules) verifiers. Its goal is to provide all the required information
 * to the analyzer to verify the checks' expected behavior.
 * <p>
 * The starting point to define a verifier is {@link #newVerifier()}. Then, a configuration can be specified.
 * <p>
 * It is required to provide to the verifier at least the following:
 * <ul>
 *   <li>A rule, by calling {@link #withCheck(JavaFileScanner)}, or {@link #withChecks(JavaFileScanner...)}</li>
 *   <li>A test file, by calling {@link #onFile(String)}, {@link #onFiles(String...)}, or {@link #onFiles(Collection)}</li>
 * </ul>
 * Methods starting with "verify..." (e.g {@link #verifyIssues()} ) are the methods which effectively validate the rule.
 * Any of them must be called at the end of the verifier's configuration to trigger the verification.
 * <strong>Nothing will happen if one of these method is not called.</strong>
 * It uses
 * <a href="https://github.com/SonarSource/sonar-analyzer-commons/blob/master/test-commons/src/main/java/org/sonarsource/analyzer/commons/checks/verifier/MultiFileVerifier.java">
 *   MultiFileVerifier
 * </a>
 * from <a href="https://github.com/SonarSource/sonar-analyzer-commons/tree/master">
 * sonar-analyzer-commons - test-commons
 * </a>
 * library
 * to verify issues on file.
 */
public interface CheckVerifier {

  /**
   * Entry point of check verification. Will return a new instance of verifier to be configured.
   *
   * @return the newly instantiated verifier
   */
  static CheckVerifier newVerifier() {
    return JavaCheckVerifier.newInstance();
  }

  /**
   * <p>
   * In the test file(s), lines on which it is expected to have issues being raised have to be flagged with a comment
   * prefixed by the "Noncompliant" string, followed by some optional details/specificity of the expected issue.
   * <p>
   * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
   * But it is usually better to use the line number relative to the current; this is possible by prefixing the number with either '+' or '-'.
   * <p>
   * For example, the following comment says that an issue is going to be raised on the following line (@+1) with the given message:
   * <pre>
   *   // Noncompliant@+1 {{do not import "java.util.List"}}
   *   import java.util.List;
   * </pre>
   * Full syntax:
   * <pre>
   *   // Noncompliant@+1 [[startColumn=1;endLine=+1;endColumn=2;effortToFix=4;secondary=3,4]] {{issue message}}
   * </pre>
   * Some attributes can also be written using a simplified form, for instance:
   * <pre>
   *   // Noncompliant [[sc=14;ec=42]] {{issue message}}
   * </pre>
   * Finally, note that attributes between {@literal [[...]]} are all optional:
   * <ul>
   *  <li>startColumn (sc): column where the highlight starts</li>
   *  <li>endLine (el): relative endLine where the highlight ends (i.e. +1), same line if omitted</li>
   *  <li>endColumn (ec): column where the highlight ends</li>
   *  <li>effortToFix: the cost to fix as integer</li>
   *  <li>secondary: a comma-separated list of integers identifying the lines of secondary locations if any</li>
   * </ul>
   *
   * @deprecated in favor of {@link #newVerifier()}, which uses the analyzer-commons-test-commons library to verify issues on checks.
   */
  @Deprecated(since = "7.35", forRemoval = true)
  static CheckVerifier newInternalVerifier() {
    return InternalCheckVerifier.newInstance();
  }

  /**
   * Defines the check to be verified against at least one test file.
   *
   * @param check the rule to be verified
   *
   * @return the verifier configured to use the check provided as an argument
   */
  CheckVerifier withCheck(JavaFileScanner check);

  /**
   * Defines the check(s) to be verified against at least one test file.
   *
   * @param checks the rules to be verified
   *
   * @return the verifier configured to use the checks provided as argument
   */
  CheckVerifier withChecks(JavaFileScanner... checks);

  /**
   * Defines the classpath to be used for the verification. Usually used when the code of the
   * test files require the knowledge of a particular set of libraries or java compiled classes.
   *
   * @param classpath a collection of files which defines the classes/jars/zips which contains
   * the bytecode to be used as a classpath when executing the rule
   *
   * @return the verifier configured to use the files provided as argument as classpath
   */
  CheckVerifier withClassPath(Collection<File> classpath);

  /**
   * Defines the java version syntax to be used for the verification. Usually used when the code of the
   * test files explicitly target a given version (e.g. java 7) where a particular syntax/API has been introduced.
   * Preview features for the specified java version will be disabled by default; use {@link CheckVerifier#withJavaVersion(int, boolean)}
   * to enable or disable preview features associated with the specified java version.
   *
   * @param javaVersionAsInt defines the java language syntax version to be considered during verification, provided as an integer.
   * For instance, for Java 1.7, use '7'. For Java 12, simply '12'.
   *
   * @return the verifier configured to consider the provided test file(s) as following the syntax of the given java version
   */
  CheckVerifier withJavaVersion(int javaVersionAsInt);

  /**
   * Defines the java version syntax to be used for the verification. Usually used when the code of the
   * test files explicitly target a given version (e.g. java 7) where a particular syntax/API has been introduced.
   *
   * @param javaVersionAsInt defines the java language syntax version to be considered during verification, provided as an integer.
   * For instance, for Java 1.7, use '7'. For Java 12, simply '12'.
   *
   * @param enablePreviewFeatures defines if preview features from the specified java version should be enabled or not
   *
   * @return the verifier configured to consider the provided test file(s) as following the syntax of the given java version
   *
   * @throws IllegalArgumentException If the enablePreviewFeatures parameter is set to true but javaVersionAsInt is not the latest supported version
   */
  CheckVerifier withJavaVersion(int javaVersionAsInt, boolean enablePreviewFeatures);

  /**
   * Defines whether the current file is an analyzer in an Android context.
   *
   * @return the verifier currently configured
   */
  CheckVerifier withinAndroidContext(boolean inAndroidContext);

  /**
   * Defines the filename to be verified with the given rule(s). This file should contain all the "Noncompliant"
   * comments defining the expected issues.
   *
   * @param filename the file to be analyzed
   *
   * @return the verifier configured to consider the provided test file as the source for the rule(s)
   */
  CheckVerifier onFile(String filename);

  /**
   * Defines the filenames to be verified with the given rule(s). These files should all contain "Noncompliant"
   * comments defining the expected issues.
   *
   * @param filenames the files to be analyzed
   *
   * @return the verifier configured to consider the provided test file(s) as source for the rule(s)
   */
  CheckVerifier onFiles(String... filenames);

  /**
   * Defines a collection of filenames to be verified with the given rule(s). These files should all
   * contain "Noncompliant" comments defining the expected issues.
   *
   * @param filenames a collection of files to be analyzed
   *
   * @return the verifier configured to consider the provided test file(s) as source for the rule(s)
   */
  CheckVerifier onFiles(Collection<String> filenames);

  /**
   * Adds a collection of files with an expected status to be verified by the given rule(s).
   * An exception is thrown if a file by the same filename is already listed to be analyzed.
   * @param status The status of the files to be analyzed
   * @param filenames a collection of files to be analyzed
   * @return the verifier configured
   * @throws IllegalArgumentException if a file by the same filename has already been added
   */
  CheckVerifier addFiles(InputFile.Status status, String... filenames);

  /**
   * Adds a collection of files with an expected status.
   * An exception is thrown if a file with the same filename is already listed to be analyzed.
   * @param status The status of the files to be analyzed
   * @param filenames a collection of files to be analyzed
   * @return the verifier configured
   * @throws IllegalArgumentException if a file by the same filename has already been added
   */
  CheckVerifier addFiles(InputFile.Status status, Collection<String> filenames);

  /**
   * Tells the verifier that no bytecode will be provided. This method is usually used in combination with
   * {@link #verifyNoIssues()}, to assert the fact that if no bytecode is provided, the rule will not raise
   * any issues.
   *
   * @return the verifier configured to consider that no bytecode will be provided for analysis
   */
  CheckVerifier withoutSemantic();

  /**
   * Tells the verifier to feed the check with cached information in its preScan phase.
   * @param readCache A source of information from previous analyses
   * @param writeCache A place to dump information at the end of the analysis
   * @return the verifier configured with the caches to use.
   */
  CheckVerifier withCache(@Nullable ReadCache readCache, @Nullable WriteCache writeCache);


  /**
   * Tells the verifier to feed the check with cached information in its preScan phase.
   * @param rootDirectory The path of the project root working directory
   * @return the verifier configured with the project root working directory.
   */
  default CheckVerifier withProjectLevelWorkDir(String rootDirectory) {
    throw new UnsupportedOperationException("Method not implemented, feel free to implement.");
  }

  /**
   * Allows to modify the compilation unit tree after parsing.
   * This is useful for adding or modifying nodes in the tree for testing purposes.
   * There is at most one modifier per {@link CheckVerifier}.
   *
   * @param compilationUnitModifier the modifier to apply to the compilation unit tree
   * @return the verifier configured with the compilation unit modifier.
   */
  default CheckVerifier withCompilationUnitModifier(Consumer<CompilationUnitTree> compilationUnitModifier) {
    throw new UnsupportedOperationException("Method not implemented, feel free to implement.");
  }

  /**
   * Verifies that all the expected issues are correctly raised by the rule(s),
   * at their expected positions and attributes.
   */
  void verifyIssues();

  /**
   * Verifies that an issue (only one) is raised directly on the file and not
   * within the content of the file.
   *
   * @param expectedIssueMessage the message to be expected with the issue.
   */
  void verifyIssueOnFile(String expectedIssueMessage);

  /**
   * Verifies that an issue (only one) is raised directly on the project, which would include this file,
   * and not within the content of the file.
   *
   * @param expectedIssueMessage
   */
  void verifyIssueOnProject(String expectedIssueMessage);

  /**
   * Verifies that no issues are raised by the rule(s) on the given file(s).
   */
  void verifyNoIssues();
}
