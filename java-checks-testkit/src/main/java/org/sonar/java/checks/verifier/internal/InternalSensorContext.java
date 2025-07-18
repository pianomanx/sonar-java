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
package org.sonar.java.checks.verifier.internal;

import java.io.InputStream;
import java.io.Serializable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.Version;

public class InternalSensorContext extends InternalMockedSonarAPI implements SensorContext {
  private static final Configuration CONFIG = new InternalConfiguration();
  private static final SonarRuntime RUNTIME = new InternalSonarRuntime();
  private static final FileSystem FILE_SYSTEM = new InternalFileSystem();
  private static final InputModule MODULE = new InputModule() {
    @Override
    public String key() {
      return "module";
    }

    @Override
    public boolean isFile() {
      return false;
    }
  };
  private static final InputProject PROJECT = new InputProject() {
    @Override
    public String key() {
      return "project";
    }

    @Override
    public boolean isFile() {
      return false;
    }
  };

  @Override
  public Configuration config() {
    return CONFIG;
  }

  @Override
  public boolean canSkipUnchangedFiles() {
    return false;
  }

  @Override
  public FileSystem fileSystem() {
    return FILE_SYSTEM;
  }

  @Override
  public Version getSonarQubeVersion() {
    return InternalSonarRuntime.VERSION_7_9;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public InputModule module() {
    return MODULE;
  }

  @Override
  public InputProject project() {
    return PROJECT;
  }

  @Override
  public SonarRuntime runtime() {
    return RUNTIME;
  }

  @Override
  public ActiveRules activeRules() {
    throw notSupportedException("activeRules()");
  }

  @Override
  public void addContextProperty(String arg0, String arg1) {
    throw notSupportedException("addContextProperty(String,String)");
  }

  @Override
  public void markForPublishing(InputFile arg0) {
    throw notSupportedException("markForPublishing(InputFile)");
  }

  @Override
  public void markAsUnchanged(InputFile inputFile) {
    throw notSupportedException("markAsUnchanged(InputFile)");
  }

  @Override
  public WriteCache nextCache() {
    return null;
  }

  @Override
  public ReadCache previousCache() {
    return null;
  }

  @Override
  public boolean isCacheEnabled() {
    return false;
  }

  @Override
  public NewAdHocRule newAdHocRule() {
    throw notSupportedException("newAdHocRule()");
  }

  @Override
  public NewAnalysisError newAnalysisError() {
    throw notSupportedException("newAnalysisError()");
  }

  @Override
  public NewCoverage newCoverage() {
    throw notSupportedException("newCoverage()");
  }

  @Override
  public NewCpdTokens newCpdTokens() {
    throw notSupportedException("newCpdTokens()");
  }

  @Override
  public NewExternalIssue newExternalIssue() {
    throw notSupportedException("newExternalIssue()");
  }

  @Override
  public NewHighlighting newHighlighting() {
    throw notSupportedException("newHighlighting()");
  }

  @Override
  public NewIssue newIssue() {
    throw notSupportedException("newIssue()");
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    throw notSupportedException("newMeasure()");
  }

  @Override
  public NewSignificantCode newSignificantCode() {
    throw notSupportedException("newSignificantCode()");
  }

  @Override
  public NewSymbolTable newSymbolTable() {
    throw notSupportedException("newSymbolTable()");
  }

  @Override
  public Settings settings() {
    throw notSupportedException("settings()");
  }

  @Override
  public void addTelemetryProperty(String s, String s1) {
    throw notSupportedException("addTelemetryProperty(String,String)");
  }

  @Override
  public void addAnalysisData(String s, String s1, InputStream inputStream) {
    throw notSupportedException("addAnalysisData(String,String,InputStream)");
  }
}
