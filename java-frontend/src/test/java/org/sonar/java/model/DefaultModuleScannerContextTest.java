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
package org.sonar.java.model;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;
import org.sonar.java.caching.DummyCache;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.caching.CacheContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultModuleScannerContextTest {

  @Test
  void test_addIssueOnProject_delegates_to_SonarComponents() {
    var sonarComponents = spy(
      new SonarComponents(null, null, null, null, null, null)
    );
    var inputComponent = mock(InputComponent.class);
    doReturn(inputComponent).when(sonarComponents).project();
    doNothing().when(sonarComponents).addIssue(any(InputComponent.class), any(JavaCheck.class), any(int.class), any(String.class), any(int.class));
    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );
    var check = new JavaCheck() {
    };
    verify(sonarComponents, never()).addIssue(any(InputComponent.class), any(JavaCheck.class), any(int.class), any(String.class), any(int.class));
    context.addIssueOnProject(check, "Hello, World!");
    verify(sonarComponents, times(1)).addIssue(inputComponent, check, -1, "Hello, World!", 0);
  }

  @Test
  void test_getJavaVersion_returns_expected_version() {
    var context = new DefaultModuleScannerContext(
      null,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      null
    );
    assertThat(context.getJavaVersion().asInt()).isEqualTo(JavaVersionImpl.MAX_SUPPORTED);
    context = new DefaultModuleScannerContext(
      null,
      new JavaVersionImpl(13),
      true,
      null
    );
    assertThat(context.getJavaVersion().asInt()).isEqualTo(13);
  }

  @Test
  void test_inAndroidContext_returns_expected_version() {
    var context = new DefaultModuleScannerContext(
      null,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      null
    );
    assertThat(context.inAndroidContext()).isTrue();
    context = new DefaultModuleScannerContext(
      null,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );
    assertThat(context.inAndroidContext()).isFalse();
  }

  @Test
  void test_getProject_delegates_to_SonarComponents() {
    var sonarComponents = spy(
      new SonarComponents(null, null, null, null, null, null)
    );
    var inputComponent = mock(InputComponent.class);
    doReturn(inputComponent).when(sonarComponents).project();
    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      null
    );
    verify(sonarComponents, never()).project();
    InputComponent project = context.getProject();
    verify(sonarComponents, times(1)).project();
    assertThat(project).isEqualTo(inputComponent);
  }

  @Test
  void test_getWorkingDirectory_delegates_to_SonarComponents() {
    var sonarComponents = spy(
      new SonarComponents(null, null, null, null, null, null)
    );
    var expectedWorkDir = mock(File.class);
    doReturn(expectedWorkDir).when(sonarComponents).projectLevelWorkDir();
    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      null
    );
    verify(sonarComponents, never()).projectLevelWorkDir();
    var workDir = context.getWorkingDirectory();
    verify(sonarComponents, times(1)).projectLevelWorkDir();
    assertThat(workDir).isEqualTo(expectedWorkDir);
  }

  @Test
  void test_getCacheContext_returns_a_disabled_cache_when_no_sensor_context() {
    var sonarComponents = mock(SonarComponents.class);
    doReturn(null).when(sonarComponents).context();
    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      null
    );
    CacheContext cacheContext = context.getCacheContext();
    assertThat(cacheContext.isCacheEnabled()).isFalse();
    assertThat(cacheContext.getReadCache()).isInstanceOf(DummyCache.class);
    assertThat(cacheContext.getWriteCache()).isInstanceOf(DummyCache.class);
  }

  @Test
  void test_getCacheContext_returns_a_disabled_cache_when_caching_is_server_disabled() {
    var sensorContext = mock(SensorContext.class);
    doReturn(false).when(sensorContext).isCacheEnabled();
    var sonarComponents = mock(SonarComponents.class);
    doReturn(sensorContext).when(sonarComponents).context();

    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      null
    );
    CacheContext cacheContext = context.getCacheContext();
    assertThat(cacheContext.isCacheEnabled()).isFalse();
    assertThat(cacheContext.getReadCache()).isInstanceOf(DummyCache.class);
    assertThat(cacheContext.getWriteCache()).isInstanceOf(DummyCache.class);
  }

  @Test
  void test_getCacheContext_returns_the_CacheContext_passed_at_construction() {
    var cacheContext = mock(CacheContext.class);
    var context = new DefaultModuleScannerContext(
      null,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      true,
      cacheContext
    );
    assertThat(context.getCacheContext()).isEqualTo(cacheContext);
  }

  @Test
  void getRootProjectWorkingDirectory_returns_the_working_dir_from_sonarComponents() {
    var projectLevelWorkDirFile = new File("foo");
    var sonarComponents = mock(SonarComponents.class);
    doReturn(projectLevelWorkDirFile).when(sonarComponents).projectLevelWorkDir();

    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );
    assertThat(context.getRootProjectWorkingDirectory()).isSameAs(projectLevelWorkDirFile);
  }

  @ParameterizedTest
  @EnumSource(SonarProduct.class)
  void should_properly_report_sonar_product(SonarProduct product) {
    var runtime = mock(SonarRuntime.class);
    doReturn(product).when(runtime).getProduct();

    var sensorContext = mock(SensorContext.class);
    doReturn(runtime).when(sensorContext).runtime();

    var sonarComponents = mock(SonarComponents.class);
    doReturn(sensorContext).when(sonarComponents).context();

    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );

    assertThat(context.sonarProduct())
      .isEqualTo(product);
  }

  @Test
  void should_not_report_product_if_sonarcomponents_is_not_available() {
    var context = new DefaultModuleScannerContext(
      null,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );

    assertThat(context.sonarProduct())
      .isNull();
  }

  @Test
  void should_not_report_product_if_no_sensor_context_is_available() {
    var sonarComponents = mock(SonarComponents.class);
    doReturn(null).when(sonarComponents).context();

    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );

    assertThat(context.sonarProduct())
      .isNull();
  }

  @Test
  void test_getConfiguration() {
    var sonarComponents = mock(SonarComponents.class);
    var configuration = mock(Configuration.class);
    doReturn(configuration).when(sonarComponents).getConfiguration();

    var context = new DefaultModuleScannerContext(
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION,
      false,
      null
    );

    assertThat(context.getConfiguration()).isSameAs(configuration);
  }
}
