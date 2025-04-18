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
package org.sonar.java.se.utils;

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public class JParserTestUtils {

  private JParserTestUtils() {
    // Utility class
  }

  private static final List<File> DEFAULT_CLASSPATH = Arrays.asList(new File("target/test-classes"), new File("target/classes"));

  public static CompilationUnitTree parse(File file) {
    return parse(file, DEFAULT_CLASSPATH);
  }

  public static CompilationUnitTree parse(File file, List<File> classpath) {
    String source;
    try {
      source = Files.readLines(file, StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read file", e);
    }
    return parse(file.getName(), source, classpath);
  }

  public static CompilationUnitTree parse(String source) {
    return parse("File.java", source);
  }

  public static CompilationUnitTree parseModule(String... lines) {
    return parse("module-info.java", Arrays.stream(lines).collect(Collectors.joining("\n")));
  }

  public static CompilationUnitTree parsePackage(String... lines) {
    return parse("package-info.java", Arrays.stream(lines).collect(Collectors.joining("\n")));
  }

  private static CompilationUnitTree parse(String unitName, String source) {
    return parse(unitName, source, DEFAULT_CLASSPATH);
  }

  public static CompilationUnitTree parse(String unitName, String source, List<File> classpath) {
    JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    return JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(version, classpath).astParser(), version.toString(), unitName, source);
  }

}
