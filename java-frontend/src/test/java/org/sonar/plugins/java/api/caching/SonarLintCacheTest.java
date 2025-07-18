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
package org.sonar.plugins.java.api.caching;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SonarLintCacheTest {
  @Test
  void read_non_existing_key() {
    SonarLintCache sonarLintCache = new SonarLintCache();
    assertThatThrownBy(() -> sonarLintCache.read("foo")).hasMessage("SonarLintCache does not contain key \"foo\"");
  }

  @Test
  void write_and_read_existing_key() throws IOException {
    SonarLintCache sonarLintCache = new SonarLintCache();
    byte[] bytes = {42};
    sonarLintCache.write("foo", bytes);
    try (var value = sonarLintCache.read("foo")) {
      assertThat(value.readAllBytes()).isEqualTo(bytes);
    }

    sonarLintCache.write("bar", new ByteArrayInputStream(bytes));
    try (var value = sonarLintCache.read("bar")) {
      assertThat(value.readAllBytes()).isEqualTo(bytes);
    }
  }

  @Test
  void contains() {
    SonarLintCache sonarLintCache = new SonarLintCache();
    assertThat(sonarLintCache.contains("foo")).isFalse();
    byte[] bytes = {42};
    sonarLintCache.write("foo", bytes);
    assertThat(sonarLintCache.contains("foo")).isTrue();
    assertThat(sonarLintCache.contains("bar")).isFalse();
  }

  @Test
  void write_non_valid_input_stream() throws IOException {
    InputStream inputStream = mock(InputStream.class);
    when(inputStream.readAllBytes()).thenThrow(IOException.class);

    SonarLintCache sonarLintCache = new SonarLintCache();
    assertThatThrownBy(() -> sonarLintCache.write("foo", inputStream)).isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(IOException.class);
  }

  @Test
  void write_same_key() {
    SonarLintCache sonarLintCache = new SonarLintCache();
    byte[] bytes1 = {42};
    byte[] bytes2 = {0, 1, 2};
    sonarLintCache.write("foo", bytes1);
    assertThatThrownBy(() -> sonarLintCache.write("foo", bytes2)).hasMessage("Same key cannot be written to multiple times (foo)");
    assertThatThrownBy(() -> sonarLintCache.write("foo", new ByteArrayInputStream(bytes2))).hasMessage(
      "Same key cannot be written to multiple times (foo)"
    );
  }

  @Test
  void copy_from_previous() {
    SonarLintCache sonarLintCache = new SonarLintCache();
    assertThatThrownBy(() -> sonarLintCache.copyFromPrevious("foo")).hasMessage("SonarLintCache does not allow to copy from previous.");
  }
}
