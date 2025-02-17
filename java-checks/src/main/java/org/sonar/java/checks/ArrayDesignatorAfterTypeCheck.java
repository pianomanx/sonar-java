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
import org.sonar.java.checks.ArrayDesignatorOnVariableCheck.MisplacedArray;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.ArrayDesignatorOnVariableCheck.createQuickFix;

@Rule(key = "S1195")
public class ArrayDesignatorAfterTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    MisplacedArray.find(methodTree.returnType(), methodTree.simpleName().identifierToken())
      .ifPresent(misplaced -> QuickFixHelper.newIssue(context)
        .forRule(this)
        .onRange(misplaced.firstArray.openBracketToken(), misplaced.lastArray.closeBracketToken())
        .withMessage("Move the array designators " + misplaced.replacement + " to the end of the return type.")
        .withQuickFix(() -> createQuickFix(misplaced, "return type"))
        .report());
  }

}
