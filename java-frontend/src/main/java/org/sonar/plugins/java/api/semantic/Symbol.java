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
package org.sonar.plugins.java.api.semantic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Interface to access symbol information.
 */
public interface Symbol {

  /**
   * An instance of {@link Symbol} representing an unknown root package
   */
  Symbol ROOT_PACKAGE = new Symbols.RootPackageSymbol();
  /**
   * An instance of {@link Symbol} representing an unknown symbol
   */
  Symbol UNKNOWN_SYMBOL = new Symbols.UnknownSymbol();

  /**
   * Name of this symbol.
   * @return simple name of the symbol.
   */
  String name();

  /**
   * The owner of this symbol.
   * @return the symbol that owns this symbol, null for package symbols, or unknown symbols
   */
  @Nullable
  Symbol owner();

  /**
   * Type of symbol.
   * @return the type of this symbol.
   */
  Type type();

  // kinds of symbols
  boolean isVariableSymbol();

  boolean isTypeSymbol();

  boolean isMethodSymbol();

  boolean isPackageSymbol();

  // flags method
  boolean isStatic();

  boolean isFinal();

  boolean isEnum();

  boolean isInterface();

  boolean isAbstract();

  boolean isPublic();

  boolean isPrivate();

  boolean isProtected();

  boolean isPackageVisibility();

  boolean isDeprecated();

  boolean isVolatile();

  boolean isUnknown();

  /**
   * Symbol metadata informations, annotations for instance.
   * @return the metadata of this symbol.
   */
  SymbolMetadata metadata();

  /**
   * The closest enclosing class.
   * @return null for package symbols, themselves for type symbol and enclosing class of methods or variables.
   */
  @Nullable
  TypeSymbol enclosingClass();

  /**
   * The identifier trees that reference this symbol.
   * @return a list of IdentifierTree referencing this symbol. An empty list if this symbol is unused.
   */
  List<IdentifierTree> usages();

  /**
   * Declaration node of this symbol. Currently, only works for declaration within the same file.
   * @return the Tree of the declaration of this symbol. Null if declaration does not occur in the currently analyzed file.
   */
  @Nullable
  Tree declaration();

  /**
   * @return true if this symbol represents a variable which is a local variable of a method.
   */
  default boolean isLocalVariable() {
    return false;
  }

  /**
   * @return true if this symbol represents a variable which is a parameter of a method.
   */
  default boolean isParameter() {
    return false;
  }

  /**
   * Symbol for a type : class, enum, interface or annotation.
   */
  interface TypeSymbol extends Symbol {

    /**
     * An instance of {@link TypeSymbol} representing an unknown type symbol
     */
    TypeSymbol UNKNOWN_TYPE = new Symbols.UnkownTypeSymbol();

    /**
     * Returns the superclass of this type symbol.
     * @return null for java.lang.Object, the superclass for every other type.
     */
    @CheckForNull
    Type superClass();

    /**
     * Interfaces implemented by this type.
     * @return an empty list if this type does not implement any interface.
     */
    List<Type> interfaces();

    /**
     * List of symbols defined by this type symbols.
     * This will not return any inherited symbol.
     * @return The collection of symbols defined by this type.
     */
    Collection<Symbol> memberSymbols();

    /**
     * Lookup symbols accessible from this type with the name passed in parameter.
     * @param name name of searched symbol.
     * @return A collection of symbol matching the looked up name.
     */
    Collection<Symbol> lookupSymbols(String name);

    @Nullable
    @Override
    ClassTree declaration();

    /**
     * @return the set of types that are super types of this type (extended classes and implemented interfaces).
     */
    Set<Type> superTypes();

    /**
     * @return the most outer class containing this symbol.
     */
    Symbol.TypeSymbol outermostClass();

    /**
     * @return true if this type is an annotation.
     */
    boolean isAnnotation();

  }

  /**
   * Symbol for field, method parameters and local variables.
   */
  interface VariableSymbol extends Symbol {

    @Nullable
    @Override
    VariableTree declaration();

    /**
     * @return true if this variable is effectively final.
     * A variable is effectively final if it is not explicitly declared final but never reassigned after initialization.
     */
    boolean isEffectivelyFinal();

    /**
     * @return the constant value of this variable if it has one.
     */
    Optional<Object> constantValue();

  }

  /**
   * Symbol for methods.
   */
  interface MethodSymbol extends Symbol {

    /**
     * Instance of {@link Symbol.MethodSymbol} representing an unknown method symbol
     */
    MethodSymbol UNKNOWN_METHOD = new Symbols.UnknownMethodSymbol();

    /**
     * Type of parameters declared by this method.
     * In case of generic types of method invocations, this list of types is more specialized than declarationParameters().stream().map(Symbol::type).
     * For example it could be String instead of T.
     *
     * @return empty list if method has a zero arity.
     */
    List<Type> parameterTypes();

    /**
     * Symbols of parameters declared by this method.
     * Use to access parameter annotations. Note:
     * 1) in case of generic types of method invocations, this list of types is less specialized than {@link #parameterTypes()}.
     * For example it could be T instead of String.
     * 2) when the declaration comes from binaries, the name of the symbol will be generated (@see JVariableSymbol.ParameterPlaceholderSymbol).
     *
     * @return empty list if methods has not parameters
     */
    List<Symbol> declarationParameters();

    TypeSymbol returnType();

    /**
     * List of the exceptions that can be thrown by the method.
     * @return empty list if no exception are declared in the throw clause of the method.
     */
    List<Type> thrownTypes();

    /**
     * Retrieve the overridden symbols in all the known type hierarchy.
     * Note that it will only returns the symbols which might be determined from known types. The list might therefore not be complete in case of missing dependencies.
     *
     * @since SonarJava 6.15.
     *
     * @return the overridden symbols, or an empty list if the method is not overriding any method or overriding can not be determined (incomplete semantics)
     */
    List<Symbol.MethodSymbol> overriddenSymbols();

    @Nullable
    @Override
    MethodTree declaration();

    /**
     * Compute the signature as identified from bytecode point of view. Will be unique for each method.
     * @return the signature of the method, as String
     */
    String signature();

    /**
     * @return true if the method symbol is overridable.
     */
    boolean isOverridable();

    /**
     * @return true if the method has type parameters.
     */
    boolean isParametrizedMethod();

    /**
     * @return true if the method has a default implementation.
     */
    boolean isDefaultMethod();

    /**
     * @return true if the method is synchronized.
     */
    boolean isSynchronizedMethod();

    /**
     * @return true if the method takes a vararg argument (e.g. `String... args`).
     */
    boolean isVarArgsMethod();

    /**
     * @return true if the method is native.
     */
    boolean isNativeMethod();
  }

  /**
   * Label symbol. Note: this is not a Symbol per say.
   */
  interface LabelSymbol {

    /**
     * Name of that label.
     */
    String name();

    /**
     * Usages tree of this label.
     */
    List<IdentifierTree> usages();

    /**
     * Declaration tree of this label.
     */
    LabeledStatementTree declaration();

  }

}
