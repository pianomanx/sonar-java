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

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class DroppedPropertiesSensorTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void test() throws Exception {
    SensorContextTester contextTester = SensorContextTester.create(tmp.newFolder());
    MapSettings mapSettings = new MapSettings().setProperty("sonar.jacoco.reportPaths", "/path");
    contextTester.setSettings(mapSettings);
    List<String> analysisWarnings = new ArrayList<>();
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(analysisWarnings::add);
    sensor.execute(contextTester);

    String msg = "Property 'sonar.jacoco.reportPaths' is no longer supported. Use JaCoCo's xml report and sonar-jacoco plugin.";
    assertThat(logTester.logs(Level.WARN)).contains(msg);
    assertThat(analysisWarnings).containsExactly(msg);
  }

  @Test
  void test_two_reportPaths_property() throws Exception {
    SensorContextTester contextTester = SensorContextTester.create(tmp.newFolder());
    MapSettings mapSettings = new MapSettings().setProperty("sonar.jacoco.reportPaths", "/path")
      .setProperty("sonar.jacoco.reportPath", "/path");
    contextTester.setSettings(mapSettings);
    List<String> analysisWarnings = new ArrayList<>();
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(analysisWarnings::add);
    sensor.execute(contextTester);

    assertThat(logTester.logs(Level.WARN)).isEmpty();
    assertThat(analysisWarnings).isEmpty();
  }

  @Test
  void test_two_reportPaths_property_plus_another() throws Exception {
    SensorContextTester contextTester = SensorContextTester.create(tmp.newFolder());
    MapSettings mapSettings = new MapSettings().setProperty("sonar.jacoco.reportPaths", "/path")
      .setProperty("sonar.jacoco.reportPath", "/path")
      .setProperty("sonar.jacoco.itReportPath", "/path");
    contextTester.setSettings(mapSettings);
    List<String> analysisWarnings = new ArrayList<>();
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(analysisWarnings::add);
    sensor.execute(contextTester);

    String msg = "Property 'sonar.jacoco.itReportPath' is no longer supported. Use JaCoCo's xml report and sonar-jacoco plugin.";
    assertThat(logTester.logs(Level.WARN)).contains(msg);
    assertThat(analysisWarnings).containsExactly(msg);
  }

  @Test
  void test_empty() throws Exception {
    SensorContextTester contextTester = SensorContextTester.create(tmp.newFolder());
    List<String> analysisWarnings = new ArrayList<>();
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(analysisWarnings::add);
    sensor.execute(contextTester);

    assertThat(logTester.logs(Level.WARN)).isEmpty();
    assertThat(analysisWarnings).isEmpty();
  }

  @Test
  void test_descriptor() {
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(w -> {
    });
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isNotBlank();
    Configuration emptyConfig = new MapSettings().asConfig();
    assertThat(descriptor.configurationPredicate().test(emptyConfig)).isFalse();
    Configuration removedProperty = new MapSettings().setProperty("sonar.jacoco.reportPaths", "/path").asConfig();
    assertThat(descriptor.configurationPredicate().test(removedProperty)).isTrue();
  }

}
