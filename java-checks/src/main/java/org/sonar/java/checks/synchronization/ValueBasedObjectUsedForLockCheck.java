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
package org.sonar.java.checks.synchronization;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3436")
public class ValueBasedObjectUsedForLockCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final List<String> VALUE_BASED_TYPES = Arrays.asList(
    "java.time.chrono.HijrahDate",
    "java.time.chrono.JapaneseDate",
    "java.time.chrono.MinguoDate",
    "java.time.chrono.ThaiBuddhistDate",
    "java.util.Optional",
    "java.util.OptionalDouble",
    "java.util.OptionalLong",
    "java.util.OptionalInt");

  private static final String JAVA_TIME_CLOCK = "java.time.Clock";
  private static final Pattern JAVA_TIME_PACKAGE_PATTERN = Pattern.compile("java\\.time\\.\\w+");

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expression = ((SynchronizedStatementTree) tree).expression();
    Type expressionType = expression.symbolType();
    if (isValueBasedType(expressionType)) {
      reportIssue(expression, String.format("Synchronize on a non-value-based object; synchronizing on a \"%s\" could lead to contention.%s",
        expressionType.name(),
        context.getJavaVersion().java8CompatibilityMessage()));
    }
  }

  private static boolean isValueBasedType(Type type) {
    if (type.isUnknown() || type.is(JAVA_TIME_CLOCK)) {
      return false;
    }
    return VALUE_BASED_TYPES.stream().anyMatch(type::is)
      || JAVA_TIME_PACKAGE_PATTERN.matcher(type.fullyQualifiedName()).matches();
  }

}
