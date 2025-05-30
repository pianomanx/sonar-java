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

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

public abstract class LeftCurlyBraceBaseTreeVisitor extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  protected void addIssue(SyntaxToken openBraceToken, JavaCheck check, String message) {
    this.context.reportIssue( check, openBraceToken, message);
  }

  protected abstract void checkTokens(SyntaxToken lastToken, SyntaxToken openBraceToken);

  @Override
  public void visitClass(ClassTree tree) {
    SyntaxToken lastToken = getLastTokenFromSignature(tree);
    if (lastToken != null) {
      checkTokens(lastToken, tree.openBraceToken());
    }
    super.visitClass(tree);
  }

  @CheckForNull
  private static SyntaxToken getLastTokenFromSignature(ClassTree classTree) {
    // JDK 17 sealed classes
    List<TypeTree> permittedTypes = classTree.permittedTypes();
    if (!permittedTypes.isEmpty()) {
      return getIdentifierToken(ListUtils.getLast(permittedTypes));
    }
    List<TypeTree> superInterfaces = classTree.superInterfaces();
    if (!superInterfaces.isEmpty()) {
      return getIdentifierToken(ListUtils.getLast(superInterfaces));
    }
    TypeTree superClass = classTree.superClass();
    if (superClass != null) {
      return getIdentifierToken(superClass);
    }
    TypeParameters typeParameters = classTree.typeParameters();
    if (!typeParameters.isEmpty()) {
      return typeParameters.closeBracketToken();
    }
    // JDK 16 records
    if(classTree.recordCloseParenToken() != null) {
      return classTree.recordCloseParenToken();
    }
    IdentifierTree simpleName = classTree.simpleName();
    if (simpleName != null) {
      return simpleName.identifierToken();
    }
    // enum constants and new class trees are handled separately
    return null;
  }

  private static SyntaxToken getIdentifierToken(TypeTree typeTree) {
    if (typeTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) typeTree).identifierToken();
    }
    if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) typeTree).identifier().identifierToken();
    } else {
      return ((ParameterizedTypeTree) typeTree).typeArguments().closeBracketToken();
    }
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.thenStatement());
    if (tree.elseKeyword() != null) {
      checkBlock(tree.elseKeyword(), tree.elseStatement());
    }
    super.visitIfStatement(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    checkTokens(tree.closeParenToken(), tree.openBraceToken());
    super.visitSwitchStatement(tree);
  }

  @Override
  public void visitSwitchExpression(SwitchExpressionTree tree) {
    checkTokens(tree.closeParenToken(), tree.openBraceToken());
    super.visitSwitchExpression(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.statement());
    super.visitWhileStatement(tree);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    checkBlock(tree.doKeyword(), tree.statement());
    super.visitDoWhileStatement(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.statement());
    super.visitForStatement(tree);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    checkBlock(tree.closeParenToken(), tree.statement());
    super.visitForEachStatement(tree);
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    checkBlock(tree.closeParenToken(), tree.block());
    super.visitSynchronizedStatement(tree);
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    checkBlock(tree.colonToken(), tree.statement());
    super.visitLabeledStatement(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    SyntaxToken closeParenToken = tree.closeParenToken();
    if (closeParenToken != null) {
      checkBlock(closeParenToken, tree.block());
    } else {
      checkBlock(tree.tryKeyword(), tree.block());
    }
    SyntaxToken finallyKeyword = tree.finallyKeyword();
    if (finallyKeyword != null) {
      checkBlock(finallyKeyword, tree.finallyBlock());
    }
    super.visitTryStatement(tree);
  }

  @Override
  public void visitCatch(CatchTree tree) {
    checkBlock(tree.closeParenToken(), tree.block());
    super.visitCatch(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    ClassTree classBody = tree.classBody();
    if (classBody != null && tree.arguments().closeParenToken() != null) {
      checkTokens(tree.arguments().closeParenToken(), classBody.openBraceToken());
    }
    super.visitNewClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    BlockTree blockTree = tree.block();
    if (blockTree != null) {
      checkTokens(getLastTokenFromSignature(tree), blockTree.openBraceToken());
    }
    super.visitMethod(tree);
  }

  private static SyntaxToken getLastTokenFromSignature(MethodTree methodTree) {
    if (methodTree.throwsClauses().isEmpty()) {
      if (isCompactConstructor(methodTree)) {
        return methodTree.simpleName().identifierToken();
      }
      return methodTree.closeParenToken();
    }
    return getIdentifierToken(ListUtils.getLast(methodTree.throwsClauses()));
  }

  private static boolean isCompactConstructor(MethodTree methodTree) {
    return methodTree.is(Tree.Kind.CONSTRUCTOR) && methodTree.closeParenToken() == null;
  }

  @Override
  public void visitBlock(BlockTree tree) {
    if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      StaticInitializerTree staticInitializerTree = (StaticInitializerTree) tree;
      checkTokens(staticInitializerTree.staticKeyword(), staticInitializerTree.openBraceToken());
    }
    super.visitBlock(tree);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    NewClassTree initializer = tree.initializer();
    ClassTree classBody = initializer.classBody();
    if (classBody != null) {
      SyntaxToken openBraceToken = classBody.openBraceToken();
      if (initializer.arguments().closeParenToken() != null) {
        checkTokens(initializer.arguments().closeParenToken(), openBraceToken);
      } else {
        checkTokens(tree.simpleName().identifierToken(), openBraceToken);
      }
    }
    super.visitEnumConstant(tree);
  }

  private void checkBlock(SyntaxToken previousToken, Tree tree) {
    if (tree.is(Tree.Kind.BLOCK)) {
      checkTokens(previousToken, ((BlockTree) tree).openBraceToken());
    }
  }
}
