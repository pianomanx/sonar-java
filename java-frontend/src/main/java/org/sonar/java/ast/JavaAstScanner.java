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
package org.sonar.java.ast;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.InputFileUtils;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.java.telemetry.TelemetryKey.JavaAnalysisKeys;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static java.lang.System.currentTimeMillis;

public class JavaAstScanner {
  private static final Logger LOG = LoggerFactory.getLogger(JavaAstScanner.class);

  private static final String LOG_ERROR_STACKOVERFLOW = "A stack overflow error occurred while analyzing file: '%s'";
  private static final String LOG_ERROR_UNABLE_TO_PARSE_FILE = "Unable to parse source file : '%s'";
  private static final String LOG_WARN_MISCONFIGURED_JAVA_VERSION = "Analyzing '%s' file with misconfigured Java version."
    + " Please check that property '%s' is correctly configured (currently set to: %d) or exclude 'module-info.java' files from analysis."
    + " Such files only exist in Java9+ projects.";

  private final SonarComponents sonarComponents;
  private final Telemetry telemetry;
  private final JavaAnalysisKeys javaAnalysisKeys;
  private VisitorsBridge visitor;
  private boolean reportedMisconfiguredVersion = false;

  public JavaAstScanner(@Nullable SonarComponents sonarComponents, Telemetry telemetry, JavaAnalysisKeys javaAnalysisKeys) {
    this.sonarComponents = sonarComponents;
    this.telemetry = telemetry;
    this.javaAnalysisKeys = javaAnalysisKeys;
  }

  public List<File> getClasspath() {
    return visitor.getClasspath();
  }

  /**
   * Attempt to scan files without parsing, using the raw input file and cached information.
   *
   * @param inputFiles The list of files to analyze
   * @return A map with 2 lists of inputFiles. Under the {@code true} key, files that have successfully been scanned without parsing and,
   * under the {@code false} key, files that need to be parsed for further analysis.
   */
  public Map<Boolean, List<InputFile>> scanWithoutParsing(Iterable<? extends InputFile> inputFiles) {
    return StreamSupport.stream(inputFiles.spliterator(), false)
      // Split files between successfully scanned without parsing and failed to scan without parsing
      .collect(Collectors.partitioningBy(visitor::scanWithoutParsing));
  }

  public void scan(Iterable<? extends InputFile> inputFiles) {
    scan(inputFiles, compilationUnitTree -> {});
  }

  /**
   * Scan the given files and modify
   *
   * @param inputFiles The list of files to analyze
   * @param modifyCompilationUnit allow you to modify the ast before running the analysis on it, for example to remove semantic information
   */
  @VisibleForTesting
  public void scanForTesting(Iterable<? extends InputFile> inputFiles, Consumer<CompilationUnitTree> modifyCompilationUnit) {
    scan(inputFiles, modifyCompilationUnit);
  }


  private void scan(Iterable<? extends InputFile> inputFiles, Consumer<CompilationUnitTree> modifyCompilationUnit) {
    List<? extends InputFile> filesNames = filterModuleInfo(inputFiles).toList();
    AnalysisProgress analysisProgress = new AnalysisProgress(filesNames.size());
    try {
      boolean shouldIgnoreUnnamedModuleForSplitPacakge = sonarComponents != null &&
        sonarComponents.shouldIgnoreUnnamedModuleForSplitPackage();
      JParserConfig.Mode.FILE_BY_FILE
        .create(visitor.getJavaVersion(), visitor.getClasspath(), shouldIgnoreUnnamedModuleForSplitPacakge)
        .parse(filesNames,
          this::analysisCancelled,
          analysisProgress,
          (i, r) -> simpleScan(i, r,
            // Due to a bug in ECJ, JAR files remain locked after the analysis on Windows, we unlock them manually. See SONARJAVA-3609.
            JavaAstScanner::cleanUpAst,
            modifyCompilationUnit));
    } finally {
      endOfAnalysis();
    }
  }

  public <T extends InputFile> Stream<T> filterModuleInfo(Iterable<T> inputFiles) {
    JavaVersion javaVersion = visitor.getJavaVersion();
    return StreamSupport.stream(inputFiles.spliterator(), false)
      .filter(file -> {
        if (("module-info.java".equals(file.filename())) && !javaVersion.isNotSet() && javaVersion.asInt() <= 8) {
          // When the java version is not set, we use the maximum version supported, able to parse module info.
          logMisconfiguredVersion("module-info.java", javaVersion);
          return false;
        }
        return true;
      });
  }

  public void endOfAnalysis() {
    visitor.endOfAnalysis();
    logUndefinedTypes();
  }

  private void logUndefinedTypes() {
    if (sonarComponents != null) {
      sonarComponents.logUndefinedTypes();
    }
  }

  private boolean analysisCancelled() {
    return sonarComponents != null && sonarComponents.analysisCancelled();
  }

  public void simpleScan(InputFile inputFile, JParserConfig.Result result, Consumer<JavaTree.CompilationUnitTreeImpl> cleanUp) {
    simpleScan(inputFile, result, cleanUp, compilationUnitTree -> {});
  }

  // modifyCompilationUnit should be used for testing.
  public void simpleScan(InputFile inputFile, JParserConfig.Result result, Consumer<JavaTree.CompilationUnitTreeImpl> cleanUp,
    Consumer<CompilationUnitTree> modifyCompilationUnit) {
    long startTime = currentTimeMillis();
    visitor.setCurrentFile(inputFile);
    var telemetryAnalysisKeys = javaAnalysisKeys.exceptions();
    try {
      JavaTree.CompilationUnitTreeImpl ast = result.get();
      modifyCompilationUnit.accept(ast);
      visitor.visitFile(ast, sonarComponents != null && sonarComponents.fileCanBeSkipped(inputFile));
      String path = inputFile.toString();
      Set<JProblem> undefinedTypes = ast.sema.undefinedTypes();
      collectUndefinedTypes(path, undefinedTypes);
      cleanUp.accept(ast);
      telemetryAnalysisKeys = javaAnalysisKeys.success();
      telemetry.aggregateAsCounter(javaAnalysisKeys.success().typeErrorCountKey(), undefinedTypes.size());
    } catch (RecognitionException e) {
      checkInterrupted(e);
      LOG.error(String.format(LOG_ERROR_UNABLE_TO_PARSE_FILE, inputFile));
      LOG.error(e.getMessage());

      parseErrorWalkAndVisit(e, inputFile);
      telemetryAnalysisKeys = javaAnalysisKeys.parseErrors();
    } catch (AnalysisException e) {
      throw e;
    } catch (Exception e) {
      checkInterrupted(e);
      interruptIfFailFast(e, inputFile);
    } catch (StackOverflowError error) {
      LOG.error(String.format(LOG_ERROR_STACKOVERFLOW, inputFile), error);
      throw error;
    } finally {
      telemetry.aggregateAsCounter(telemetryAnalysisKeys.sizeCharsKey(), InputFileUtils.charCount(inputFile, 0));
      telemetry.aggregateAsCounter(telemetryAnalysisKeys.timeMsKey(), currentTimeMillis() - startTime);
    }
  }

  private static void cleanUpAst(JavaTree.CompilationUnitTreeImpl ast) {
    // release environment used for semantic resolution
    ast.sema.getEnvironmentCleaner().run();
  }

  private void collectUndefinedTypes(String path, Set<JProblem> undefinedTypes) {
    if (sonarComponents != null) {
      sonarComponents.collectUndefinedTypes(path, undefinedTypes);
    }
  }

  void logMisconfiguredVersion(String inputFile, JavaVersion javaVersion) {
    if (!reportedMisconfiguredVersion) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(String.format(LOG_WARN_MISCONFIGURED_JAVA_VERSION, inputFile, JavaVersion.SOURCE_VERSION, javaVersion.asInt()));
      }
      reportedMisconfiguredVersion = true;
    }
  }

  private void interruptIfFailFast(Exception e, InputFile inputFile) {
    if (shouldFailAnalysis()) {
      throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e);
    }
  }

  public boolean shouldFailAnalysis() {
    return sonarComponents != null && sonarComponents.shouldFailAnalysisOnException();
  }

  public void checkInterrupted(Exception e) {
    Throwable cause = ExceptionUtils.getRootCause(e);
    if (cause instanceof InterruptedException
      || cause instanceof InterruptedIOException
      || cause instanceof CancellationException
      || analysisCancelled()) {
      throw new AnalysisException("Analysis cancelled", e);
    }
  }

  private void parseErrorWalkAndVisit(RecognitionException e, InputFile inputFile) {
    try {
      visitor.processRecognitionException(e, inputFile);
    } catch (Exception e2) {
      throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e2);
    }
  }

  private static String getAnalysisExceptionMessage(InputFile file) {
    return String.format("Unable to analyze file : '%s'", file);
  }

  public void setVisitorBridge(VisitorsBridge visitor) {
    this.visitor = visitor;
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(InputFile file, VisitorsBridge visitorsBridge) {
    scanSingleFileForTests(file, visitorsBridge, null);
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(InputFile inputFile, VisitorsBridge visitorsBridge, @Nullable SonarComponents sonarComponents) {
    JavaAstScanner astScanner = new JavaAstScanner(sonarComponents, new NoOpTelemetry(), TelemetryKey.JAVA_ANALYSIS_MAIN);
    astScanner.setVisitorBridge(visitorsBridge);
    astScanner.scan(Collections.singleton(inputFile));
  }

}
