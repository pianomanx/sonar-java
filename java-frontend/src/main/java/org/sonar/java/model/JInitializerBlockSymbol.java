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

import java.util.Collections;
import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;

/**
 * Symbol type to represent initializer blocks. It's a type of method symbol and was introduced, so that variables
 * defined inside an initializer block can have an owner other than the containing class. This is necessary to properly
 * detect them as local variables.
 */
final class JInitializerBlockSymbol implements Symbol.MethodSymbol {
  private final TypeSymbol owner;
  private final boolean isStatic;

  public JInitializerBlockSymbol(TypeSymbol owner, boolean isStatic) {
    this.owner = owner;
    this.isStatic = isStatic;
  }

  @Override
  public MethodTree declaration() {
    return null;
  }

  @Override
  public List<Type> parameterTypes() {
    return Collections.emptyList();
  }

  @Override
  public List<Symbol> declarationParameters() {
    return Collections.emptyList();
  }

  @Override
  public Symbol.TypeSymbol returnType() {
    return TypeSymbol.UNKNOWN_TYPE;
  }

  @Override
  public List<Type> thrownTypes() {
    return Collections.emptyList();
  }

  @Override
  public List<Symbol.MethodSymbol> overriddenSymbols() {
    return Collections.emptyList();
  }

  @Override
  public Symbol owner() {
    return owner;
  }

  @Override
  public Type type() {
    return Type.UNKNOWN;
  }

  @Override
  public boolean isVariableSymbol() {
    return false;
  }

  @Override
  public boolean isTypeSymbol() {
    return false;
  }

  @Override
  public boolean isMethodSymbol() {
    return true;
  }

  @Override
  public boolean isPackageSymbol() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPackageVisibility() {
    return false;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public SymbolMetadata metadata() {
    return Symbols.EMPTY_METADATA;
  }

  @Override
  public TypeSymbol enclosingClass() {
    return owner;
  }

  @Override
  public List<IdentifierTree> usages() {
    return Collections.emptyList();
  }

  @Override
  public String name() {
    return isStatic ? "<clinit> (initializer block)" : "<init> (initializer block)";
  }

  @Override
  public String signature() {
    return owner.name() + "." + name();
  }

  @Override
  public boolean isOverridable() {
    return false;
  }

  @Override
  public boolean isParametrizedMethod() {
    return false;
  }

  @Override
  public boolean isDefaultMethod() {
    return false;
  }

  @Override
  public boolean isSynchronizedMethod() {
    return false;
  }

  @Override
  public boolean isVarArgsMethod() {
    return false;
  }

  @Override
  public boolean isNativeMethod() {
    return false;
  }
}
