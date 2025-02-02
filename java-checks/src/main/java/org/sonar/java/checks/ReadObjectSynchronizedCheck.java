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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2675")
public class ReadObjectSynchronizedCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (implementsSerializable(classTree)) {
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.METHOD)) {
          checkMember((MethodTree) member);
        }
      }
    }
  }

  private void checkMember(MethodTree member) {
    if (isReadObject(member)) {
      ModifierKeywordTree modifier = ModifiersUtils.getModifier(member.modifiers(), Modifier.SYNCHRONIZED);
      if (modifier != null) {
        reportIssue(modifier.keyword(), "Remove the \"synchronized\" keyword from this method.");
      }
    }
  }

  private static boolean implementsSerializable(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("java.io.Serializable");
  }

  private static boolean isReadObject(MethodTree methodTree) {
    return "readObject".equals(methodTree.simpleName().name())
      && methodTree.parameters().size() == 1
      && methodTree.parameters().get(0).type().symbolType().is("java.io.ObjectInputStream");
  }

}
