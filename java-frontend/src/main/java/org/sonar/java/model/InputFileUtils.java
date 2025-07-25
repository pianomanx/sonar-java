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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;

public final class InputFileUtils {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileUtils.class);

  private InputFileUtils() {
    // utility class
  }

  public static String md5Hash(InputFile inputFile) {
    String contents;
    try {
      contents = inputFile.contents();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return hash(contents.getBytes(inputFile.charset()), "MD5", 32);
  }

  public static String hash(byte[] input, String algorithm, int expectedLength) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(algorithm + " not supported", e);
    }
    md.update(input);
    String suffix = new BigInteger(1,md.digest()).toString(16);
    String prefix = "0".repeat(expectedLength - suffix.length());
    return prefix + suffix;
  }

  public static int charCount(InputFile inputFile, int defaultOnError) {
    try {
      return inputFile.contents().length();
    } catch (IOException e) {
      LOG.debug("Error, failed to get content size for: {}, {}: {}" , inputFile, e.getClass().getSimpleName(), e.getMessage());
      return defaultOnError;
    }
  }

}
