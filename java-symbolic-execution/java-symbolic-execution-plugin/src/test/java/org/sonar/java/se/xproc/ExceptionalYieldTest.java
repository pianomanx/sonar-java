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
package org.sonar.java.se.xproc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JavaTree;
import org.sonar.java.se.Pair;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.plugins.java.api.semantic.Sema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.se.utils.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.utils.SETestUtils.createSymbolicExecutionVisitorAndSemantic;
import static org.sonar.java.se.utils.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.utils.SETestUtils.mockMethodBehavior;

class ExceptionalYieldTest {

  @Test
  void test_equals() {
    MethodBehavior methodBehavior = mockMethodBehavior();

    ExceptionalYield methodYield = new ExceptionalYield(methodBehavior);
    ExceptionalYield otherYield = new ExceptionalYield(methodBehavior);

    assertThat(methodYield)
      .isNotEqualTo(null)
      .isEqualTo(methodYield)
      .isEqualTo(otherYield);

    otherYield.setExceptionType("java.lang.Exception");
    assertThat(methodYield).isNotEqualTo(otherYield);

    methodYield.setExceptionType("java.lang.Exception");
    assertThat(methodYield).isEqualTo(otherYield);

    Sema semanticModel = ((JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("class A{}")).sema;
    assertThat(methodYield.exceptionType(semanticModel)).isEqualTo(otherYield.exceptionType(semanticModel));

    // same arity and parameters but happy yield
    assertThat(methodYield).isNotEqualTo(new HappyPathYield(methodBehavior));
  }

  @Test
  void test_hashCode() {
    MethodBehavior methodBehavior = mockMethodBehavior();

    ExceptionalYield methodYield = new ExceptionalYield(methodBehavior);
    ExceptionalYield other = new ExceptionalYield(methodBehavior);

    // same values for same yields
    assertThat(methodYield).hasSameHashCodeAs(other);

    // different values for different yields
    other.setExceptionType("java.lang.Exception");
    assertThat(methodYield.hashCode()).isNotEqualTo(other.hashCode());

    // happy Path Yield method yield
    assertThat(methodYield.hashCode()).isNotEqualTo(new HappyPathYield(methodBehavior).hashCode());
  }

  @Test
  void exceptional_yields() {
    Pair<SymbolicExecutionVisitor, Sema> sevAndSemantic = 
      createSymbolicExecutionVisitorAndSemantic("src/test/files/se/ExceptionalYields.java", new NullDereferenceCheck());
    SymbolicExecutionVisitor sev = sevAndSemantic.a;
    Sema semanticModel = sevAndSemantic.b;

    MethodBehavior mb = getMethodBehavior(sev, "myMethod");
    assertThat(mb.yields()).hasSize(4);

    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().toList();
    assertThat(exceptionalYields).hasSize(3);

    // runtime exception
    ExceptionalYield runtime = exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).isUnknown()).findFirst().get();
    assertThat(paramBooleanConstraint(runtime)).isEqualTo(BooleanConstraint.FALSE);

    // exception from other method call
    ExceptionalYield implicit = exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).is("org.foo.MyException2")).findFirst().get();
    assertThat(paramBooleanConstraint(implicit)).isEqualTo(BooleanConstraint.FALSE);

    // explicitly thrown exception
    ExceptionalYield explicit = exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).is("org.foo.MyException1")).findFirst().get();
    assertThat(paramBooleanConstraint(explicit)).isEqualTo(BooleanConstraint.TRUE);
  }

  @Test
  void test_toString() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/ExceptionalYields.java", new NullDereferenceCheck());
    Set<String> yieldsToString = getMethodBehavior(sev, "myMethod").exceptionalPathYields().map(MethodYield::toString).collect(Collectors.toSet());
    assertThat(yieldsToString).contains(
      "{params: [[FALSE]], exceptional}",
      "{params: [[TRUE]], exceptional (org.foo.MyException1)}",
      "{params: [[FALSE]], exceptional (org.foo.MyException2)}");
  }

  @Test
  void exceptional_yields_void_method() {
    Pair<SymbolicExecutionVisitor, Sema> sevAndSemantic = 
      createSymbolicExecutionVisitorAndSemantic("src/test/files/se/ExceptionalYieldsVoidMethod.java", new NullDereferenceCheck());
    SymbolicExecutionVisitor sev = sevAndSemantic.a;
    Sema semanticModel = sevAndSemantic.b;
    MethodBehavior mb = getMethodBehavior(sev, "myVoidMethod");
    assertThat(mb.yields()).hasSize(4);

    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().toList();
    assertThat(exceptionalYields).hasSize(3);
    assertThat(exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).isUnknown()).count()).isEqualTo(1);

    ExceptionalYield explicit = exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).is("org.foo.MyException1")).findAny().get();
    assertThat(paramObjectConstraint(explicit)).isEqualTo(ObjectConstraint.NULL);

    ExceptionalYield implicit = exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).is("org.foo.MyException2")).findAny().get();
    assertThat(paramObjectConstraint(implicit)).isEqualTo(ObjectConstraint.NOT_NULL);
  }

  @Test
  void exceptional_yields_anonymous_exception() {
    Pair<SymbolicExecutionVisitor, Sema> sevAndSemantic = createSymbolicExecutionVisitorAndSemantic(
      mainCodeSourcesPath("symbolicexecution/behaviorcache/AnonymousExceptionalYield.java"),
      new NullDereferenceCheck());

    Optional<ExceptionalYield> runtimeYield = getMethodBehavior(sevAndSemantic.a, "foo")
      .exceptionalPathYields()
      .filter(y -> y.exceptionType(sevAndSemantic.b).is("java.lang.RuntimeException"))
      .findAny();
    assertThat(runtimeYield)
      .map(ExceptionalYieldTest::paramObjectConstraint)
      .contains(ObjectConstraint.NOT_NULL);
  }

  private static Constraint paramObjectConstraint(MethodYield methodYield) {
    return methodYield.parametersConstraints.get(0).get(ObjectConstraint.class);
  }

  private static Constraint paramBooleanConstraint(MethodYield methodYield) {
    return methodYield.parametersConstraints.get(0).get(BooleanConstraint.class);
  }
}
