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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.location.Position.endOf;
import static org.sonar.plugins.java.api.location.Position.startOf;

public final class JWarning extends JProblem {
  private final Position start;
  private final Position end;

  private Tree syntaxTree;

  JWarning(String message, Type type, int startLine, int startColumnOffset, int endLine, int endColumnOffset) {
    super(message, type);
    this.start = InternalPosition.atOffset(startLine, startColumnOffset);
    this.end = InternalPosition.atOffset(endLine, endColumnOffset);
  }

  public Tree syntaxTree() {
    return syntaxTree;
  }

  Position start() {
    return start;
  }

  Position end() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JWarning)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    JWarning jWarning = (JWarning) o;
    // skip syntaxTree
    return start.equals(jWarning.start) && end.equals(jWarning.end);
  }

  @Override
  public int hashCode() {
    // skip syntaxTree
    return Objects.hash(super.hashCode(), start, end);
  }

  public static class Mapper extends SubscriptionVisitor {

    private static final Set<Tree.Kind> KINDS = Stream.of(Type.values())
      .map(Type::getKinds)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());

    private final Map<Type, Set<JWarning>> warningsByType = new EnumMap<>(Type.class);

    private final PriorityQueue<JWarning> warnings = new PriorityQueue<>(Comparator.comparing(JWarning::start).thenComparing(JWarning::end));

    private Mapper(CompilationUnit ast, LineColumnConverter lineColumnConverter) {
      Stream.of(ast.getProblems())
        .map(problem -> convert(problem, ast, lineColumnConverter))
        .filter(Objects::nonNull)
        .forEach(warnings::add);
    }

    @CheckForNull
    private static JWarning convert(IProblem problem, CompilationUnit root, LineColumnConverter lineColumnConverter) {
      for (Type type : Type.values()) {
        if (type.matches(problem)) {
          LineColumnConverter.Pos start = lineColumnConverter.toPos(problem.getSourceStart());
          LineColumnConverter.Pos end = lineColumnConverter.toPos(problem.getSourceStart());
          return new JWarning(problem.getMessage(),
            type,
            start.line(), start.columnOffset(),
            end.line(), end.columnOffset());
        }
      }
      return null;
    }

    public static Mapper warningsFor(CompilationUnit ast, LineColumnConverter lineColumnConverter) {
      return new Mapper(ast, lineColumnConverter);
    }

    public void mappedInto(JavaTree.CompilationUnitTreeImpl cut) {
      scanTree(cut);
      cut.addWarnings(warningsByType);
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return new ArrayList<>(KINDS);
    }

    @Override
    public void visitNode(Tree tree) {
      if (warnings.isEmpty()) {
        return;
      }
      for (Iterator<JWarning> iterator = warnings.iterator(); iterator.hasNext();) {
        JWarning warning = iterator.next();
        if (isInsideTree(warning, tree)) {
          setSyntaxTree(warning, tree);
          warningsByType.computeIfAbsent(warning.type(), k -> new LinkedHashSet<>()).add(warning);

          if (matchesTreeExactly(warning)) {
            iterator.remove();
          }
        }
      }
    }

    @VisibleForTesting
    static void setSyntaxTree(JWarning warning, Tree tree) {
      if (warning.syntaxTree == null || isMorePreciseTree(warning.syntaxTree, tree)) {
        warning.syntaxTree = tree;
      }
    }

    @VisibleForTesting
    static boolean isInsideTree(JWarning warning, Tree tree) {
      if (warning.type().getKinds().stream().noneMatch(tree::is)) {
        // wrong kind
        return false;
      }
      return warning.start().compareTo(startOf(tree)) >= 0
        && warning.end().compareTo(endOf(tree)) <= 0;
    }

    @VisibleForTesting
    static boolean isMorePreciseTree(Tree currentTree, Tree newTree) {
      return startOf(newTree).compareTo(startOf(currentTree)) >= 0
        && endOf(newTree).compareTo(endOf(currentTree)) <= 0;
    }

    @VisibleForTesting
    static boolean matchesTreeExactly(JWarning warning) {
      Tree syntaxTree = warning.syntaxTree();
      return warning.start().compareTo(startOf(syntaxTree)) == 0
        && warning.end().compareTo(endOf(syntaxTree)) == 0;
    }
  }

}
