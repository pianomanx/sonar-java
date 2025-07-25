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
package org.sonar.plugins.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.Version;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.DefaultTelemetry;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JspCodeVisitor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class JavaSensorTest {

  private static final CheckFactory checkFactory = mock(CheckFactory.class);
  private static final Checks<Object> checks = mock(Checks.class);

  static {
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("java", RuleAnnotationUtils.getRuleKey(BadMethodNameCheck.class)));
    when(checkFactory.create(anyString())).thenReturn(checks);
  }

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private Telemetry telemetry = new DefaultTelemetry();

  @Test
  void test_toString() throws IOException {
    SonarComponents sonarComponents = createSonarComponentsMock(createContext(InputFile.Type.MAIN));
    assertThat(new JavaSensor(sonarComponents, null, null, null, null, null, telemetry)).hasToString("JavaSensor");
  }

  @Test
  void test_issues_creation_on_main_file() throws IOException {
    // Expected issues : the number of methods violating BadMethodName rule. Currently, 18 tests.
    testIssueCreation(InputFile.Type.MAIN, 15);

    Map<String, String> telemetryMap = telemetry.toMap();
    assertThat(telemetryMap).containsOnlyKeys(
      "java.analysis.main.success.size_chars",
      "java.analysis.main.success.time_ms",
      "java.analysis.main.success.type_error_count",
      "java.dependency.lombok",
      "java.dependency.spring-boot",
      "java.dependency.spring-web",
      "java.is_autoscan",
      "java.language.version",
      "java.module_count",
      "java.scanner_app");
    assertThat(telemetryMap.get("java.analysis.main.success.size_chars")).matches("\\d{5}");
    assertThat(telemetryMap.get("java.analysis.main.success.time_ms")).matches("\\d+");
    assertThat(telemetryMap).containsEntry("java.analysis.main.success.type_error_count", "199");
  }

  @Test
  // Renaming this method will break lineNumberOfTheMethodWithNoSonar(fs). The name is used to locate the line number.
  void test_issues_creation_on_test_file() throws IOException { // NOSONAR required to test NOSONAR reporting on test files
    testIssueCreation(InputFile.Type.TEST, 0);

    Map<String, String> telemetryMap = telemetry.toMap();
    assertThat(telemetryMap).containsOnlyKeys(
      "java.analysis.test.success.size_chars",
      "java.analysis.test.success.time_ms",
      "java.analysis.test.success.type_error_count",
      "java.dependency.lombok",
      "java.dependency.spring-boot",
      "java.dependency.spring-web",
      "java.is_autoscan",
      "java.language.version",
      "java.module_count",
      "java.scanner_app");
    assertThat(telemetryMap.get("java.analysis.test.success.size_chars")).matches("\\d{5}");
    assertThat(telemetryMap.get("java.analysis.test.success.time_ms")).matches("\\d+");
    assertThat(telemetryMap).containsEntry("java.analysis.test.success.type_error_count", "199");
  }

  private static int lineNumberOfTheMethodWithNoSonar(FileSystem fs) throws IOException {
    String[] lines = fs.inputFile(fs.predicates().hasPath("org/sonar/plugins/java/JavaSensorTest.java")).contents().split("\n");
    int zeroBasedLineIndex = (int) Stream.of(lines)
      .takeWhile(line -> !line.contains("test_issues_creation_on_test_file"))
      .count();
    return zeroBasedLineIndex + 1;
  }

  private void testIssueCreation(InputFile.Type onType, int expectedIssues) throws IOException {
    logTester.setLevel(Level.DEBUG);
    MapSettings settings = new MapSettings();
    settings.setProperty(JavaVersion.SOURCE_VERSION, "22");
    settings.setProperty("sonar.scanner.app", "ScannerJavaSensorTest");

    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    SensorContextTester context = spy(createContext(onType).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7))));
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(tmp.newFolder().toPath());
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    DefaultJavaResourceLocator javaResourceLocator = createDefaultJavaResourceLocator(settings.asConfig(), fs);

    JavaSensor jss = new JavaSensor(sonarComponents, fs, javaResourceLocator, settings.asConfig(), noSonarFilter, null, telemetry);

    jss.execute(context);
    int expectedNoSonarLine = lineNumberOfTheMethodWithNoSonar(fs);
    verify(noSonarFilter, times(1)).noSonarInFile(fs.inputFiles().iterator().next(), Collections.singleton(expectedNoSonarLine));
    verify(sonarComponents, times(expectedIssues)).reportIssue(any(AnalyzerMessage.class));

    // There are additional entries, but we do not test them.
    assertThat(telemetry.toMap()).contains(
      entry("java.dependency.lombok", "absent"),
      entry("java.dependency.spring-boot", "absent"),
      entry("java.dependency.spring-web", "absent"),
      entry("java.is_autoscan", "false"),
      entry("java.language.version", "22"),
      entry("java.module_count", "1"),
      entry("java.scanner_app", "ScannerJavaSensorTest"));

    settings.setProperty(JavaVersion.SOURCE_VERSION, "wrongFormat");
    jss.execute(context);

    assertThat(telemetry.toMap()).contains(
      entry("java.language.version", "22,none"),
      entry("java.module_count", "2"));

    settings.setProperty(JavaVersion.SOURCE_VERSION, "1.7");
    jss.execute(context);

    assertThat(telemetry.toMap()).contains(
      entry("java.language.version", "7,22,none"),
      entry("java.module_count", "3"));
  }

  private static SensorContextTester createContext(InputFile.Type onType) throws IOException {
    SensorContextTester context = SensorContextTester.create(new File("src/test/java/").getAbsoluteFile());
    DefaultFileSystem fs = context.fileSystem();

    String effectiveKey = "org/sonar/plugins/java/JavaSensorTest.java";
    File file = new File(fs.baseDir(), effectiveKey);
    DefaultInputFile inputFile = new TestInputFileBuilder("", effectiveKey).setLanguage("java").setModuleBaseDir(fs.baseDirPath())
      .setType(onType)
      .initMetadata(new String(Files.readAllBytes(file.toPath()), UTF_8))
      .setCharset(UTF_8)
      .build();
    fs.add(inputFile);
    return context;
  }

  private static SonarComponents createSonarComponentsMock(SensorContextTester contextTester) {
    DefaultFileSystem fs = contextTester.fileSystem();
    ClasspathForTest javaTestClasspath = new ClasspathForTest(contextTester.config(), fs);
    ClasspathForMain javaClasspath = new ClasspathForMain(contextTester.config(), fs);

    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    SonarComponents sonarComponents = spy(new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath,
      checkFactory, mock(ActiveRules.class)));
    sonarComponents.setSensorContext(contextTester);

    BadMethodNameCheck check = new BadMethodNameCheck();
    when(sonarComponents.mainChecks()).thenReturn(Collections.singletonList(check));
    return sonarComponents;
  }

  private static DefaultJavaResourceLocator createDefaultJavaResourceLocator(Configuration settings, DefaultFileSystem fs) {
    ClasspathForMain classpathForMain = new ClasspathForMain(settings, fs);
    ClasspathForTest classpathForTest = new ClasspathForTest(settings, fs);

    return new DefaultJavaResourceLocator(classpathForMain, classpathForTest);
  }

  @Test
  void should_invoke_visitors_on_generated_code() throws Exception {
    assertJasperIsInvoked(new MapSettings());
  }

  @Test
  void should_invoke_visitors_on_generated_code_in_batch_mode() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.experimental.batchModeSizeInKB", "10");
    assertJasperIsInvoked(settings);
  }

  private void assertJasperIsInvoked(MapSettings settings) throws IOException {
    Path base = tmp.newFolder().toPath();
    Path generatedFilePath = tmp.newFile("Generated.java").toPath();
    Files.write(generatedFilePath, "class Generated {}".getBytes());
    GeneratedFile generatedFile = new GeneratedFile(generatedFilePath);

    SensorContextTester context = SensorContextTester.create(base);
    context.setSettings(settings);
    context.fileSystem().setWorkDir(tmp.newFolder().toPath());
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    JavaFileScanner javaFileScanner = mock(JavaFileScanner.class);
    JspCodeScanner testCodeVisitor = mock(JspCodeScanner.class);
    when(sonarComponents.jspChecks()).thenReturn(Collections.singletonList(testCodeVisitor));
    when(sonarComponents.mainChecks()).thenReturn(Collections.singletonList(javaFileScanner));

    Jasper jasper = mock(Jasper.class);
    when(jasper.generateFiles(any(), any())).thenReturn(asList(generatedFile));
    JavaSensor jss = new JavaSensor(sonarComponents, context.fileSystem(), mock(JavaResourceLocator.class),
      new MapSettings().asConfig(), mock(NoSonarFilter.class), null, jasper, telemetry);
    jss.execute(context);

    ArgumentCaptor<JavaFileScannerContext> scannerContext = ArgumentCaptor.forClass(JavaFileScannerContext.class);
    verify(testCodeVisitor, times(1)).scanFile(scannerContext.capture());
    assertThat(scannerContext.getValue().getInputFile()).isSameAs(generatedFile);

    // normal visitors are not invoked on generated files
    verify(javaFileScanner, never()).scanFile(any());
  }

  @Test
  void should_not_invoke_jasper_jsp_compilation_in_autoscan_for_security_reasons() throws Exception {
    Path base = tmp.newFolder().toPath();
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");

    SensorContextTester context = SensorContextTester.create(base);
    context.setSettings(settings);
    context.fileSystem().setWorkDir(tmp.newFolder().toPath());
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    JspCodeScanner jspCodeVisitor = mock(JspCodeScanner.class);
    when(sonarComponents.mainChecks()).thenReturn(Collections.emptyList());
    when(sonarComponents.testChecks()).thenReturn(Collections.emptyList());
    when(sonarComponents.jspChecks()).thenReturn(Collections.singletonList(jspCodeVisitor));

    Jasper jasper = mock(Jasper.class);
    JavaSensor jss = new JavaSensor(sonarComponents, context.fileSystem(), mock(JavaResourceLocator.class),
      context.config(), mock(NoSonarFilter.class), null, jasper, telemetry);
    jss.execute(context);

    verify(jasper, never()).generateFiles(any(), any());
    verify(jspCodeVisitor, never()).scanFile(any());
  }

  @Test
  void performance_measure_should_not_be_activated_by_default() throws IOException {
    logTester.setLevel(Level.DEBUG);
    MapSettings settings = new MapSettings();
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(Level.DEBUG));
    assertThat(debugLogs).doesNotContain("Performance Measures:");
    Path performanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(performanceFile).doesNotExist();
  }

  @Test
  void performance_measure_should_log_in_debug_mode() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.performance.measure", "true");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(Level.DEBUG));
    assertThat(debugLogs).contains("Performance Measures:\n{ \"name\": \"JavaSensor\"");
    Path performanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(performanceFile).exists();
    assertThat(new String(Files.readAllBytes(performanceFile), UTF_8)).contains("\"JavaSensor\"");
  }

  @Test
  void custom_performance_measure_file_path_can_be_provided() throws IOException {
    logTester.setLevel(Level.DEBUG);
    MapSettings settings = new MapSettings();
    Path workDir = tmp.newFolder().toPath();
    Path customPerformanceFile = workDir.resolve("custom.performance.measure.json");
    settings.setProperty("sonar.java.performance.measure", "true");
    settings.setProperty("sonar.java.performance.measure.path", customPerformanceFile.toString());
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(Level.DEBUG));
    assertThat(debugLogs).contains("{ \"name\": \"JavaSensor\"");
    Path defaultPerformanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(defaultPerformanceFile).doesNotExist();
    assertThat(customPerformanceFile).exists();
    assertThat(new String(Files.readAllBytes(customPerformanceFile), UTF_8)).contains("\"JavaSensor\"");
  }

  @Test
  void custom_performance_measure_file_path_can_be_empty() throws IOException {
    logTester.setLevel(Level.DEBUG);
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.performance.measure", "true");
    settings.setProperty("sonar.java.performance.measure.path", "");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    String debugLogs = String.join("\n", logTester.logs(Level.DEBUG));
    assertThat(debugLogs).contains("{ \"name\": \"JavaSensor\"");
    Path defaultPerformanceFile = workDir.resolve("sonar.java.performance.measure.json");
    assertThat(defaultPerformanceFile).exists();
    assertThat(new String(Files.readAllBytes(defaultPerformanceFile), UTF_8)).contains("\"JavaSensor\"");
  }

  /*@Test
  void test_java_version_automatically_accepts_enablePreview_flag_when_maximum_version() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.source", JavaVersionImpl.MAX_SUPPORTED);
    settings.setProperty("sonar.java.enablePreview", "True");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
    List<String> infoLogs = logTester.logs(Level.INFO);
    assertThat(infoLogs).contains("Configured Java source version (sonar.java.source): " + JavaVersionImpl.MAX_SUPPORTED +
      ", preview features enabled (sonar.java.enablePreview): true");
  }*/

  @Test
  void test_java_version_automatically_disables_enablePreview_flag_when_version_is_less_than_maximum_version() throws IOException {
    MapSettings settings = new MapSettings();
    int version = JavaVersionImpl.MAX_SUPPORTED - 1;
    settings.setProperty("sonar.java.source", version);
    settings.setProperty("sonar.java.enablePreview", "True");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    assertThat(logTester.logs(Level.WARN)).contains(
      "sonar.java.enablePreview is set but will be discarded as the Java version is less than the max supported version (" +
        version + " < " + JavaVersionImpl.MAX_SUPPORTED + ")"
    );
    List<String> infoLogs = logTester.logs(Level.INFO);
    assertThat(infoLogs).contains("Configured Java source version (sonar.java.source): " + version +
      ", preview features enabled (sonar.java.enablePreview): false");
  }

  @Test
  void getJavaVersion_does_not_try_to_check_consistency_when_sonar_java_source_is_not_set() throws IOException {
    // We set the sonar.java.enablePreview flag to true but it will be ignored because there is no sonar.java.source
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.enablePreview", "true");
    Path workDir = tmp.newFolder().toPath();
    executeJavaSensorForPerformanceMeasure(settings, workDir);
    assertThat(logTester.logs(Level.WARN)).noneMatch(
      log -> log.startsWith("sonar.java.enablePreview is set but will be discarded as the Java version is less than the max supported version")
    );
    List<String> infoLogs = logTester.logs(Level.INFO);
    assertThat(infoLogs).noneMatch(log -> log.startsWith("Configured Java source version (sonar.java.source):"));
  }

  @Test
  void do_not_filter_checks_when_no_autoscan() throws IOException {
    MapSettings settings = new MapSettings();
    // no "sonar.internal.analysis.autoscan"
    SensorContextTester context = analyzeTwoFilesWithIssues(settings);
    assertThat(context.allIssues())
      .extracting(issue -> issue.ruleKey().toString())
      .contains(
        "CustomRepository:CustomMainCheck",
        "CustomRepository:CustomJspCheck",
        "CustomRepository:CustomTestCheck",
        // not in SonarWay (FileHeaderCheck)
        "java:S1451",
        // main check in SonarWay (DefaultPackageCheck)
        "java:S1220",
        // main check in SonarWay, not supported by autoscan (CombineCatchCheck)
        "java:S2147",
        // test check in SonarWay (NoTestInTestClassCheck)
        "java:S2187"
      );
  }

  @Test
  void filter_checks_when_autoscan_true() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    SensorContextTester context = analyzeTwoFilesWithIssues(settings);
    assertThat(context.allIssues())
      .extracting(issue -> issue.ruleKey().toString())
      .contains(
        // main check in SonarWay
        "java:S1220",
        // test check in SonarWay
        "java:S2187"
      ).doesNotContain(
      "CustomRepository:CustomMainCheck",
      "CustomRepository:CustomJspCheck",
      "CustomRepository:CustomTestCheck",
      // main check in SonarWay, not supported by autoscan (CombineCatchCheck)
      "java:S2147",
      // not in SonarWay (FileHeaderCheck)
      "java:S1451"
    );
  }

  @Test
  void test_describe_sensor() throws IOException {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    SonarComponents sonarComponents = createSonarComponentsMock(createContext(InputFile.Type.MAIN));
    var sensor = new JavaSensor(sonarComponents, null, null, null, null, null, telemetry);
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("JavaSensor");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

  private SensorContextTester analyzeTwoFilesWithIssues(MapSettings settings) throws IOException {
    SensorContextTester context = SensorContextTester.create(new File("src/test/files").getAbsoluteFile())
      .setSettings(settings)
      .setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(8, 7), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));

    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(tmp.newFolder().toPath());

    File mainFile = new File(fs.baseDir(), "CodeWithIssues.java");
    fs.add(new TestInputFileBuilder("", mainFile.getName()).setLanguage("java").setModuleBaseDir(fs.baseDirPath())
      .setType(InputFile.Type.MAIN).initMetadata(Files.readString(mainFile.toPath())).setCharset(UTF_8).build());

    File testFile = new File(fs.baseDir(), "CodeWithIssuesTest.java");
    fs.add(new TestInputFileBuilder("", testFile.getName()).setLanguage("java").setModuleBaseDir(fs.baseDirPath())
      .setType(InputFile.Type.TEST).initMetadata(Files.readString(testFile.toPath())).setCharset(UTF_8).build());

    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(mock(FileLinesContext.class));
    ClasspathForTest javaTestClasspath = new ClasspathForTest(context.config(), fs);
    ClasspathForMain javaClasspath = new ClasspathForMain(context.config(), fs);
    DefaultJavaResourceLocator resourceLocator = createDefaultJavaResourceLocator(context.config(), fs);

    CheckRegistrar[] checkRegistrars = new CheckRegistrar[] {new CustomRegistrar()};

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();

    GeneratedCheckList.getChecks().stream()
      .map(check -> AnnotationUtils.getAnnotation(check, org.sonar.check.Rule.class).key())
      .map(key -> new NewActiveRule.Builder().setRuleKey(RuleKey.of("java", key)).build())
      .forEach(activeRulesBuilder::addRule);

    Stream.of("CustomMainCheck", "CustomJspCheck", "CustomTestCheck")
      .map(key -> new NewActiveRule.Builder().setRuleKey(RuleKey.of("CustomRepository", key)).build())
      .forEach(activeRulesBuilder::addRule);

    CheckFactory specificCheckFactory = new CheckFactory(activeRulesBuilder.build());

    SonarComponents components = new SonarComponents(fileLinesContextFactory, fs,
      javaClasspath, javaTestClasspath, specificCheckFactory, context.activeRules(), checkRegistrars, null, null);

    JavaSensor jss = new JavaSensor(components, fs, resourceLocator, context.config(), mock(NoSonarFilter.class), null, telemetry);
    jss.execute(context);
    return context;
  }

  private void executeJavaSensorForPerformanceMeasure(MapSettings settings, Path workDir) throws IOException {
    Configuration configuration = settings.asConfig();
    SensorContextTester context = createContext(InputFile.Type.MAIN)
      .setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(8, 7), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));
    context.setSettings(settings);
    DefaultFileSystem fs = context.fileSystem();
    fs.setWorkDir(workDir);
    SonarComponents components = createSonarComponentsMock(context);
    DefaultJavaResourceLocator resourceLocator = createDefaultJavaResourceLocator(context.config(), fs);
    JavaSensor jss = new JavaSensor(components, fs, resourceLocator, configuration, mock(NoSonarFilter.class), null, telemetry);
    jss.execute(context);
  }

  interface JspCodeScanner extends JavaFileScanner, JspCodeVisitor {
  }

  @org.sonar.check.Rule(key = "CustomMainCheck")
  public static class CustomMainCheck implements JavaFileScanner {
    public void scanFile(JavaFileScannerContext context) {
      context.reportIssue(this, context.getTree().firstToken(), "CustomMainCheck");
    }
  }

  @org.sonar.check.Rule(key = "CustomJspCheck")
  public static class CustomJspCheck implements JspCodeScanner {
    public void scanFile(JavaFileScannerContext context) {
      context.reportIssue(this, context.getTree().firstToken(), "CustomJspCheck");
    }
  }

  @org.sonar.check.Rule(key = "CustomTestCheck")
  public static class CustomTestCheck implements JavaFileScanner {
    public void scanFile(JavaFileScannerContext context) {
      context.reportIssue(this, context.getTree().firstToken(), "CustomTestCheck");
    }
  }

  public static class CustomRegistrar implements CheckRegistrar {
    @Override
    public void register(RegistrarContext registrarContext) {
      registrarContext.registerClassesForRepository("CustomRepository",
        List.of(CustomMainCheck.class, CustomJspCheck.class),
        List.of(CustomTestCheck.class));
    }
  }

}
