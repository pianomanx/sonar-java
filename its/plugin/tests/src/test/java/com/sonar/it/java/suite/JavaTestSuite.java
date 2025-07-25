/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource SA
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
package com.sonar.it.java.suite;

import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.junit4.OrchestratorRuleBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JavaExtensionsTest.class,
  JavaTutorialTest.class,
  UnitTestsTest.class,
  JavaTest.class,
  JavaComplexityTest.class,
  PackageInfoTest.class,
  Struts139Test.class,
  JavaClasspathTest.class,
  SuppressWarningTest.class,
  SonarLintTest.class,
  ExternalReportTest.class,
  DuplicationTest.class,
  MultiModuleTelemetryTest.class
})
public class JavaTestSuite {

  public static final FileLocation JAVA_PLUGIN_LOCATION = FileLocation.of(TestClasspathUtils.findModuleJarPath("../../../sonar-java-plugin").toFile());
  public static final FileLocation TUTORIAL_EXAMPLE_PLUGIN_LOCATION = FileLocation.of(TestClasspathUtils.findModuleJarPath("../../../docs/java-custom-rules-example").toFile());

  @ClassRule
  public static final OrchestratorRule ORCHESTRATOR;

  static {
    OrchestratorRuleBuilder orchestratorBuilder = OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
      .setEdition(com.sonar.orchestrator.container.Edition.ENTERPRISE_LW)
      .activateLicense()
      .addPlugin(JAVA_PLUGIN_LOCATION)
      // for support of custom rules
      .addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")))
      // making sure the tutorial is still working
      .addPlugin(TUTORIAL_EXAMPLE_PLUGIN_LOCATION)
      // profiles for each test projects
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-extension.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-tutorial.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-version-aware-visitor.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-dit.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-ignored-test.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-complexity.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-filtered-issues.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-using-aar-dep.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-package-info.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings-pmd.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-depends-on-jdk-types.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-multi-module.xml"));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  public static String keyFor(String projectKey, String pkgDir, String cls) {
    return projectKey + ":src/main/java/" + pkgDir + cls;
  }

  @CheckForNull
  static Measure getMeasure(String componentKey, String metricKey) {
    Measures.ComponentWsResponse response = TestUtils.newWsClient(ORCHESTRATOR).measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(singletonList(metricKey)));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  static Map<String, Measure> getMeasures(String componentKey, String... metricKeys) {
    return TestUtils.newWsClient(ORCHESTRATOR).measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  @CheckForNull
  static Integer getMeasureAsInteger(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Integer.parseInt(measure.getValue());
  }

  @CheckForNull
  static Double getMeasureAsDouble(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Double.parseDouble(measure.getValue());
  }

  static Component getComponent(String componentKey) {
    return TestUtils.newWsClient(ORCHESTRATOR).components().show(new ShowRequest().setComponent(componentKey)).getComponent();
  }

}
