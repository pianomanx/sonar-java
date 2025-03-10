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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2737")
public class CatchRethrowingCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tst = (TryStatementTree) tree;
    if (tst.catches().size() == 1) {
      CatchTree catchTree = tst.catches().get(0);
      if (onlyRethrows(catchTree)) {
        reportIssue(catchTree.block().body().get(0), "Add logic to this catch clause or eliminate it and rethrow the exception automatically.");
      }
    }
  }

  private static boolean onlyRethrows(CatchTree catchTree) {
    List<StatementTree> catchBody = catchTree.block().body();
    if (catchBody.size() == 1) {
      return catchBody.get(0).is(Tree.Kind.THROW_STATEMENT) && catchTree.parameter().symbol().usages().contains(((ThrowStatementTree) catchBody.get(0)).expression());
    }
    return false;
  }
}
