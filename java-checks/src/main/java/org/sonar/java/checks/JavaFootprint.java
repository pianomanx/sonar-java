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
package org.sonar.java.checks;

import java.util.HashSet;
import java.util.Set;
import org.sonarsource.analyzer.commons.recognizers.CamelCaseDetector;
import org.sonarsource.analyzer.commons.recognizers.ContainsDetector;
import org.sonarsource.analyzer.commons.recognizers.Detector;
import org.sonarsource.analyzer.commons.recognizers.EndWithDetector;
import org.sonarsource.analyzer.commons.recognizers.KeywordsDetector;
import org.sonarsource.analyzer.commons.recognizers.LanguageFootprint;

public final class JavaFootprint implements LanguageFootprint {

  private final Set<Detector> detectors = new HashSet<>();

  public JavaFootprint() {
    detectors.add(new EndWithDetector(0.95, '}', ';', '{'));
    detectors.add(new KeywordsDetector(0.7, "++", "||", "&&"));
    detectors.add(new KeywordsDetector(0.3, "public", "abstract", "class", "implements", "extends", "return", "throw",
        "private", "protected", "enum", "continue", "assert", "package", "synchronized", "boolean", "this", "double", "instanceof",
        "final", "interface", "static", "void", "long", "int", "float", "super", "true", "case:"));
    detectors.add(new ContainsDetector(0.95, "for(", "if(", "while(", "catch(", "switch(", "try{", "else{"));
    detectors.add(new CamelCaseDetector(0.5));
  }

  @Override
  public Set<Detector> getDetectors() {
    return detectors;
  }

}
