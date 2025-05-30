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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ClassTreeImpl extends JavaTree implements ClassTree {

  private final Kind kind;
  private final SyntaxToken openBraceToken;
  private final List<Tree> members;
  private final SyntaxToken closeBraceToken;
  private ModifiersTree modifiers;
  private SyntaxToken atToken;
  private SyntaxToken declarationKeyword;
  private IdentifierTree simpleName;
  private TypeParameters typeParameters;
  @Nullable
  private SyntaxToken recordOpenParenToken;
  private List<VariableTree> recordComponents = Collections.emptyList();
  @Nullable
  private SyntaxToken recordCloseParenToken;
  @Nullable
  private SyntaxToken extendsKeyword;
  @Nullable
  private TypeTree superClass;
  @Nullable
  private SyntaxToken implementsKeyword;
  private ListTree<TypeTree> superInterfaces;
  @Nullable
  private SyntaxToken permitsKeyword;
  private ListTree<TypeTree> permittedTypes;
  @Nullable
  public ITypeBinding typeBinding;

  public ClassTreeImpl(Kind kind, SyntaxToken openBraceToken, List<Tree> members, SyntaxToken closeBraceToken) {
    this.kind = kind;
    this.openBraceToken = openBraceToken;
    this.members = orderMembers(kind, members);
    this.closeBraceToken = closeBraceToken;
    this.modifiers = ModifiersTreeImpl.emptyModifiers();
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superInterfaces = QualifiedIdentifierListTreeImpl.emptyList();
    this.permittedTypes = QualifiedIdentifierListTreeImpl.emptyList();
  }

  public ClassTreeImpl complete(ModifiersTreeImpl modifiers, SyntaxToken declarationKeyword, IdentifierTree name) {
    this.modifiers = modifiers;
    this.declarationKeyword = declarationKeyword;
    this.simpleName = name;
    return this;
  }

  public ClassTreeImpl completeTypeParameters(TypeParameterListTreeImpl typeParameters) {
    this.typeParameters = typeParameters;
    return this;
  }

  public ClassTreeImpl completeSuperclass(SyntaxToken extendsKeyword, TypeTree superClass) {
    this.extendsKeyword = extendsKeyword;
    this.superClass = superClass;
    return this;
  }

  public ClassTreeImpl completeInterfaces(SyntaxToken keyword, QualifiedIdentifierListTreeImpl interfaces) {
    if (is(Kind.INTERFACE)) {
      extendsKeyword = keyword;
    } else {
      implementsKeyword = keyword;
    }
    this.superInterfaces = interfaces;
    return this;
  }

  public ClassTreeImpl completePermittedTypes(SyntaxToken permitsKeyword, QualifiedIdentifierListTreeImpl permittedTypes) {
    this.permitsKeyword = permitsKeyword;
    this.permittedTypes = permittedTypes;
    return this;
  }

  public ClassTreeImpl completeAtToken(InternalSyntaxToken atToken) {
    this.atToken = atToken;
    return this;
  }

  public ClassTreeImpl completeRecordComponents(List<VariableTree> recordComponents) {
    this.recordComponents = recordComponents;
    return this;
  }

  public ClassTreeImpl completeRecordComponents(InternalSyntaxToken openParenToken, List<VariableTree> recordComponents, InternalSyntaxToken closeParenToken) {
    this.recordOpenParenToken = openParenToken;
    this.recordCloseParenToken = closeParenToken;
    return completeRecordComponents(recordComponents);
  }

  private static List<Tree> orderMembers(Tree.Kind kind, List<Tree> members) {
    if (kind == Tree.Kind.RECORD && members.size() > 1) {
      // eclipse's records members are not properly ordered
      members.sort(Position.TREE_START_POSITION_COMPARATOR);
    }
    return members;
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Nullable
  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public TypeParameters typeParameters() {
    return typeParameters;
  }

  @Nullable
  @Override
  public SyntaxToken recordOpenParenToken() {
    return this.recordOpenParenToken;
  }

  @Override
  public List<VariableTree> recordComponents() {
    return recordComponents;
  }

  @Nullable
  @Override
  public SyntaxToken recordCloseParenToken() {
    return this.recordCloseParenToken;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Nullable
  @Override
  public TypeTree superClass() {
    return superClass;
  }

  @Override
  public ListTree<TypeTree> superInterfaces() {
    return superInterfaces;
  }

  @Override
  public SyntaxToken permitsKeyword() {
    return permitsKeyword;
  }

  @Override
  public ListTree<TypeTree> permittedTypes() {
    return permittedTypes;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<Tree> members() {
    return members;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return typeBinding != null
      ? root.sema.typeSymbol(typeBinding)
      : Symbol.TypeSymbol.UNKNOWN_TYPE;
  }

  @Nullable
  @Override
  public SyntaxToken declarationKeyword() {
    return declarationKeyword;
  }

  /**
   * Only used for annotation types, not part of API
   */
  @Nullable
  public SyntaxToken atToken() {
    return atToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitClass(this);
  }

  @Override
  public int getLine() {
    if (simpleName == null) {
      return super.getLine();
    }
    return ((IdentifierTreeImpl) simpleName).getLine();
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(modifiers),
      addIfNotNull(atToken),
      addIfNotNull(declarationKeyword),
      addIfNotNull(simpleName),
      Collections.singletonList(typeParameters),
      addIfNotNull(recordOpenParenToken),
      recordComponents,
      addIfNotNull(recordCloseParenToken),
      addIfNotNull(extendsKeyword),
      addIfNotNull(superClass),
      addIfNotNull(implementsKeyword),
      Collections.singletonList(superInterfaces),
      Collections.singletonList(permittedTypes),
      Collections.singletonList(openBraceToken),
      members,
      Collections.singletonList(closeBraceToken)
    );
  }

  private static Iterable<Tree> addIfNotNull(@Nullable Tree tree) {
    if (tree == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(tree);
  }

}
