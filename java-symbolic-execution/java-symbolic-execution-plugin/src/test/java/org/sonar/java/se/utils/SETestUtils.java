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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.java.checks.verifier.FilesUtils;
import org.sonar.java.checks.verifier.internal.InternalInputFile;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.se.Pair;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.VariableSymbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SETestUtils {

  public static final List<File> CLASS_PATH = TestClasspathUtils.loadFromFile(FilesUtils.DEFAULT_TEST_CLASSPATH_FILE);
  static {
    CLASS_PATH.add(new File("target/test-classes"));
    Optional.of(new File(FilesUtils.DEFAULT_TEST_CLASSES_DIRECTORY)).filter(File::exists).ifPresent(CLASS_PATH::add);
  }

  public static SymbolicExecutionVisitor createSymbolicExecutionVisitor(String fileName, SECheck... checks) {
    return createSymbolicExecutionVisitorAndSemantic(fileName, checks).a;
  }

  public static Pair<SymbolicExecutionVisitor, Sema> createSymbolicExecutionVisitorAndSemantic(String fileName, SECheck... checks) {
    InputFile inputFile = InternalInputFile.inputFile("", new File(fileName));
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(inputFile.file(), CLASS_PATH);
    Sema semanticModel = cut.sema;
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Arrays.asList(checks));
    sev.scanFile(new DefaultJavaFileScannerContext(cut, inputFile, semanticModel, null, new JavaVersionImpl(8), true, false));
    return new Pair<>(sev, semanticModel);
  }

  public static Sema getSemanticModel(String filename) {
    File file = new File(filename);
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(file, CLASS_PATH);
    return cut.sema;
  }

  public static MethodBehavior getMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    Optional<MethodBehavior> mb = sev.behaviorCache.behaviors.entrySet().stream()
      .filter(e -> e.getKey().contains("#" + methodName))
      .map(Map.Entry::getValue)
      .findFirst();
    assertThat(mb).isPresent();
    return mb.get();
  }

  public static MethodBehavior mockMethodBehavior(int arity, boolean varArgs) {
    return new MethodBehaviorStub(arity, varArgs);
  }

  public static MethodBehavior mockMethodBehavior() {
    return mockMethodBehavior(0, false);
  }

  public static InputFile inputFile(String filename) {
    return inputFile(new File(filename));
  }

  public static InputFile inputFile(File file) {
    try {
      return new TestInputFileBuilder("", file.getPath())
        .setContents(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
        .setCharset(StandardCharsets.UTF_8)
        .setLanguage("java")
        .build();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to read file '%s", file.getAbsolutePath()));
    }
  }

  private static class MethodBehaviorStub extends MethodBehavior {

    private final int arity;

    public MethodBehaviorStub(int arity, boolean varArgs) {
      super("()", varArgs);
      this.arity = arity;
    }

    @Override
    public int methodArity() {
      return arity;
    }
  }

  public static Symbol.VariableSymbol variable(String name) {
    VariableSymbol variable = mock(Symbol.VariableSymbol.class);
    when(variable.name()).thenReturn(name);
    when(variable.toString()).thenReturn("A#" + name);
    // return new JavaSymbol.VariableJavaSymbol(0, name, new JavaSymbol(JavaSymbol.TYP, 0, "A", Symbols.unknownSymbol));
    return variable;
  }
}
