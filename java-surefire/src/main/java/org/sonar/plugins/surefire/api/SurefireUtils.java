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
package org.sonar.plugins.surefire.api;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;

/**
 * @since 2.4
 */
public final class SurefireUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(SurefireUtils.class);
  /**
   * @since 4.11
   */
  public static final String SUREFIRE_REPORT_PATHS_PROPERTY = "sonar.junit.reportPaths";

  private SurefireUtils() {
  }

  /**
   * Find the directories containing the surefire reports.
   * @param settings Analysis settings.
   * @param fs FileSystem containing indexed files.
   * @param pathResolver Path solver.
   * @return The directories containing the surefire reports or default one (target/surefire-reports) if not found (not configured or not found).
   */
  public static List<File> getReportsDirectories(Configuration settings, FileSystem fs, PathResolver pathResolver) {
    List<File> dirs = getReportsDirectoriesFromProperty(settings, fs, pathResolver);
    if (dirs != null) {
      return dirs;
    }
    return Collections.singletonList(new File(fs.baseDir(), "target/surefire-reports"));
  }

  @CheckForNull
  private static List<File> getReportsDirectoriesFromProperty(Configuration settings, FileSystem fs, PathResolver pathResolver) {
    if(settings.hasKey(SUREFIRE_REPORT_PATHS_PROPERTY)) {
      return Arrays.stream(settings.getStringArray(SUREFIRE_REPORT_PATHS_PROPERTY))
        .map(String::trim)
        .map(path -> getFileFromPath(fs, pathResolver, path))
        .filter(Objects::nonNull)
        .toList();
    }
    return null;
  }

  @CheckForNull
  private static File getFileFromPath(FileSystem fs, PathResolver pathResolver, String path) {
    try {
      return pathResolver.relativeFile(fs.baseDir(), path);
    } catch (Exception e) {
      // exceptions on file not found was only occurring with SQ 5.6 LTS, not with SQ 6.4
      LOGGER.info("Surefire report path: {}/{} not found.", fs.baseDir(), path);
    }
    return null;
  }

}
