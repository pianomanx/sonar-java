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
package org.sonar.java.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class JTypeSymbol extends JSymbol implements Symbol.TypeSymbol {

  /**
   * Cache for {@link #superClass()}.
   */
  private Type superclass = Type.UNKNOWN;

  /**
   * Cache for {@link #interfaces()}.
   */
  private List<Type> interfaces;

  /**
   * Cache for {@link #memberSymbols()}.
   */
  private Collection<Symbol> memberSymbols;

  /**
   * Cache for {@link #superTypes()}.
   */
  private Set<Type> superTypes;

  final SpecialField superSymbol = new SpecialField() {
    @Override
    public String name() {
      return "super";
    }

    @Override
    public Type type() {
      if (typeBinding().isInterface()) {
        // JLS § 15.12.1:
        // for "T.super.foo()", if T is an interface, 'super' keyword is used to access method of the interface itself
        return JTypeSymbol.this.type();
      }
      Type superClass = JTypeSymbol.this.superClass();
      return superClass == null ? Type.UNKNOWN : superClass;
    }
  };

  final SpecialField thisSymbol = new SpecialField() {
    @Override
    public String name() {
      return "this";
    }

    @Override
    public Type type() {
      return JTypeSymbol.this.type();
    }
  };

  JTypeSymbol(JSema sema, ITypeBinding typeBinding) {
    super(sema, typeBinding);
  }

  ITypeBinding typeBinding() {
    return (ITypeBinding) binding;
  }

  @CheckForNull
  @Override
  public Type superClass() {
    if (superclass == Type.UNKNOWN) {
      superclass = convertSuperClass();
    }
    return superclass;
  }

  @CheckForNull
  private Type convertSuperClass() {
    if (typeBinding().isInterface() || typeBinding().isArray()) {
      ITypeBinding objectBinding = sema.resolveType("java.lang.Object");
      return objectBinding != null ? sema.type(objectBinding) : Type.UNKNOWN;
    } else if (typeBinding().getSuperclass() == null) {
      // java.lang.Object
      return null;
    } else {
      return sema.type(typeBinding().getSuperclass());
    }
  }

  @Override
  public List<Type> interfaces() {
    if (interfaces == null) {
      interfaces = sema.types(typeBinding().getInterfaces());
    }
    return interfaces;
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    if (memberSymbols == null) {
      memberSymbols = convertMemberSymbols();
    }
    return memberSymbols;
  }

  private Collection<Symbol> convertMemberSymbols() {
    Collection<Symbol> members = new ArrayList<>();
    for (ITypeBinding b : typeBinding().getDeclaredTypes()) {
      members.add(sema.typeSymbol(b));
    }
    for (IVariableBinding b : typeBinding().getDeclaredFields()) {
      members.add(sema.variableSymbol(b));
    }
    for (IMethodBinding b : typeBinding().getDeclaredMethods()) {
      members.add(sema.methodSymbol(b));
    }
    return members;
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    return memberSymbols().stream()
      .filter(m -> name.equals(m.name()))
      .collect(Collectors.toSet());
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return (ClassTree) super.declaration();
  }

  @Override
  public Set<Type> superTypes() {
    if (superTypes == null) {
      if (isUnknown()) {
        superTypes = Collections.emptySet();
      } else {
        superTypes = new HashSet<>();
        JUtils.collectSuperTypes(superTypes, sema, typeBinding());
      }
    }
    return superTypes;
  }

  @Override
  @Nullable
  public TypeSymbol outermostClass() {
    Symbol symbol = this;
    Symbol result = null;
    while (symbol != null && !symbol.isPackageSymbol()) {
      result = symbol;
      symbol = symbol.owner();
    }
    return (Symbol.TypeSymbol) result;
  }

  @Override
  public boolean isAnnotation() {
    return !isUnknown() && typeBinding().isAnnotation();
  }

  abstract class SpecialField extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {
    @Override
    public final Symbol owner() {
      return JTypeSymbol.this;
    }

    @Override
    public final boolean isVariableSymbol() {
      return true;
    }

    @Override
    public final boolean isFinal() {
      return true;
    }

    @Override
    public final boolean isUnknown() {
      return false;
    }

    @Override
    public final TypeSymbol enclosingClass() {
      return JTypeSymbol.this;
    }

    @Override
    public final List<IdentifierTree> usages() {
      return Collections.emptyList();
    }

    @Nullable
    @Override
    public final VariableTree declaration() {
      return null;
    }

    @Override
    public final boolean isEffectivelyFinal(){
      return false;
    }
    @Override
    public final Optional<Object> constantValue() {
      return Optional.empty();
    }

    @Override
    public final boolean isLocalVariable() {
      return false;
    }

    @Override
    public final boolean isParameter() {
      return false;
    }
  }

}
