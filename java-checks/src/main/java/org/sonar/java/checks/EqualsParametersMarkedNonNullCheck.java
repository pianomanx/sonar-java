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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S4454")
public class EqualsParametersMarkedNonNullCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!MethodTreeUtils.isEqualsMethod(methodTree)) {
      return;
    }
    VariableTree variable = methodTree.parameters().get(0);
    SymbolMetadata.NullabilityData nullabilityData = variable.symbol().metadata().nullabilityData();
    AnnotationInstance annotation = nullabilityData.annotation();
    Tree annotationTree = nullabilityData.declaration();
    if (annotationTree != null && annotation != null && nullabilityData.isNonNull(NullabilityLevel.VARIABLE, true, false)) {
      String annotationName = annotation.symbol().name();
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(annotationTree)
        .withMessage("\"equals\" method parameters should not be marked \"@%s\".", annotationName)
        .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove \"@%s\"", annotationName)
          .addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(annotationTree, true,
            QuickFixHelper.nextToken(annotationTree), false)))
          .build())
        .report();
    }
  }

}
