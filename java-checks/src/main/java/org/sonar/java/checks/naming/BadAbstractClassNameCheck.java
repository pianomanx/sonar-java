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
package org.sonar.java.checks.naming;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.regex.Pattern;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00118", repositoryKey = "squid")
@Rule(key = "S118")
public class BadAbstractClassNameCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String DEFAULT_FORMAT = "^Abstract[A-Z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the abstract class names against.",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (tree.is(Tree.Kind.CLASS) && simpleName != null) {
      if (pattern.matcher(simpleName.name()).matches()) {
        if (!isAbstract(tree)) {
          context.reportIssue(this, simpleName, "Make this class abstract or rename it, since it matches the regular expression '" + format + "'.");
        }
      } else {
        if (isAbstract(tree)) {
          context.reportIssue(this, simpleName, "Rename this abstract class name to match the regular expression '" + format + "'.");
        }
      }
    }
    super.visitClass(tree);
  }

  private static boolean isAbstract(ClassTree tree) {
    return ModifiersUtils.hasModifier(tree.modifiers(), Modifier.ABSTRACT);
  }

}
