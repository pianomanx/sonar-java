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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Class, enum, interface or annotation declaration.
 * <p>
 * JLS 8.1. Class declaration ({@link Tree.Kind#CLASS}):
 * <pre>
 *   {@link #modifiers()} class {@link #simpleName()} {@link #typeParameters()} extends {@link #superClass()} implements {@link #superInterfaces()} {
 *     {@link #members()}
 *   }
 * </pre>
 * JLS 8.9. Enum declaration ({@link Tree.Kind#ENUM}):
 * <pre>
 *   {@link #modifiers()} enum {@link #simpleName()} {@link #typeParameters()} implements {@link #superInterfaces()} {
 *     {@link #members()}
 *   }
 * </pre>
 * JLS 8.10 Record declaration ({@link Tree.Kind#RECORD}):
 * <pre>
 *   {@link #modifiers()} record {@link #simpleName()} {@link #typeParameters()} ({@link #recordComponents()}) implements {@link #superInterfaces()} {
 *     {@link #members()}
 *   }
 * </pre>
 * JLS 9.1. Interface declaration ({@link Tree.Kind#INTERFACE}):
 * <pre>
 *   {@link #modifiers()} interface {@link #simpleName()} {@link #typeParameters()} extends {@link #superInterfaces()} {
 *     {@link #members()}
 *   }
 * </pre>
 * JLS 9.6. Annotation declaration ({@link Tree.Kind#ANNOTATION_TYPE}):
 * <pre>
 *   {@link #modifiers()}{@code @}interface {@link #simpleName()} {
 *     {@link #members()}
 *   }
 * </pre>
 * </p>
 *
 * @since Java 1.3
 */
@Beta
public interface ClassTree extends StatementTree {

  @Nullable
  SyntaxToken declarationKeyword();

  @Nullable
  IdentifierTree simpleName();

  TypeParameters typeParameters();

  /**
   * @since Java 16
   */
  @Nullable
  SyntaxToken recordOpenParenToken();

  List<VariableTree> recordComponents();

  @Nullable
  SyntaxToken recordCloseParenToken();

  ModifiersTree modifiers();

  @Nullable
  TypeTree superClass();

  ListTree<TypeTree> superInterfaces();

  /**
   * @since Java 15 (preview), Java 17 (final)
   */
  @Nullable
  SyntaxToken permitsKeyword();

  /**
   * @since Java 15 (preview), Java 17 (final)
   */
  ListTree<TypeTree> permittedTypes();

  SyntaxToken openBraceToken();

  List<Tree> members();

  SyntaxToken closeBraceToken();

  Symbol.TypeSymbol symbol();
}
