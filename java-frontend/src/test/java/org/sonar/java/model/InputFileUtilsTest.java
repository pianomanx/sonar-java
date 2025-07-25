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

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class InputFileUtilsTest {

  private InputFile inputFile = mock(InputFile.class);

  @Test
  void md5_hash_from_bytes() {
    byte[] bytes = "content".getBytes(UTF_8);
    assertEquals("9a0364b9e99bb480dd25e1f0284c8555", InputFileUtils.hash(bytes, "MD5", 32));
    bytes = "363".getBytes(UTF_8);
    assertEquals("00411460f7c92d2124a67ea0f4cb5f85", InputFileUtils.hash(bytes, "MD5", 32));
  }

  @Test
  void md5_hash_from_input_file() throws Exception {
    when(inputFile.contents()).thenReturn("abc");
    when(inputFile.charset()).thenReturn(UTF_8);
    assertEquals("900150983cd24fb0d6963f7d28e17f72", InputFileUtils.md5Hash(inputFile));
  }

  @Test
  void md5_hash_from_invalid_input_file() throws Exception {
    when(inputFile.contents()).thenThrow(new IOException("Boom!"));
    when(inputFile.charset()).thenReturn(UTF_8);
    assertThatThrownBy(() -> InputFileUtils.md5Hash(inputFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("java.io.IOException: Boom!");
  }

  @Test
  void hash_using_invalid_algorithm() {
    byte[] bytes = "363".getBytes(UTF_8);
    assertThatThrownBy(() -> InputFileUtils.hash(bytes, "invalid-algorithm", 32))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("invalid-algorithm not supported");
  }

  @Test
  void char_count() throws IOException {
    when(inputFile.contents()).thenReturn("1234");
    assertThat(InputFileUtils.charCount(inputFile, -1)).isEqualTo(4);
  }

  @Test
  void char_count_on_error() throws IOException {
    when(inputFile.contents()).thenThrow(new IOException("Boom!"));
    assertThat(InputFileUtils.charCount(inputFile, 42)).isEqualTo(42);
  }

}
