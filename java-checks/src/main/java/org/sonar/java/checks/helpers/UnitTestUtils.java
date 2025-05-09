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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.util.Arrays.asList;

public final class UnitTestUtils {

  private static final String ORG_JUNIT_TEST = "org.junit.Test";
  public static final Pattern ASSERTION_METHODS_PATTERN = Pattern.compile(
    "(assert|verify|fail|should|check|expect|validate|andExpect).*" +
    // Eclipse Vert.x with JUnit 5 (VertxTestContext)
      "|laxCheckpoint|succeedingThenComplete");
  private static final Pattern TEST_METHODS_PATTERN = Pattern.compile("test.*|.*Test");

  @VisibleForTesting
  static final Predicate<String> ASSERTJ_ASSERTION_METHODS_PREDICATE = Pattern.compile(
    "(allMatch|assert|contains|doesNot|has|is|returns|satisfies)([A-Z].*)?").asMatchPredicate();

  private static final Pattern ASSERTJ_ASSERTION_CLASSNAME_PATTERN = Pattern.compile("org\\.assertj\\.core\\.api\\.[a-zA-Z]+Assert");
  private static final Predicate<Type> ASSERTJ_ASSERTION_TYPE_PREDICATE = type -> ASSERTJ_ASSERTION_CLASSNAME_PATTERN.matcher(type.fullyQualifiedName()).matches()
    || type.isSubtypeOf("org.assertj.core.api.AbstractAssert");

  public static final MethodMatchers ASSERTION_INVOCATION_MATCHERS = MethodMatchers.or(
    // fest 1.x / 2.X
    MethodMatchers.create().ofSubTypes("org.fest.assertions.GenericAssert", "org.fest.assertions.api.AbstractAssert").anyName().withAnyParameters().build(),
    // rest assured 2.x, 3.x, 4.x
    MethodMatchers.create().ofTypes(
        "com.jayway.restassured.response.ValidatableResponseOptions", // restassured 2.x
        "io.restassured.response.ValidatableResponseOptions", // restassured 3.x and 4.x
        "io.restassured.module.mockmvc.response.ValidatableMockMvcResponse" // spring mock mvc extending the io.restassured library
      )
      .name(name -> "body".equals(name) ||
        "time".equals(name) ||
        name.startsWith("time") ||
        name.startsWith("content") ||
        name.startsWith("status") ||
        name.startsWith("header") ||
        name.startsWith("cookie") ||
        name.startsWith("spec"))
      .withAnyParameters()
      .build(),
    // assertJ
    MethodMatchers.create().ofType(ASSERTJ_ASSERTION_TYPE_PREDICATE)
      .name(ASSERTJ_ASSERTION_METHODS_PREDICATE).withAnyParameters().build(),
    // spring
    MethodMatchers.create().ofTypes("org.springframework.test.web.servlet.ResultActions").names("andExpect", "andExpectAll").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("org.springframework.util.Assert").anyName().withAnyParameters().build(),
    // JMockit
    MethodMatchers.create().ofTypes("mockit.Verifications").constructor().withAnyParameters().build(),
    // Eclipse Vert.x with JUnit 4
    MethodMatchers.create().ofTypes("io.vertx.ext.unit.TestContext").name(name -> name.startsWith("asyncAssert")).addWithoutParametersMatcher().build(),
    // Awaitility
    MethodMatchers.create().ofTypes("org.awaitility.core.ConditionFactory").name(name -> name.startsWith("until")).withAnyParameters().build());

  public static final MethodMatchers REACTIVE_X_TEST_METHODS = MethodMatchers.create().ofSubTypes("rx.Observable", "io.reactivex.Observable").names("test").withAnyParameters()
    .build();

  private static final MethodMatchers VERTX_TEST_CONTEXT_METHODS = MethodMatchers.create().ofTypes("io.vertx.junit5.VertxTestContext").anyName().withAnyParameters().build();

  public static final MethodMatchers FAIL_METHOD_MATCHER = MethodMatchers.or(
    MethodMatchers.create().ofTypes(
      // JUnit 5
      "org.junit.jupiter.api.Assertions",
      // JUnit 4
      "org.junit.Assert",
      // JUnit 3
      "junit.framework.Assert",
      // Fest assert
      "org.fest.assertions.Fail",
      // AssertJ
      "org.assertj.core.api.Fail",
      "org.assertj.core.api.Assertions")
      .names("fail").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(
      // AssertJ
      "org.assertj.core.api.Assertions")
      .names("failBecauseExceptionWasNotThrown").withAnyParameters().build());

  public static final MethodMatchers ASSERTIONS_METHOD_MATCHER = MethodMatchers.or(
    // JUnit 3, 4 and 5
    MethodMatchers.create()
      .ofTypes("org.junit.Assert", "org.junit.jupiter.api.Assertions", "junit.framework.Assert", "junit.framework.TestCase")
      .name(name -> name.startsWith("assert"))
      .withAnyParameters()
      .build(),
    // Fest assert and AssertJ
    MethodMatchers.create()
      .ofTypes("org.assertj.core.api.Assertions", "org.fest.assertions.Assertions")
      .names("assertThat")
      .withAnyParameters()
      .build());

  /**
   * Match when we are sure that the intention is to assert something, that will result in an AssertionError if the assertion fails.
   * The purpose is not to detect any assertion method (similar to S2699).
   */
  public static final MethodMatchers COMMON_ASSERTION_MATCHER = MethodMatchers.or(
    FAIL_METHOD_MATCHER, ASSERTIONS_METHOD_MATCHER);

  private static final Set<String> TEST_ANNOTATIONS = new HashSet<>(asList(ORG_JUNIT_TEST, "org.testng.annotations.Test"));
  private static final Set<String> JUNIT5_TEST_ANNOTATIONS = Set.of(
    "org.junit.jupiter.api.Test",
    "org.junit.jupiter.api.RepeatedTest",
    "org.junit.jupiter.api.TestFactory",
    "org.junit.jupiter.api.TestTemplate",
    "org.junit.jupiter.params.ParameterizedTest");

  private static final Set<String> JUNIT5_INSTANCE_LIFECYCLE_ANNOTATIONS = Set.of(
    "org.junit.jupiter.api.BeforeEach",
    "org.junit.jupiter.api.AfterEach");

  private static final Set<String> JUNIT5_CLASS_LIFECYCLE_ANNOTATIONS = Set.of(
    "org.junit.jupiter.api.BeforeAll",
    "org.junit.jupiter.api.AfterAll");

  private static final String NESTED_ANNOTATION = "org.junit.jupiter.api.Nested";

  private static final Pattern UNIT_TEST_NAME_RELATED_TO_OBJECT_METHODS_REGEX = Pattern.compile("equal|hash_?code|object_?method|to_?string", Pattern.CASE_INSENSITIVE);

  private UnitTestUtils() {
  }

  public static boolean hasNestedAnnotation(ClassTree tree) {
    SymbolMetadata metadata = tree.symbol().metadata();
    return metadata.isAnnotatedWith(NESTED_ANNOTATION);
  }

  public static boolean hasTestAnnotation(MethodTree tree) {
    SymbolMetadata symbolMetadata = tree.symbol().metadata();
    return TEST_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith) || hasJUnit5TestAnnotation(symbolMetadata);
  }

  public static boolean hasJUnit5TestAnnotation(MethodTree tree) {
    return hasJUnit5TestAnnotation(tree.symbol().metadata());
  }

  private static boolean hasJUnit5TestAnnotation(SymbolMetadata symbolMetadata) {
    return JUNIT5_TEST_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith);
  }

  public static boolean hasJUnit5InstanceLifecycleAnnotation(MethodTree tree) {
    SymbolMetadata symbolMetadata = tree.symbol().metadata();
    return JUNIT5_INSTANCE_LIFECYCLE_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith);
  }


  public static boolean hasJUnit5ClassLifecycleAnnotation(MethodTree tree) {
    SymbolMetadata symbolMetadata = tree.symbol().metadata();
    return JUNIT5_CLASS_LIFECYCLE_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith);
  }

  public static boolean isInUnitTestRelatedToObjectMethods(ExpressionTree expr) {
    return isUnitTestRelatedToObjectMethods(ExpressionUtils.getEnclosingMethod(expr));
  }

  public static boolean isUnitTestRelatedToObjectMethods(@Nullable MethodTree method) {
    return method != null && UNIT_TEST_NAME_RELATED_TO_OBJECT_METHODS_REGEX.matcher(method.simpleName().name()).find();
  }

  public static boolean isUnitTest(MethodTree methodTree) {
    if (isOrOverridesJunit4TestMethod(methodTree)) {
      return true;
    }

    if (hasJUnit5TestAnnotation(methodTree)) {
      // contrary to JUnit 4, JUnit 5 Test annotations are not inherited when method is overridden, so no need to check overridden symbols
      return true;
    }
    Symbol.TypeSymbol enclosingClass = Objects.requireNonNull(methodTree.symbol().enclosingClass(), "Must not be null for method symbols");
    return enclosingClass.type().isSubtypeOf("junit.framework.TestCase") && methodTree.simpleName().name().startsWith("test");
  }

  private static boolean isOrOverridesJunit4TestMethod(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    return symbol.metadata().isAnnotatedWith(ORG_JUNIT_TEST)
      // JUnit 4 considers as test any method overriding another annotated with @Test
      || symbol.overriddenSymbols().stream()
        .map(Symbol::metadata)
        .anyMatch(meta -> meta.isAnnotatedWith(ORG_JUNIT_TEST));
  }

  public static boolean isTestClass(ClassTree classTree) {
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    return !classSymbol.isAbstract()
      && isTopLevelClass(classSymbol)
      && (hasTestMethod(classTree.members()) || hasNestedClass(classTree));
  }

  private static boolean isTopLevelClass(Symbol.TypeSymbol classSymbol) {
    return classSymbol.owner().isPackageSymbol();
  }

  private static boolean hasTestMethod(List<Tree> members) {
    return members.stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .anyMatch(UnitTestUtils::hasTestAnnotation);
  }

  private static boolean hasNestedClass(ClassTree classTree) {
    return classTree.members()
      .stream()
      .filter(member -> member.is(Tree.Kind.CLASS))
      .map(ClassTree.class::cast)
      .anyMatch(UnitTestUtils::hasNestedAnnotation);
  }

  public static boolean methodNameMatchesAssertionMethodPattern(String methodName, Symbol methodSymbol) {
    if (TEST_METHODS_PATTERN.matcher(methodName).matches()) {
      return !REACTIVE_X_TEST_METHODS.matches(methodSymbol);
    }
    if ("verify".equals(methodName) || "failing".equals(methodName)) {
      return !VERTX_TEST_CONTEXT_METHODS.matches(methodSymbol);
    }

    return ASSERTION_METHODS_PATTERN.matcher(methodName).matches();
  }
}
