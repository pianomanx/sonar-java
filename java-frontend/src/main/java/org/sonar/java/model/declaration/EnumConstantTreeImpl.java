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
package org.sonar.java.model.declaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class EnumConstantTreeImpl extends VariableTreeImpl implements EnumConstantTree {

  public EnumConstantTreeImpl(ModifiersTree modifiers, IdentifierTree simpleName, NewClassTreeImpl initializer,
    @Nullable InternalSyntaxToken separatorToken) {
    super(modifiers, simpleName, Objects.requireNonNull(initializer));
    if (separatorToken != null) {
      this.setEndToken(separatorToken);
    }
  }

  @Override
  @Nonnull
  public NewClassTree initializer() {
    return (NewClassTree) super.initializer();
  }

  @Override
  public Kind kind() {
    return Kind.ENUM_CONSTANT;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitEnumConstant(this);
  }

  @Override
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    // the identifierTree simpleName is also present in initializer
    list.add(modifiers());
    list.add(initializer());
    SyntaxToken endToken = endToken();
    if (endToken != null) {
      list.add(endToken);
    }
    return Collections.unmodifiableList(list);
  }

  @Nullable
  @Override
  public SyntaxToken separatorToken() {
    return endToken();
  }

}
