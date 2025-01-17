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
package org.sonar.plugins.surefire;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.surefire.api.SurefireUtils;

public class SurefireSensor implements Sensor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SurefireSensor.class);

  private final SurefireJavaParser surefireJavaParser;
  private final Configuration settings;
  private final FileSystem fs;
  private final PathResolver pathResolver;

  public SurefireSensor(SurefireJavaParser surefireJavaParser, Configuration settings, FileSystem fs, PathResolver pathResolver) {
    this.surefireJavaParser = surefireJavaParser;
    this.settings = settings;
    this.fs = fs;
    this.pathResolver = pathResolver;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage("java").name("SurefireSensor");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> dirs = SurefireUtils.getReportsDirectories(settings, fs, pathResolver);
    collect(context, dirs);
  }

  protected void collect(SensorContext context, List<File> reportsDirs) {
    LOGGER.info("parsing {}", reportsDirs);
    surefireJavaParser.collect(context, reportsDirs, settings.hasKey(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
