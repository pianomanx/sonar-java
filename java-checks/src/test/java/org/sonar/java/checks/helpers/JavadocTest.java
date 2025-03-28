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
package org.sonar.java.checks.helpers;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class JavadocTest {
  private static CompilationUnitTree tree;
  private static Javadoc fooJavadoc;
  private static Javadoc barJavadoc;
  private static List<Javadoc> emptyJavadocs;
  private static Javadoc emptyDescriptionJavadoc;
  private static Javadoc fullParamsDescriptionJavadoc;
  private static Javadoc genericExceptionThrownJavadoc;
  private static Javadoc genericExceptionThrownUndocumented;
  private static Javadoc invalidThrownExceptionUndocumented;

  @BeforeAll
  static void setup() {
    File file = new File("src/test/files/checks/helpers/JavadocTest.java");
    tree = JParserTestUtils.parse(file);

    Map<String, MethodTree> methods = ((ClassTree) tree.types().get(0)).members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .collect(Collectors.toMap(methodTree -> methodTree.simpleName().name(), Function.identity()));

    fooJavadoc = new Javadoc(methods.get("foo"));
    barJavadoc = new Javadoc(methods.get("bar"));
    emptyDescriptionJavadoc = new Javadoc(methods.get("emptyDescription"));
    fullParamsDescriptionJavadoc = new Javadoc(methods.get("fullParamsDescription"));
    genericExceptionThrownJavadoc = new Javadoc(methods.get("genericExceptionThrown"));
    genericExceptionThrownUndocumented = new Javadoc(methods.get("genericExceptionThrownUndocumented"));
    invalidThrownExceptionUndocumented = new Javadoc(methods.get("invalidThrownExceptionUndocumented"));

    emptyJavadocs = methods.keySet().stream()
      .filter(name -> name.startsWith("emptyJavadoc"))
      .map(methods::get)
      .map(Javadoc::new)
      .toList();
  }

  @Test
  void test_no_main_description() {
    assertThat(fooJavadoc.noMainDescription()).isFalse();
    assertThat(barJavadoc.noMainDescription()).isTrue();
    assertThat(emptyDescriptionJavadoc.noMainDescription()).isTrue();
    assertThat(fullParamsDescriptionJavadoc.noMainDescription()).isTrue();
    assertThat(genericExceptionThrownJavadoc.noMainDescription()).isTrue();
    assertThat(genericExceptionThrownUndocumented.noMainDescription()).isTrue();
    assertThat(emptyJavadocs.stream().map(Javadoc::noMainDescription)).hasSize(6).allMatch(Boolean.TRUE::equals);
  }

  @Test
  void test_no_return_description() {
    assertThat(fooJavadoc.noReturnDescription()).isFalse();
    assertThat(barJavadoc.noReturnDescription()).isFalse();
    assertThat(emptyDescriptionJavadoc.noReturnDescription()).isTrue();
    assertThat(fullParamsDescriptionJavadoc.noReturnDescription()).isTrue();
    assertThat(genericExceptionThrownJavadoc.noReturnDescription()).isTrue();
    assertThat(genericExceptionThrownUndocumented.noReturnDescription()).isTrue();
    assertThat(emptyJavadocs.stream().map(Javadoc::noReturnDescription)).hasSize(6).allMatch(Boolean.TRUE::equals);
  }

  @Test
  void test_undocumented_parameters() {
    assertThat(fooJavadoc.undocumentedParameters()).containsExactlyInAnyOrder("c", "e");
    assertThat(barJavadoc.undocumentedParameters()).containsExactlyInAnyOrder("a");
    assertThat(emptyDescriptionJavadoc.undocumentedParameters()).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(fullParamsDescriptionJavadoc.undocumentedParameters()).isEmpty();
    assertThat(genericExceptionThrownJavadoc.undocumentedParameters()).isEmpty();
    assertThat(genericExceptionThrownUndocumented.undocumentedParameters()).isEmpty();
    assertThat(emptyJavadocs.stream().map(Javadoc::undocumentedParameters)).hasSize(6).allMatch(p -> p.size() == 1 && "a".equals(p.iterator().next()));
  }

  @Test
  void test_undocumented_thrown_exceptions() {
    assertThat(fooJavadoc.undocumentedThrownExceptions()).containsExactlyInAnyOrder("IOException", "IllegalStateException", "B");
    assertThat(barJavadoc.undocumentedThrownExceptions()).isEmpty();
    assertThat(emptyDescriptionJavadoc.undocumentedThrownExceptions()).containsExactlyInAnyOrder("NullPointerException");
    assertThat(fullParamsDescriptionJavadoc.undocumentedThrownExceptions()).isEmpty();
    assertThat(genericExceptionThrownJavadoc.undocumentedThrownExceptions()).containsExactlyInAnyOrder("ObjectStreamException", "InvalidObjectException");
    assertThat(genericExceptionThrownUndocumented.undocumentedThrownExceptions()).containsExactlyInAnyOrder("Exception");
    assertThat(invalidThrownExceptionUndocumented.undocumentedThrownExceptions()).isEmpty();
    assertThat(emptyJavadocs.stream().map(Javadoc::undocumentedThrownExceptions)).hasSize(6).allMatch(Collection::isEmpty);
  }

  @Test
  void test_no_exception_on_invalid_type() {
    Javadoc invalidJavadoc = new Javadoc(tree);
    assertThat(invalidJavadoc.undocumentedParameters()).isEmpty();
    assertThat(invalidJavadoc.undocumentedThrownExceptions()).isEmpty();
    assertThat(invalidJavadoc.getBlockTagDescriptions()).isEmpty();
    assertThat(invalidJavadoc.getMainDescription()).isEmpty();
  }

  @Test
  void test_class_javadoc() {
    Javadoc classJavadoc = new Javadoc(tree.types().get(0));
    assertThat(classJavadoc.noMainDescription()).isFalse();
    assertThat(classJavadoc.noReturnDescription()).isTrue();
    assertThat(classJavadoc.undocumentedParameters()).containsExactlyInAnyOrder("<C>", "<E>");
    assertThat(classJavadoc.undocumentedThrownExceptions()).isEmpty();

    Javadoc classNoJavadoc = new Javadoc(tree.types().get(1));
    assertThat(classNoJavadoc.noMainDescription()).isTrue();
    assertThat(classNoJavadoc.noReturnDescription()).isTrue();
    assertThat(classNoJavadoc.undocumentedParameters()).isEmpty();
    assertThat(classNoJavadoc.undocumentedThrownExceptions()).isEmpty();
  }
}
