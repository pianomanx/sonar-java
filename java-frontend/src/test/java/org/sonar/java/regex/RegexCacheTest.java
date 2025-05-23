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
package org.sonar.java.regex;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;

import static org.assertj.core.api.Assertions.assertThat;

class RegexCacheTest {

  @Test
  void same_result_if_same_tree_is_provided() {
    CompilationUnitTree cut = JParserTestUtils.parse("""
        class A {
          String s0 = "abc";
          String s1 = "abc";
        }
        """);
    ClassTree a = (ClassTree) cut.types().get(0);
    List<Tree> fields = a.members();
    LiteralTree s0 = (LiteralTree) ((VariableTree) fields.get(0)).initializer();
    LiteralTree s1 = (LiteralTree) ((VariableTree) fields.get(1)).initializer();

    RegexCache cache = new RegexCache();
    RegexParseResult resultForS0 = cache.getRegexForLiterals(new FlagSet(), s0);
    RegexParseResult resultForS1 = cache.getRegexForLiterals(new FlagSet(), s1);

    assertThat(s0.value()).isEqualTo(s1.value());
    assertThat(resultForS0)
      .isNotEqualTo(resultForS1)
      // same input, same result
      .isSameAs(cache.getRegexForLiterals(new FlagSet(), s0));

    assertThat(resultForS1).isSameAs(cache.getRegexForLiterals(new FlagSet(), s1));
  }

  @Test
  void same_result_if_same_trees_are_provided() {
    CompilationUnitTree cut = JParserTestUtils.parse("""
        class A {
          String s0 = "abc";
          String s1 = "abc";
        }
        """);
    ClassTree a = (ClassTree) cut.types().get(0);
    List<Tree> fields = a.members();
    LiteralTree s0 = (LiteralTree) ((VariableTree) fields.get(0)).initializer();
    LiteralTree s1 = (LiteralTree) ((VariableTree) fields.get(1)).initializer();

    RegexCache cache = new RegexCache();
    RegexParseResult resultForS0S1 = cache.getRegexForLiterals(new FlagSet(), s0, s1);
    RegexParseResult resultForS1S0 = cache.getRegexForLiterals(new FlagSet(), s1, s0);

    assertThat(s0.value() + s1.value()).isEqualTo(s1.value() + s0.value());
    assertThat(resultForS0S1)
      .isNotEqualTo(resultForS1S0)
      // same order of input, same result
      .isSameAs(cache.getRegexForLiterals(new FlagSet(), s0, s1));
    assertThat(resultForS1S0).isSameAs(cache.getRegexForLiterals(new FlagSet(), s1, s0));
  }

}
