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

import com.sonar.sslr.api.RecognitionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.GuardedPattern;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.ModuleModifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.OpensDirective;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.Pattern;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.RecordPattern;
import org.eclipse.jdt.core.dom.RequiresDirective;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.TypePattern;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalToken;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.pattern.NullPatternTreeImpl;
import org.sonar.java.model.pattern.RecordPatternTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.SyntaxTrivia.CommentKind;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JParser {

  private static final Logger LOG = LoggerFactory.getLogger(JParser.class);

  private static final Set<Integer> WRONGLY_CATEGORIZED_AS_SYNTAX_ERROR = Set.of(
    // Accept missing default clause, it may be due to missing semantic information of the switch expression,
    // in this case, an enum fully covered with the switch cases will be seen as something that is not an enum
    // when it is unknown, and the parser will wrongly consider the missing default clause as a syntax error.
    IProblem.SwitchExpressionsYieldMissingDefaultCase,
    // Accept missing default clause, it may be due the switch expression being an enum from a wrong dependency.
    // In this case, the parser will wrongly consider the missing default clause as a syntax error.
    IProblem.SwitchExpressionsYieldMissingEnumConstantCase
  );

  private static final Predicate<IProblem> IS_SYNTAX_ERROR = error -> ((error.getID() & IProblem.Syntax) != 0) &&
    !WRONGLY_CATEGORIZED_AS_SYNTAX_ERROR.contains(error.getID());

  private static final Predicate<IProblem> IS_UNDEFINED_TYPE_ERROR = error -> (error.getID() & IProblem.UndefinedType) != 0;

  /**
   * @param unitName see {@link ASTParser#setUnitName(String)}
   * @throws RecognitionException in case of syntax errors
   */
  public static JavaTree.CompilationUnitTreeImpl parse(ASTParser astParser, String version, String unitName, String source) {
    astParser.setUnitName(unitName);
    astParser.setSource(source.toCharArray());

    CompilationUnit astNode;
    try {
      astNode = (CompilationUnit) astParser.createAST(null);
    } catch (Exception e) {
      LOG.error("ECJ: Unable to parse file", e);
      throw new RecognitionException(-1, "ECJ: Unable to parse file.", e);
    }

    return convert(version, unitName, source, astNode);
  }

  static JavaTree.CompilationUnitTreeImpl convert(String version, String unitName, String source, CompilationUnit astNode) {
    List<IProblem> errors = Stream.of(astNode.getProblems()).filter(IProblem::isError).toList();
    Optional<IProblem> possibleSyntaxError = errors.stream().filter(IS_SYNTAX_ERROR).findFirst();
    LineColumnConverter lineColumnConverter = new LineColumnConverter(source);
    if (possibleSyntaxError.isPresent()) {
      IProblem syntaxError = possibleSyntaxError.get();
      LineColumnConverter.Pos pos = lineColumnConverter.toPos(syntaxError.getSourceStart());
      String message = String.format("Parse error at line %d column %d: %s", pos.line(), pos.columnOffset() + 1, syntaxError.getMessage());
      // interrupt parsing
      throw new RecognitionException(pos.line(), message);
    }

    Set<JProblem> undefinedTypes = errors.stream()
      .filter(IS_UNDEFINED_TYPE_ERROR)
      .map(i -> new JProblem(
        i.getMessage(),
        (i.getID() & IProblem.PreviewFeatureUsed) != 0 ? JProblem.Type.PREVIEW_FEATURE_USED : JProblem.Type.UNDEFINED_TYPE))
      .collect(Collectors.toSet());

    JParser converter = new JParser();
    converter.sema = new JSema(astNode.getAST());
    converter.sema.undefinedTypes.addAll(undefinedTypes);
    converter.compilationUnit = astNode;
    converter.tokenManager = createTokenManager(version, unitName, source);
    converter.lineColumnConverter = lineColumnConverter;

    JavaTree.CompilationUnitTreeImpl tree = converter.convertCompilationUnit(astNode);
    tree.sema = converter.sema;
    JWarning.Mapper.warningsFor(astNode, converter.lineColumnConverter).mappedInto(tree);

    ASTUtils.mayTolerateMissingType(astNode.getAST());

    setParents(tree);
    return tree;
  }

  @VisibleForTesting
  static TokenManager createTokenManager(String version, String unitName, String source) {
    return new TokenManager(lex(version, unitName, source.toCharArray()), source, new DefaultCodeFormatterOptions(new HashMap<>()));
  }

  private static void setParents(Tree node) {
    Iterator<Tree> childrenIterator = iteratorFor(node);
    while (childrenIterator.hasNext()) {
      Tree child = childrenIterator.next();
      ((JavaTree) child).setParent(node);
      setParents(child);
    }
  }

  private static Iterator<Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TOKEN) {
      // getChildren throws exception in this case
      return Collections.emptyIterator();
    }
    return ((JavaTree) node).getChildren().iterator();
  }

  @VisibleForTesting
  static List<Token> lex(String version, String unitName, char[] sourceChars) {
    List<Token> tokens = new ArrayList<>();
    Scanner scanner = new Scanner(
      true,
      false,
      false,
      CompilerOptions.versionToJdkLevel(version),
      null,
      null,
      false
    );
    scanner.fakeInModule = "module-info.java".equals(unitName);
    scanner.setSource(sourceChars);
    while (true) {
      try {
        TerminalToken tokenType = scanner.getNextToken();
        Token token = Token.fromCurrent(scanner, tokenType);
        tokens.add(token);
        if (tokenType == TerminalToken.TokenNameEOF) {
          break;
        }
      } catch (InvalidInputException e) {
        throw new IllegalStateException(e);
      }
    }
    return tokens;
  }

  private CompilationUnit compilationUnit;

  private TokenManager tokenManager;
  private LineColumnConverter lineColumnConverter;

  private JSema sema;

  private final Deque<JLabelSymbol> labels = new LinkedList<>();

  private void declaration(@Nullable IBinding binding, Tree node) {
    if (binding == null) {
      return;
    }
    sema.declarations.put(binding, node);
  }

  private void usage(@Nullable IBinding binding, IdentifierTree node) {
    if (binding == null) {
      return;
    }
    binding = JSema.declarationBinding(binding);
    sema.usages.computeIfAbsent(binding, k -> new ArrayList<>()).add(node);
  }

  private void usageLabel(@Nullable IdentifierTreeImpl node) {
    if (node == null) {
      return;
    }
    labels.stream()
      .filter(symbol -> symbol.name().equals(node.name()))
      .findFirst()
      .ifPresent(labelSymbol -> {
        labelSymbol.usages.add(node);
        node.labelSymbol = labelSymbol;
      });
  }

  private int firstTokenIndexAfter(ASTNode e) {
    int index = tokenManager.firstIndexAfter(e, ANY_TOKEN);
    while (isComment(tokenManager.get(index))) {
      index++;
    }
    return index;
  }

  /**
   * @param tokenType {@link TerminalToken}
   */
  private int nextTokenIndex(int tokenIndex, TerminalToken tokenType) {
    assert tokenType != ANY_TOKEN;
    do {
      tokenIndex += 1;
    } while (tokenManager.get(tokenIndex).tokenType != tokenType);
    return tokenIndex;
  }

  /**
   * @param tokenType {@link TerminalToken}
   */
  private InternalSyntaxToken firstTokenBefore(ASTNode e, TerminalToken tokenType) {
    return createSyntaxToken(tokenManager.firstIndexBefore(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalToken}
   */
  private InternalSyntaxToken firstTokenAfter(ASTNode e, TerminalToken tokenType) {
    return createSyntaxToken(tokenManager.firstIndexAfter(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalToken}
   */
  private InternalSyntaxToken firstTokenIn(ASTNode e, TerminalToken tokenType) {
    return createSyntaxToken(tokenManager.firstIndexIn(e, tokenType));
  }

  /**
   * @param tokenTypeCandidateA {@link TerminalToken}
   * @param tokenTypeCandidateB {@link TerminalToken}
   * @return {@link TerminalToken}
   */
  @VisibleForTesting
  static int firstIndexIn(TokenManager tokenManager, ASTNode e, TerminalToken tokenTypeCandidateA, TerminalToken tokenTypeCandidateB) {
    int first = tokenManager.firstIndexIn(e, ANY_TOKEN);
    int last = tokenManager.lastIndexIn(e, ANY_TOKEN);
    for (int tokenIndex = first; tokenIndex <= last; tokenIndex++) {
      Token token = tokenManager.get(tokenIndex);
      if (token.tokenType == tokenTypeCandidateA || token.tokenType == tokenTypeCandidateB) {
        return tokenIndex;
      }
    }
    throw new IllegalStateException("Failed to find token " + tokenTypeCandidateA + " or " + tokenTypeCandidateB +
      " in the tokens of a " + ASTNode.nodeClassForType(e.getNodeType()).getName());
  }

  /**
   * @param tokenType {@link TerminalToken}
   */
  private InternalSyntaxToken lastTokenIn(ASTNode e, TerminalToken tokenType) {
    return createSyntaxToken(tokenManager.lastIndexIn(e, tokenType));
  }

  private InternalSyntaxToken createSyntaxToken(int tokenIndex) {
    Token t = tokenManager.get(tokenIndex);
    String value;
    boolean isEOF;
    if (t.tokenType == TerminalToken.TokenNameEOF) {
      isEOF = true;
      value = "";
    } else {
      isEOF = false;
      value = t.toString(tokenManager.getSource());
    }
    LineColumnConverter.Pos pos = lineColumnConverter.toPos(t.originalStart);
    return new InternalSyntaxToken(pos.line(), pos.columnOffset(), value, collectComments(tokenIndex), isEOF);
  }

  private InternalSyntaxToken createSpecialToken(int tokenIndex) {
    Token t = tokenManager.get(tokenIndex);
    List<SyntaxTrivia> comments = t.tokenType == TerminalToken.TokenNameGREATER
      ? collectComments(tokenIndex)
      : Collections.emptyList();
    LineColumnConverter.Pos pos = lineColumnConverter.toPos(t.originalEnd);
    return new InternalSyntaxToken(pos.line(), pos.columnOffset(), ">", comments, false);
  }

  private List<SyntaxTrivia> collectComments(int tokenIndex) {
    int commentIndex = tokenIndex;
    while (commentIndex > 0 && isComment(tokenManager.get(commentIndex - 1))) {
      commentIndex--;
    }
    List<SyntaxTrivia> comments = new ArrayList<>();
    for (int i = commentIndex; i < tokenIndex; i++) {
      Token t = tokenManager.get(i);
      LineColumnConverter.Pos pos = lineColumnConverter.toPos(t.originalStart);
      comments.add(new InternalSyntaxTrivia(convertTokenTypeToCommentKind(t),
        t.toString(tokenManager.getSource()),
        pos.line(),
        pos.columnOffset()
      ));
    }
    return comments;
  }

  @VisibleForTesting
  static CommentKind convertTokenTypeToCommentKind(Token token) {
    return switch (token.tokenType) {
      case TokenNameCOMMENT_BLOCK -> CommentKind.BLOCK;
      case TokenNameCOMMENT_JAVADOC -> CommentKind.JAVADOC;
      case TokenNameCOMMENT_LINE -> CommentKind.LINE;
      case TokenNameCOMMENT_MARKDOWN -> CommentKind.MARKDOWN;
      default -> throw new IllegalStateException("Unexpected value: " + token.tokenType);
    };
  }

  /**
   * {@link Token#isComment()} has an issue https://github.com/eclipse-jdt/eclipse.jdt.core/issues/3914
   * it does not support Markdown comments. This method has to be used instead.
   */
  @VisibleForTesting
  static boolean isComment(Token token) {
    return switch (token.tokenType) {
      case TokenNameCOMMENT_BLOCK, TokenNameCOMMENT_JAVADOC, TokenNameCOMMENT_LINE, TokenNameCOMMENT_MARKDOWN -> true;
      default -> false;
    };
  }

  private void addEmptyStatementsToList(int tokenIndex, List list) {
    while (true) {
      Token token;
      do {
        tokenIndex++;
        token = tokenManager.get(tokenIndex);
      } while (isComment(token));

      if (token.tokenType != TerminalToken.TokenNameSEMICOLON) {
        break;
      }
      list.add(new EmptyStatementTreeImpl(createSyntaxToken(tokenIndex)));
    }
  }

  private JavaTree.CompilationUnitTreeImpl convertCompilationUnit(CompilationUnit e) {
    PackageDeclarationTree packageDeclaration = null;
    if (e.getPackage() != null) {
      packageDeclaration = new JavaTree.PackageDeclarationTreeImpl(
        convertAnnotations(e.getPackage().annotations()),
        firstTokenIn(e.getPackage(), TerminalToken.TokenNamepackage),
        convertExpression(e.getPackage().getName()),
        firstTokenIn(e.getPackage(), TerminalToken.TokenNameSEMICOLON)
      );
    }

    List<ImportClauseTree> imports = new ArrayList<>();
    for (int i = 0; i < e.imports().size(); i++) {
      ImportDeclaration e2 = (ImportDeclaration) e.imports().get(i);
      ExpressionTree name = convertImportName(e2.getName());
      if (e2.isOnDemand()) {
        name = new MemberSelectExpressionTreeImpl(
          name,
          lastTokenIn(e2, TerminalToken.TokenNameDOT),
          new IdentifierTreeImpl(lastTokenIn(e2, TerminalToken.TokenNameMULTIPLY))
        );
      }
      JavaTree.ImportTreeImpl t = new JavaTree.ImportTreeImpl(
        firstTokenIn(e2, TerminalToken.TokenNameimport),
        e2.isStatic() ? firstTokenIn(e2, TerminalToken.TokenNamestatic) : null,
        name,
        lastTokenIn(e2, TerminalToken.TokenNameSEMICOLON)
      );
      t.binding = e2.resolveBinding();
      imports.add(t);

      int tokenIndex = tokenManager.lastIndexIn(e2, TerminalToken.TokenNameSEMICOLON);
      addEmptyStatementsToList(tokenIndex, imports);
    }

    List<Tree> types = new ArrayList<>();
    for (Object type : e.types()) {
      processBodyDeclaration((AbstractTypeDeclaration) type, types);
    }

    if (e.imports().isEmpty() && e.types().isEmpty()) {
      addEmptyStatementsToList(-1, imports);
    }

    return new JavaTree.CompilationUnitTreeImpl(
      packageDeclaration,
      imports,
      types,
      convertModuleDeclaration(compilationUnit.getModule()),
      firstTokenAfter(e, TerminalToken.TokenNameEOF)
    );
  }

  private ExpressionTree convertImportName(Name node) {
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME: {
        return new IdentifierTreeImpl(firstTokenIn(node, TerminalToken.TokenNameIdentifier));
      }
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        return new MemberSelectExpressionTreeImpl(
          convertImportName(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
          (IdentifierTreeImpl) convertImportName(e.getName()));
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  @Nullable
  private ModuleDeclarationTree convertModuleDeclaration(@Nullable ModuleDeclaration e) {
    if (e == null) {
      return null;
    }
    List<ModuleDirectiveTree> moduleDirectives = new ArrayList<>();
    for (Object o : e.moduleStatements()) {
      moduleDirectives.add(
        convertModuleDirective((ModuleDirective) o)
      );
    }
    return new ModuleDeclarationTreeImpl(
      convertAnnotations(e.annotations()),
      e.isOpen() ? firstTokenIn(e, TerminalToken.TokenNameopen) : null,
      firstTokenBefore(e.getName(), TerminalToken.TokenNamemodule),
      convertModuleName(e.getName()),
      firstTokenAfter(e.getName(), TerminalToken.TokenNameLBRACE),
      moduleDirectives,
      lastTokenIn(e, TerminalToken.TokenNameRBRACE)
    );
  }

  private ModuleNameTreeImpl convertModuleName(Name node) {
    switch (node.getNodeType()) {
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        ModuleNameTreeImpl t = convertModuleName(e.getQualifier());
        t.add(new IdentifierTreeImpl(firstTokenIn(e.getName(), TerminalToken.TokenNameIdentifier)));
        return t;
      }
      case ASTNode.SIMPLE_NAME: {
        ModuleNameTreeImpl t = ModuleNameTreeImpl.emptyList();
        t.add(new IdentifierTreeImpl(firstTokenIn(node, TerminalToken.TokenNameIdentifier)));
        return t;
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private ModuleNameListTreeImpl convertModuleNames(List<?> list) {
    ModuleNameListTreeImpl t = ModuleNameListTreeImpl.emptyList();
    for (int i = 0; i < list.size(); i++) {
      Name o = (Name) list.get(i);
      if (i > 0) {
        t.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
      }
      t.add(convertModuleName(o));
    }
    return t;
  }

  private ModuleDirectiveTree convertModuleDirective(ModuleDirective node) {
    switch (node.getNodeType()) {
      case ASTNode.REQUIRES_DIRECTIVE: {
        RequiresDirective e = (RequiresDirective) node;
        List<ModifierTree> modifiers = new ArrayList<>();
        for (Object o : e.modifiers()) {
          switch (((ModuleModifier) o).getKeyword().toString()) {
            case "static":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            case "transitive":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.TRANSITIVE, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            default:
              throw new IllegalStateException();
          }
        }
        return new RequiresDirectiveTreeImpl(
          firstTokenIn(e, TerminalToken.TokenNamerequires),
          new ModifiersTreeImpl(modifiers),
          convertModuleName(e.getName()),
          lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
        );
      }
      case ASTNode.EXPORTS_DIRECTIVE: {
        ExportsDirective e = (ExportsDirective) node;
        return new ExportsDirectiveTreeImpl(
          firstTokenIn(e, TerminalToken.TokenNameexports),
          convertExpression(e.getName()),
          e.modules().isEmpty() ? null : firstTokenAfter(e.getName(), TerminalToken.TokenNameto),
          convertModuleNames(e.modules()),
          lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
        );
      }
      case ASTNode.OPENS_DIRECTIVE: {
        OpensDirective e = (OpensDirective) node;
        return new OpensDirectiveTreeImpl(
          firstTokenIn(e, TerminalToken.TokenNameopens),
          convertExpression(e.getName()),
          e.modules().isEmpty() ? null : firstTokenAfter(e.getName(), TerminalToken.TokenNameto),
          convertModuleNames(e.modules()),
          lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
        );
      }
      case ASTNode.USES_DIRECTIVE: {
        UsesDirective e = (UsesDirective) node;
        return new UsesDirectiveTreeImpl(
          firstTokenIn(e, TerminalToken.TokenNameuses),
          (TypeTree) convertExpression(e.getName()),
          lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
        );
      }
      case ASTNode.PROVIDES_DIRECTIVE: {
        ProvidesDirective e = (ProvidesDirective) node;
        QualifiedIdentifierListTreeImpl typeNames = QualifiedIdentifierListTreeImpl.emptyList();
        for (int i = 0; i < e.implementations().size(); i++) {
          Name o = (Name) e.implementations().get(i);
          if (i > 0) {
            typeNames.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
          }
          typeNames.add((TypeTree) convertExpression(o));
        }
        return new ProvidesDirectiveTreeImpl(
          firstTokenIn(e, TerminalToken.TokenNameprovides),
          (TypeTree) convertExpression(e.getName()),
          firstTokenAfter(e.getName(), TerminalToken.TokenNamewith),
          typeNames,
          lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
        );
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private ClassTreeImpl convertTypeDeclaration(AbstractTypeDeclaration e) {
    List<Tree> members = new ArrayList<>();

    int leftBraceTokenIndex = findLeftBraceTokenIndex(e);
    addEmptyStatementsToList(leftBraceTokenIndex, members);

    for (Object o : e.bodyDeclarations()) {
      processBodyDeclaration((BodyDeclaration) o, members);
    }

    ModifiersTreeImpl modifiers = convertModifiers(e.modifiers());
    IdentifierTreeImpl name = createSimpleName(e.getName());

    InternalSyntaxToken openBraceToken = createSyntaxToken(leftBraceTokenIndex);
    InternalSyntaxToken closeBraceToken = lastTokenIn(e, TerminalToken.TokenNameRBRACE);

    final ClassTreeImpl t;
    switch (e.getNodeType()) {
      case ASTNode.TYPE_DECLARATION:
        t = convertTypeDeclaration((TypeDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      case ASTNode.ENUM_DECLARATION:
        t = convertEnumDeclaration((EnumDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      case ASTNode.RECORD_DECLARATION:
        t = convertRecordDeclaration((RecordDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
        t = convertAnnotationTypeDeclaration((AnnotationTypeDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(e.getNodeType()).toString());
    }

    // no-op for annotation-types
    completeSuperInterfaces(e, t);

    t.typeBinding = e.resolveBinding();
    declaration(t.typeBinding, t);

    return t;
  }

  private ClassTreeImpl convertTypeDeclaration(TypeDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
                                               InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), e.isInterface() ? TerminalToken.TokenNameinterface : TerminalToken.TokenNameclass);
    ClassTreeImpl t = new ClassTreeImpl(e.isInterface() ? Tree.Kind.INTERFACE : Tree.Kind.CLASS, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name)
      .completeTypeParameters(convertTypeParameters(e.typeParameters()));

    if (!e.permittedTypes().isEmpty()) {
      List permittedTypes = e.permittedTypes();
      InternalSyntaxToken permitsKeyword = firstTokenBefore((Type) permittedTypes.get(0), TerminalToken.TokenNameRestrictedIdentifierpermits);
      QualifiedIdentifierListTreeImpl classPermittedTypes = QualifiedIdentifierListTreeImpl.emptyList();

      convertSeparatedTypeList(permittedTypes, classPermittedTypes);
      t.completePermittedTypes(permitsKeyword, classPermittedTypes);
    }

    if (!e.isInterface() && e.getSuperclassType() != null) {
      Type superclassType = e.getSuperclassType();
      t.completeSuperclass(firstTokenBefore(superclassType, TerminalToken.TokenNameextends), convertType(superclassType));
    }
    return t;
  }

  private ClassTreeImpl convertEnumDeclaration(EnumDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
                                               InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    List<Tree> enumConstants = new ArrayList<>();
    for (Object o : e.enumConstants()) {
      // introduced as first members
      enumConstants.add(processEnumConstantDeclaration((EnumConstantDeclaration) o));
    }
    members.addAll(0, enumConstants);

    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), TerminalToken.TokenNameenum);
    return new ClassTreeImpl(Tree.Kind.ENUM, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name);
  }

  private ClassTreeImpl convertAnnotationTypeDeclaration(AnnotationTypeDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
    InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), TerminalToken.TokenNameinterface);
    return new ClassTreeImpl(Tree.Kind.ANNOTATION_TYPE, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name)
      .completeAtToken(firstTokenBefore(e.getName(), TerminalToken.TokenNameAT));
  }

  private ClassTreeImpl convertRecordDeclaration(RecordDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
                                                 InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), TerminalToken.TokenNameRestrictedIdentifierrecord);
    List recordComponents = e.recordComponents();
    InternalSyntaxToken openParen = firstTokenAfter(e.getName(), TerminalToken.TokenNameLPAREN);
    InternalSyntaxToken closeParen = firstTokenAfter(
      recordComponents.isEmpty() ? e.getName() : (ASTNode) recordComponents.get(recordComponents.size() - 1),
      TerminalToken.TokenNameRPAREN);
    return new ClassTreeImpl(Tree.Kind.RECORD, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name)
      .completeTypeParameters(convertTypeParameters(e.typeParameters()))
      .completeRecordComponents(openParen, convertRecordComponents(e), closeParen);
  }

  private List<VariableTree> convertRecordComponents(RecordDeclaration e) {
    List<VariableTree> recordComponents = new ArrayList<>();

    for (int i = 0; i < e.recordComponents().size(); i++) {
      SingleVariableDeclaration o = (SingleVariableDeclaration) e.recordComponents().get(i);
      VariableTreeImpl recordComponent = convertVariable(o);
      if (i < e.recordComponents().size() - 1) {
        recordComponent.setEndToken(firstTokenAfter(o, TerminalToken.TokenNameCOMMA));
      }
      recordComponents.add(recordComponent);
    }
    return recordComponents;
  }

  private int findLeftBraceTokenIndex(AbstractTypeDeclaration e) {
    // TODO try to simplify, note that type annotations can contain LBRACE
    if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
      EnumDeclaration enumDeclaration = (EnumDeclaration) e;
      if (!enumDeclaration.enumConstants().isEmpty()) {
        return tokenManager.firstIndexBefore((ASTNode) enumDeclaration.enumConstants().get(0), TerminalToken.TokenNameLBRACE);
      }
      if (!enumDeclaration.bodyDeclarations().isEmpty()) {
        return tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalToken.TokenNameLBRACE);
      }
      return tokenManager.lastIndexIn(e, TerminalToken.TokenNameLBRACE);
    }
    if (!e.bodyDeclarations().isEmpty()) {
      // for records, bodyDeclarations may not be in the order encountered in file, for classes they are
      List<BodyDeclaration> bodyDeclarations = e.bodyDeclarations();
      BodyDeclaration firstDeclaration = bodyDeclarations.get(0);
      for (int i = 1; i < bodyDeclarations.size(); i++) {
        BodyDeclaration declaration = bodyDeclarations.get(i);
        if (firstDeclaration.getStartPosition() > declaration.getStartPosition()) {
          firstDeclaration = declaration;
        }
      }
      return tokenManager.firstIndexBefore(firstDeclaration, TerminalToken.TokenNameLBRACE);
    }
    return tokenManager.lastIndexIn(e, TerminalToken.TokenNameLBRACE);
  }

  private void completeSuperInterfaces(AbstractTypeDeclaration e, ClassTreeImpl t) {
    List superInterfaces = superInterfaceTypes(e);
    if (!superInterfaces.isEmpty()) {
      QualifiedIdentifierListTreeImpl interfaces = QualifiedIdentifierListTreeImpl.emptyList();
      convertSeparatedTypeList(superInterfaces, interfaces);

      ASTNode firstInterface = (ASTNode) superInterfaces.get(0);
      InternalSyntaxToken keyword = firstTokenBefore(firstInterface, t.is(Tree.Kind.INTERFACE) ? TerminalToken.TokenNameextends : TerminalToken.TokenNameimplements);
      t.completeInterfaces(keyword, interfaces);
    }
  }

  private static List<?> superInterfaceTypes(AbstractTypeDeclaration e) {
    switch (e.getNodeType()) {
      case ASTNode.TYPE_DECLARATION:
        return ((TypeDeclaration) e).superInterfaceTypes();
      case ASTNode.ENUM_DECLARATION:
        return ((EnumDeclaration) e).superInterfaceTypes();
      case ASTNode.RECORD_DECLARATION:
        return ((RecordDeclaration) e).superInterfaceTypes();
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
        return Collections.emptyList();
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(e.getNodeType()).toString());
    }
  }

  private <T extends Tree> void convertSeparatedTypeList(List<? extends Type> source, ListTree<T> target) {
    for (int i = 0; i < source.size(); i++) {
      Type o = source.get(i);
      T tree = (T) convertType(o);
      if (i > 0) {
        target.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
      }
      target.add(tree);
    }
  }

  private EnumConstantTreeImpl processEnumConstantDeclaration(EnumConstantDeclaration e) {
    final int openParTokenIndex = firstTokenIndexAfter(e.getName());
    final InternalSyntaxToken openParToken;
    final InternalSyntaxToken closeParToken;
    if (tokenManager.get(openParTokenIndex).tokenType == TerminalToken.TokenNameLPAREN) {
      openParToken = createSyntaxToken(openParTokenIndex);
      closeParToken = e.arguments().isEmpty()
        ? firstTokenAfter(e.getName(), TerminalToken.TokenNameRPAREN)
        : firstTokenAfter((ASTNode) e.arguments().get(e.arguments().size() - 1), TerminalToken.TokenNameRPAREN);
    } else {
      openParToken = null;
      closeParToken = null;
    }

    final ArgumentListTreeImpl arguments = convertArguments(openParToken, e.arguments(), closeParToken);
    ClassTreeImpl classBody = null;
    if (e.getAnonymousClassDeclaration() != null) {
      List<Tree> members = new ArrayList<>();
      for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
        processBodyDeclaration((BodyDeclaration) o, members);
      }
      classBody = new ClassTreeImpl(
        Tree.Kind.CLASS,
        firstTokenIn(e.getAnonymousClassDeclaration(), TerminalToken.TokenNameLBRACE),
        members,
        lastTokenIn(e.getAnonymousClassDeclaration(), TerminalToken.TokenNameRBRACE)
      );
      classBody.typeBinding = e.getAnonymousClassDeclaration().resolveBinding();
      declaration(classBody.typeBinding, classBody);
    }

    final int separatorTokenIndex = firstTokenIndexAfter(e);
    final InternalSyntaxToken separatorToken;
    switch (tokenManager.get(separatorTokenIndex).tokenType) {
      case TokenNameCOMMA,
        TokenNameSEMICOLON:
        separatorToken = createSyntaxToken(separatorTokenIndex);
        break;
      case TokenNameRBRACE:
        separatorToken = null;
        break;
      default:
        throw new IllegalStateException();
    }

    IdentifierTreeImpl identifier = createSimpleName(e.getName());
    if (e.getAnonymousClassDeclaration() == null) {
      identifier.binding = excludeRecovery(e.resolveConstructorBinding(), arguments.size());
    } else {
      identifier.binding = findConstructorForAnonymousClass(e.getAST(), identifier.typeBinding, e.resolveConstructorBinding());
    }
    usage(identifier.binding, identifier);

    EnumConstantTreeImpl t = new EnumConstantTreeImpl(
      convertModifiers(e.modifiers()),
      identifier,
      new NewClassTreeImpl(identifier, arguments, classBody),
      separatorToken
    );
    t.variableBinding = e.resolveVariable();
    declaration(t.variableBinding, t);
    return t;
  }

  private void processBodyDeclaration(BodyDeclaration node, List<Tree> members) {
    final int lastTokenIndex;

    switch (node.getNodeType()) {
      case ASTNode.ANNOTATION_TYPE_DECLARATION,
        ASTNode.ENUM_DECLARATION,
        ASTNode.RECORD_DECLARATION,
        ASTNode.TYPE_DECLARATION:
        lastTokenIndex = processTypeDeclaration((AbstractTypeDeclaration) node, members);
        break;
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
        lastTokenIndex = processAnnotationTypeMemberDeclaration((AnnotationTypeMemberDeclaration) node, members);
        break;
      case ASTNode.INITIALIZER:
        lastTokenIndex = processInitializerDeclaration((Initializer) node, members);
        break;
      case ASTNode.METHOD_DECLARATION:
        lastTokenIndex = processMethodDeclaration((MethodDeclaration) node, members);
        break;
      case ASTNode.FIELD_DECLARATION:
        lastTokenIndex = processFieldDeclaration((FieldDeclaration) node, members);
        break;
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }

    addEmptyStatementsToList(lastTokenIndex, members);
  }

  private int processTypeDeclaration(AbstractTypeDeclaration node, List<Tree> members) {
    members.add(convertTypeDeclaration(node));
    return tokenManager.lastIndexIn(node, TerminalToken.TokenNameRBRACE);
  }

  private int processAnnotationTypeMemberDeclaration(AnnotationTypeMemberDeclaration e, List<Tree> members) {
    FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(
      firstTokenAfter(e.getName(), TerminalToken.TokenNameLPAREN),
      firstTokenAfter(e.getName(), TerminalToken.TokenNameRPAREN));

    Expression defaultExpression = e.getDefault();
    InternalSyntaxToken defaultToken = defaultExpression == null ? null : firstTokenBefore(defaultExpression, TerminalToken.TokenNamedefault);
    ExpressionTree defaultValue = defaultExpression == null ? null : convertExpression(defaultExpression);

    MethodTreeImpl t = new MethodTreeImpl(parameters, defaultToken, defaultValue)
      .complete(convertType(e.getType()), createSimpleName(e.getName()), lastTokenIn(e, TerminalToken.TokenNameSEMICOLON))
      .completeWithModifiers(convertModifiers(e.modifiers()));

    t.methodBinding = e.resolveBinding();
    declaration(t.methodBinding, t);

    members.add(t);
    return tokenManager.lastIndexIn(e, TerminalToken.TokenNameSEMICOLON);
  }

  private int processInitializerDeclaration(Initializer e, List<Tree> members) {
    BlockTreeImpl blockTree = convertBlock(e.getBody());
    if (org.eclipse.jdt.core.dom.Modifier.isStatic(e.getModifiers())) {
      members.add(new StaticInitializerTreeImpl(
        firstTokenIn(e, TerminalToken.TokenNamestatic),
        (InternalSyntaxToken) blockTree.openBraceToken(),
        blockTree.body(),
        (InternalSyntaxToken) blockTree.closeBraceToken()));
    } else {
      members.add(new BlockTreeImpl(
        Tree.Kind.INITIALIZER,
        (InternalSyntaxToken) blockTree.openBraceToken(),
        blockTree.body(),
        (InternalSyntaxToken) blockTree.closeBraceToken()));
    }
    return tokenManager.lastIndexIn(e, TerminalToken.TokenNameRBRACE);
  }

  private int processMethodDeclaration(MethodDeclaration e, List<Tree> members) {
    List p = e.parameters();
    final FormalParametersListTreeImpl formalParameters;
    if (e.isCompactConstructor()) {
      // only used for records
      formalParameters = new FormalParametersListTreeImpl(null, null);
    } else {
      InternalSyntaxToken openParen = firstTokenAfter(e.getName(), TerminalToken.TokenNameLPAREN);
      InternalSyntaxToken closeParen = firstTokenAfter(p.isEmpty() ? e.getName() : (ASTNode) p.get(p.size() - 1), TerminalToken.TokenNameRPAREN);
      formalParameters = new FormalParametersListTreeImpl(openParen, closeParen);
    }

    for (int i = 0; i < p.size(); i++) {
      SingleVariableDeclaration o = (SingleVariableDeclaration) p.get(i);
      VariableTreeImpl parameter = convertVariable(o);
      if (i < p.size() - 1) {
        parameter.setEndToken(firstTokenAfter(o, TerminalToken.TokenNameCOMMA));
      }
      formalParameters.add(parameter);
    }

    QualifiedIdentifierListTreeImpl thrownExceptionTypes = QualifiedIdentifierListTreeImpl.emptyList();
    List tt = e.thrownExceptionTypes();
    convertSeparatedTypeList(tt, thrownExceptionTypes);

    Block body = e.getBody();
    Type returnType = e.getReturnType2();
    InternalSyntaxToken throwsToken = tt.isEmpty() ? null : firstTokenBefore((Type) tt.get(0), TerminalToken.TokenNamethrows);
    InternalSyntaxToken semcolonToken = body == null ? lastTokenIn(e, TerminalToken.TokenNameSEMICOLON) : null;
    MethodTreeImpl t = new MethodTreeImpl(
      returnType == null ? null : applyExtraDimensions(convertType(returnType), e.extraDimensions()),
      createSimpleName(e.getName()),
      formalParameters,
      throwsToken,
      thrownExceptionTypes,
      body == null ? null : convertBlock(body),
      semcolonToken
    ).completeWithModifiers(
      convertModifiers(e.modifiers())
    ).completeWithTypeParameters(
      convertTypeParameters(e.typeParameters())
    );
    t.methodBinding = e.resolveBinding();
    declaration(t.methodBinding, t);

    members.add(t);
    return tokenManager.lastIndexIn(e, body == null ? TerminalToken.TokenNameSEMICOLON : TerminalToken.TokenNameRBRACE);
  }

  private int processFieldDeclaration(FieldDeclaration fieldDeclaration, List<Tree> members) {
    ModifiersTreeImpl modifiers = convertModifiers(fieldDeclaration.modifiers());
    TypeTree type = convertType(fieldDeclaration.getType());

    for (int i = 0; i < fieldDeclaration.fragments().size(); i++) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration.fragments().get(i);
      VariableTreeImpl t = new VariableTreeImpl(createSimpleName(fragment.getName()))
        .completeModifiersAndType(modifiers, applyExtraDimensions(type, fragment.extraDimensions()));

      if (fragment.getInitializer() != null) {
        t.completeTypeAndInitializer(t.type(), firstTokenAfter(fragment.getName(), TerminalToken.TokenNameEQUAL), convertExpression(fragment.getInitializer()));
      }

      t.setEndToken(firstTokenAfter(fragment, i + 1 < fieldDeclaration.fragments().size() ? TerminalToken.TokenNameCOMMA : TerminalToken.TokenNameSEMICOLON));
      t.variableBinding = fragment.resolveBinding();
      declaration(t.variableBinding, t);

      members.add(t);
    }
    return tokenManager.lastIndexIn(fieldDeclaration, TerminalToken.TokenNameSEMICOLON);
  }

  private ArgumentListTreeImpl convertArguments(@Nullable InternalSyntaxToken openParen, List<?> list, @Nullable InternalSyntaxToken closeParen) {
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList().complete(openParen, closeParen);
    for (int i = 0; i < list.size(); i++) {
      Expression o = (Expression) list.get(i);
      arguments.add(convertExpression(o));
      if (i < list.size() - 1) {
        arguments.separators().add(firstTokenAfter(o, TerminalToken.TokenNameCOMMA));
      }
    }
    return arguments;
  }

  @Nullable
  private TypeArgumentListTreeImpl convertTypeArguments(List<?> list) {
    if (list.isEmpty()) {
      return null;
    }
    ASTNode last = (ASTNode) list.get(list.size() - 1);
    int tokenIndex = tokenManager.firstIndexAfter(last, ANY_TOKEN);
    while (isComment(tokenManager.get(tokenIndex))) {
      tokenIndex++;
    }
    return convertTypeArguments(
      firstTokenBefore((ASTNode) list.get(0), TerminalToken.TokenNameLESS),
      list,
      // TerminalToken.TokenNameUNSIGNED_RIGHT_SHIFT vs TerminalToken.TokenNameGREATER
      createSpecialToken(tokenIndex)
    );
  }

  private TypeArgumentListTreeImpl convertTypeArguments(InternalSyntaxToken l, List<?> list, InternalSyntaxToken g) {
    TypeArgumentListTreeImpl typeArguments = new TypeArgumentListTreeImpl(l, g);
    for (int i = 0; i < list.size(); i++) {
      Type o = (Type) list.get(i);
      if (i > 0) {
        typeArguments.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
      }
      typeArguments.add(convertType(o));
    }
    return typeArguments;
  }

  private TypeParameterListTreeImpl convertTypeParameters(List<?> list) {
    if (list.isEmpty()) {
      return new TypeParameterListTreeImpl();
    }
    ASTNode last = (ASTNode) list.get(list.size() - 1);
    int tokenIndex = tokenManager.firstIndexAfter(last, ANY_TOKEN);
    while (isComment(tokenManager.get(tokenIndex))) {
      tokenIndex++;
    }
    TypeParameterListTreeImpl t = new TypeParameterListTreeImpl(
      firstTokenBefore((ASTNode) list.get(0), TerminalToken.TokenNameLESS),
      // TerminalToken.TokenNameUNSIGNED_RIGHT_SHIFT vs TerminalToken.TokenNameGREATER
      createSpecialToken(tokenIndex)
    );
    for (int i = 0; i < list.size(); i++) {
      TypeParameter o = (TypeParameter) list.get(i);
      if (i > 0) {
        t.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
      }
      t.add(convertTypeParameter(o));
    }
    return t;
  }

  private TypeParameterTree convertTypeParameter(TypeParameter e) {
    IdentifierTreeImpl i = createSimpleName(e.getName());
    // TODO why ECJ uses IExtendedModifier here instead of Annotation ?
    i.complete(convertAnnotations(e.modifiers()));
    TypeParameterTreeImpl t;
    List<?> typeBounds = e.typeBounds();
    if (typeBounds.isEmpty()) {
      t = new TypeParameterTreeImpl(i);
    } else {
      QualifiedIdentifierListTreeImpl bounds = QualifiedIdentifierListTreeImpl.emptyList();
      for (int j = 0; j < typeBounds.size(); j++) {
        Object o = typeBounds.get(j);
        bounds.add(convertType((Type) o));
        if (j < typeBounds.size() - 1) {
          bounds.separators().add(firstTokenAfter((ASTNode) o, TerminalToken.TokenNameAND));
        }
      }
      t = new TypeParameterTreeImpl(
        i,
        firstTokenAfter(e.getName(), TerminalToken.TokenNameextends),
        bounds
      );
    }
    t.typeBinding = e.resolveBinding();
    return t;
  }

  /**
   * @param extraDimensions list of {@link org.eclipse.jdt.core.dom.Dimension}
   */
  private TypeTree applyExtraDimensions(TypeTree type, List<?> extraDimensions) {
    ITypeBinding typeBinding = ((AbstractTypedTree) type).typeBinding;
    for (int i = 0; i < extraDimensions.size(); i++) {
      Dimension e = (Dimension) extraDimensions.get(i);
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.annotations()),
        firstTokenIn(e, TerminalToken.TokenNameLBRACKET),
        firstTokenIn(e, TerminalToken.TokenNameRBRACKET)
      );
      if (typeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) type).typeBinding = typeBinding.createArrayType(i + 1);
      }
    }
    return type;
  }

  private VariableTreeImpl convertVariable(TypePattern typePattern) {
    if (typePattern.getAST().apiLevel() < AST.JLS22) {
      return convertVariable(typePattern.getPatternVariable());
    }
    VariableDeclaration variableDeclaration = typePattern.getPatternVariable2();
    if (variableDeclaration instanceof VariableDeclarationFragment declarationFragment) {
      return convertVariable(declarationFragment);
    } else {
      return convertVariable((SingleVariableDeclaration) variableDeclaration);
    }
  }

  private VariableTreeImpl convertVariable(VariableDeclarationFragment declarationFragment) {
    return completeInitializerAndBinding(
      new VariableTreeImpl(createSimpleName(declarationFragment.getName())),
      declarationFragment);
  }

  private VariableTreeImpl convertVariable(SingleVariableDeclaration e) {
    // TODO are extraDimensions and varargs mutually exclusive?
    TypeTree type = convertType(e.getType());
    type = applyExtraDimensions(type, e.extraDimensions());
    if (e.isVarargs()) {
      ITypeBinding typeBinding = ((AbstractTypedTree) type).typeBinding;
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.varargsAnnotations()),
        firstTokenAfter(e.getType(), TerminalToken.TokenNameELLIPSIS)
      );
      if (typeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) type).typeBinding = typeBinding.createArrayType(1);
      }
    }

    VariableTreeImpl t = new VariableTreeImpl(
      convertModifiers(e.modifiers()),
      type,
      createSimpleName(e.getName())
    );
    return completeInitializerAndBinding(t, e);
  }

  private VariableTreeImpl completeInitializerAndBinding(VariableTreeImpl t, VariableDeclaration e) {
    if (e.getInitializer() != null) {
      t.completeTypeAndInitializer(
        t.type(),
        firstTokenAfter(e.getName(), TerminalToken.TokenNameEQUAL),
        convertExpression(e.getInitializer())
      );
    }
    t.variableBinding = e.resolveBinding();
    if (t.variableBinding != null) {
      if (t.type() instanceof InferedTypeTree inferredType) {
        inferredType.typeBinding = t.variableBinding.getType();
      }
      declaration(t.variableBinding, t);
    }
    return t;
  }

  private void addVariableToList(VariableDeclarationExpression e2, List list) {
    ModifiersTreeImpl modifiers = convertModifiers(e2.modifiers());
    TypeTree type = convertType(e2.getType());

    for (int i = 0; i < e2.fragments().size(); i++) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) e2.fragments().get(i);
      VariableTreeImpl t = new VariableTreeImpl(createSimpleName(fragment.getName()));
      t.completeModifiers(modifiers);
      if (fragment.getInitializer() == null) {
        t.completeType(applyExtraDimensions(type, fragment.extraDimensions()));
      } else {
        t.completeTypeAndInitializer(
          applyExtraDimensions(type, fragment.extraDimensions()),
          firstTokenBefore(fragment.getInitializer(), TerminalToken.TokenNameEQUAL),
          convertExpression(fragment.getInitializer())
        );
      }
      if (i < e2.fragments().size() - 1) {
        t.setEndToken(firstTokenAfter(fragment, TerminalToken.TokenNameCOMMA));
      }
      t.variableBinding = fragment.resolveBinding();
      declaration(t.variableBinding, t);
      list.add(t);
    }
  }

  private VarTypeTreeImpl convertVarType(SimpleType simpleType) {
    VarTypeTreeImpl varTree = new VarTypeTreeImpl(firstTokenIn(simpleType.getName(), TerminalToken.TokenNameIdentifier));
    varTree.typeBinding = simpleType.resolveBinding();
    return varTree;
  }

  private IdentifierTreeImpl createSimpleName(SimpleName e) {
    int tokenIndex = firstIndexIn(tokenManager, e, TerminalToken.TokenNameIdentifier, TerminalToken.TokenNameUNDERSCORE);
    Token token = tokenManager.get(tokenIndex);
    boolean isUnnamedVariable = token.tokenType == TerminalToken.TokenNameUNDERSCORE;
    IdentifierTreeImpl t = new IdentifierTreeImpl(createSyntaxToken(tokenIndex), isUnnamedVariable);
    t.typeBinding = e.resolveTypeBinding();
    t.binding = e.resolveBinding();
    return t;
  }

  private BlockTreeImpl convertBlock(Block e) {
    List<StatementTree> statements = new ArrayList<>();
    for (Object o : e.statements()) {
      addStatementToList((Statement) o, statements);
    }
    return new BlockTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameLBRACE),
      statements,
      lastTokenIn(e, TerminalToken.TokenNameRBRACE)
    );
  }

  private void addStatementToList(Statement node, List<StatementTree> statements) {
    if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
      VariableDeclarationStatement e = (VariableDeclarationStatement) node;
      TypeTree tType = convertType(e.getType());
      ModifiersTreeImpl modifiers = convertModifiers(e.modifiers());
      for (int i = 0; i < e.fragments().size(); i++) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) e.fragments().get(i);
        VariableTreeImpl t = new VariableTreeImpl(createSimpleName(fragment.getName()))
          .completeType(applyExtraDimensions(tType, fragment.extraDimensions()))
          .completeModifiers(modifiers);
        Expression initalizer = fragment.getInitializer();
        if (initalizer != null) {
          InternalSyntaxToken equalToken = firstTokenAfter(fragment.getName(), TerminalToken.TokenNameEQUAL);
          t.completeTypeAndInitializer(t.type(), equalToken, convertExpression(initalizer));
        }
        TerminalToken endTokenType = i < e.fragments().size() - 1 ? TerminalToken.TokenNameCOMMA : TerminalToken.TokenNameSEMICOLON;
        t.setEndToken(firstTokenAfter(fragment, endTokenType));

        t.variableBinding = fragment.resolveBinding();
        declaration(t.variableBinding, t);
        statements.add(t);
      }
    } else {
      statements.add(createStatement(node));
    }
  }

  private StatementTree createStatement(Statement node) {
    switch (node.getNodeType()) {
      case ASTNode.BLOCK:
        return convertBlock((Block) node);
      case ASTNode.EMPTY_STATEMENT:
        return convertEmptyStatement((EmptyStatement) node);
      case ASTNode.RETURN_STATEMENT:
        return convertReturn((ReturnStatement) node);
      case ASTNode.FOR_STATEMENT:
        return convertFor((ForStatement) node);
      case ASTNode.WHILE_STATEMENT:
        return convertWhile((WhileStatement) node);
      case ASTNode.IF_STATEMENT:
        return convertIf((IfStatement) node);
      case ASTNode.BREAK_STATEMENT:
        return convertBreak((BreakStatement) node);
      case ASTNode.DO_STATEMENT:
        return convertDoWhile((DoStatement) node);
      case ASTNode.ASSERT_STATEMENT:
        return convertAssert((AssertStatement) node);
      case ASTNode.SWITCH_STATEMENT:
        return convertSwitchStatement((SwitchStatement) node);
      case ASTNode.SYNCHRONIZED_STATEMENT:
        return convertSynchronized((SynchronizedStatement) node);
      case ASTNode.EXPRESSION_STATEMENT:
        return convertExpressionStatement((ExpressionStatement) node);
      case ASTNode.CONTINUE_STATEMENT:
        return convertContinue((ContinueStatement) node);
      case ASTNode.LABELED_STATEMENT:
        return convertLabel((LabeledStatement) node);
      case ASTNode.ENHANCED_FOR_STATEMENT:
        return convertForeach((EnhancedForStatement) node);
      case ASTNode.THROW_STATEMENT:
        return convertThrow((ThrowStatement) node);
      case ASTNode.TRY_STATEMENT:
        return convertTry((TryStatement) node);
      case ASTNode.TYPE_DECLARATION_STATEMENT:
        return convertTypeDeclaration(((TypeDeclarationStatement) node).getDeclaration());
      case ASTNode.CONSTRUCTOR_INVOCATION:
        return convertConstructorInvocation((ConstructorInvocation) node);
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
        return convertSuperConstructorInvocation((SuperConstructorInvocation) node);
      case ASTNode.YIELD_STATEMENT:
        return convertYield((YieldStatement) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private EmptyStatementTreeImpl convertEmptyStatement(EmptyStatement e) {
    return new EmptyStatementTreeImpl(lastTokenIn(e, TerminalToken.TokenNameSEMICOLON));
  }

  private ReturnStatementTreeImpl convertReturn(ReturnStatement e) {
    Expression expression = e.getExpression();
    return new ReturnStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamereturn),
      expression == null ? null : convertExpression(expression),
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private ForStatementTreeImpl convertFor(ForStatement e) {
    StatementListTreeImpl forInitStatement = StatementListTreeImpl.emptyList();
    for (int i = 0; i < e.initializers().size(); i++) {
      Expression o = (Expression) e.initializers().get(i);
      if (i > 0) {
        forInitStatement.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
      }
      if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
        addVariableToList((VariableDeclarationExpression) o, forInitStatement);
      } else {
        forInitStatement.add(new ExpressionStatementTreeImpl(convertExpression(o), null));
      }
    }

    StatementListTreeImpl forUpdateStatement = StatementListTreeImpl.emptyList();
    for (int i = 0; i < e.updaters().size(); i++) {
      Expression o = (Expression) e.updaters().get(i);
      if (i > 0) {
        forUpdateStatement.separators().add(firstTokenBefore(o, TerminalToken.TokenNameCOMMA));
      }
      forUpdateStatement.add(new ExpressionStatementTreeImpl(convertExpression(o), null));
    }

    final int firstSemicolonTokenIndex = e.initializers().isEmpty()
      ? tokenManager.firstIndexIn(e, TerminalToken.TokenNameSEMICOLON)
      : tokenManager.firstIndexAfter((ASTNode) e.initializers().get(e.initializers().size() - 1), TerminalToken.TokenNameSEMICOLON);
    Expression expression = e.getExpression();
    final int secondSemicolonTokenIndex = expression == null
      ? nextTokenIndex(firstSemicolonTokenIndex, TerminalToken.TokenNameSEMICOLON)
      : tokenManager.firstIndexAfter(expression, TerminalToken.TokenNameSEMICOLON);

    return new ForStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamefor),
      firstTokenIn(e, TerminalToken.TokenNameLPAREN),
      forInitStatement,
      createSyntaxToken(firstSemicolonTokenIndex),
      expression == null ? null : convertExpression(expression),
      createSyntaxToken(secondSemicolonTokenIndex),
      forUpdateStatement,
      firstTokenBefore(e.getBody(), TerminalToken.TokenNameRPAREN),
      createStatement(e.getBody())
    );
  }

  private WhileStatementTreeImpl convertWhile(WhileStatement e) {
    Expression expression = e.getExpression();
    return new WhileStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamewhile),
      firstTokenBefore(expression, TerminalToken.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalToken.TokenNameRPAREN),
      createStatement(e.getBody())
    );
  }

  private IfStatementTreeImpl convertIf(IfStatement e) {
    Expression expression = e.getExpression();
    Statement thenStatement = e.getThenStatement();
    Statement elseStatement = e.getElseStatement();
    return new IfStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameif),
      firstTokenBefore(expression, TerminalToken.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalToken.TokenNameRPAREN),
      createStatement(thenStatement),
      elseStatement == null ? null : firstTokenAfter(thenStatement, TerminalToken.TokenNameelse),
      elseStatement == null ? null : createStatement(elseStatement)
    );
  }

  private BreakStatementTreeImpl convertBreak(BreakStatement e) {
    IdentifierTreeImpl identifier = e.getLabel() == null ? null : createSimpleName(e.getLabel());
    usageLabel(identifier);
    return new BreakStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamebreak),
      identifier,
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private DoWhileStatementTreeImpl convertDoWhile(DoStatement e) {
    Statement body = e.getBody();
    Expression expression = e.getExpression();
    return new DoWhileStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamedo),
      createStatement(body),
      firstTokenAfter(body, TerminalToken.TokenNamewhile),
      firstTokenBefore(expression, TerminalToken.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalToken.TokenNameRPAREN),
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private AssertStatementTreeImpl convertAssert(AssertStatement e) {
    Expression message = e.getMessage();
    AssertStatementTreeImpl t = new AssertStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameassert),
      convertExpression(e.getExpression()),
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
    if (message != null) {
      t.complete(firstTokenBefore(message, TerminalToken.TokenNameCOLON), convertExpression(message));
    }
    return t;
  }

  private SwitchStatementTreeImpl convertSwitchStatement(SwitchStatement e) {
    Expression expression = e.getExpression();
    return new SwitchStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameswitch),
      firstTokenBefore(expression, TerminalToken.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalToken.TokenNameRPAREN),
      firstTokenAfter(expression, TerminalToken.TokenNameLBRACE),
      convertSwitchStatements(e.statements()),
      lastTokenIn(e, TerminalToken.TokenNameRBRACE)
    );
  }

  private List<CaseGroupTreeImpl> convertSwitchStatements(List<?> list) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> caselabels = null;
    StatementListTreeImpl body = null;

    for (Object o : list) {
      if (o instanceof SwitchCase c) {
        if (caselabels == null) {
          caselabels = new ArrayList<>();
          body = StatementListTreeImpl.emptyList();
        }

        List<ExpressionTree> expressions = new ArrayList<>();
        for (Object oo : c.expressions()) {
          expressions.add(convertExpressionFromCase((Expression) oo));
        }

        caselabels.add(new CaseLabelTreeImpl(
          firstTokenIn(c, c.isDefault() ? TerminalToken.TokenNamedefault : TerminalToken.TokenNamecase),
          expressions,
          lastTokenIn(c, /* TerminalToken.TokenNameCOLON or TerminalToken.TokenNameARROW */ ANY_TOKEN)
        ));
      } else {
        if (caselabels != null) {
          groups.add(new CaseGroupTreeImpl(caselabels, body));
        }
        caselabels = null;
        addStatementToList((Statement) o, Objects.requireNonNull(body));
      }
    }
    if (caselabels != null) {
      groups.add(new CaseGroupTreeImpl(caselabels, body));
    }
    return groups;
  }

  private ExpressionTree convertExpressionFromCase(Expression e) {
    if (e.getNodeType() == ASTNode.CASE_DEFAULT_EXPRESSION) {
      return new DefaultPatternTreeImpl(firstTokenIn(e, TerminalToken.TokenNamedefault), e.resolveTypeBinding());
    }
    if (e.getNodeType() == ASTNode.NULL_LITERAL) {
      return new NullPatternTreeImpl((LiteralTreeImpl) convertExpression(e));
    }
    if (e instanceof Pattern pattern) {
      return convertPattern(pattern);
    }
    return convertExpression(e);
  }

  private PatternTree convertPattern(Pattern p) {
    switch (p.getNodeType()) {
      case ASTNode.TYPE_PATTERN:
        TypePattern typePattern = (TypePattern) p;
        return new TypePatternTreeImpl(convertVariable(typePattern), typePattern.resolveTypeBinding());
      case ASTNode.RECORD_PATTERN:
        RecordPattern recordPattern = (RecordPattern) p;
        List<PatternTree> nestedPatterns = recordPattern.patterns().stream()
          .map(this::convertPattern)
          .toList();

        TypeTree patternType = convertType(recordPattern.getPatternType());
        var openParenToken = firstTokenIn(recordPattern, TerminalToken.TokenNameLPAREN);
        var closeParenToken = lastTokenIn(recordPattern, TerminalToken.TokenNameRPAREN);
        return new RecordPatternTreeImpl(patternType, openParenToken, nestedPatterns, closeParenToken);
      case ASTNode.GUARDED_PATTERN:
        GuardedPattern g = (GuardedPattern) p;
        return new GuardedPatternTreeImpl(
          convertPattern(g.getPattern()),
          firstTokenBefore(g.getExpression(), TerminalToken.TokenNameRestrictedIdentifierWhen),
          convertExpression(g.getExpression()),
          g.resolveTypeBinding());
      case ASTNode.NULL_PATTERN:
        // It is not clear how to reach this one, it seems to be possible only with badly constructed AST
        // fall-through. Do nothing for now.
      default:
        // JEP-405 (not released as part of any JDK yet): ArrayPattern, RecordPattern
        throw new IllegalStateException(ASTNode.nodeClassForType(p.getNodeType()).toString());
    }
  }

  private SynchronizedStatementTreeImpl convertSynchronized(SynchronizedStatement e) {
    Expression expression = e.getExpression();
    return new SynchronizedStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamesynchronized),
      firstTokenBefore(expression, TerminalToken.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalToken.TokenNameRPAREN),
      convertBlock(e.getBody())
    );
  }

  private ExpressionStatementTreeImpl convertExpressionStatement(ExpressionStatement e) {
    return new ExpressionStatementTreeImpl(
      convertExpression(e.getExpression()),
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private ContinueStatementTreeImpl convertContinue(ContinueStatement e) {
    SimpleName label = e.getLabel();
    IdentifierTreeImpl i = label == null ? null : createSimpleName(label);
    usageLabel(i);
    return new ContinueStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamecontinue),
      i,
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private LabeledStatementTreeImpl convertLabel(LabeledStatement e) {
    IdentifierTreeImpl i = createSimpleName(e.getLabel());

    JLabelSymbol symbol = new JLabelSymbol(i.name());
    labels.push(symbol);

    LabeledStatementTreeImpl t = new LabeledStatementTreeImpl(
      i,
      firstTokenAfter(e.getLabel(), TerminalToken.TokenNameCOLON),
      createStatement(e.getBody())
    );

    labels.pop();
    symbol.declaration = t;
    t.labelSymbol = symbol;
    return t;
  }

  private ForEachStatementImpl convertForeach(EnhancedForStatement e) {
    SingleVariableDeclaration parameter = e.getParameter();
    Expression expression = e.getExpression();
    return new ForEachStatementImpl(
      firstTokenIn(e, TerminalToken.TokenNamefor),
      firstTokenBefore(parameter, TerminalToken.TokenNameLPAREN),
      convertVariable(parameter),
      firstTokenAfter(parameter, TerminalToken.TokenNameCOLON),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalToken.TokenNameRPAREN),
      createStatement(e.getBody())
    );
  }

  private ThrowStatementTreeImpl convertThrow(ThrowStatement e) {
    return new ThrowStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNamethrow),
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalToken.TokenNameSEMICOLON)
    );
  }

  private TryStatementTreeImpl convertTry(TryStatement e) {
    ResourceListTreeImpl resources = convertResources(e);
    List<CatchTree> catches = convertCatchClauses(e);

    Block f = e.getFinally();
    return new TryStatementTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNametry),
      resources.isEmpty() ? null : firstTokenIn(e, TerminalToken.TokenNameLPAREN),
      resources,
      resources.isEmpty() ? null : firstTokenBefore(e.getBody(), TerminalToken.TokenNameRPAREN),
      convertBlock(e.getBody()),
      catches,
      f == null ? null : firstTokenBefore(f, TerminalToken.TokenNamefinally),
      f == null ? null : convertBlock(f)
    );
  }

  private ResourceListTreeImpl convertResources(TryStatement e) {
    List r = e.resources();
    ResourceListTreeImpl resources = ResourceListTreeImpl.emptyList();
    for (int i = 0; i < r.size(); i++) {
      Expression o = (Expression) r.get(i);
      if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
        addVariableToList((VariableDeclarationExpression) o, resources);
      } else {
        resources.add(convertExpression(o));
      }
      addSeparatorToList(e, o, resources.separators(), i < e.resources().size() - 1);
    }
    return resources;
  }

  private void addSeparatorToList(TryStatement tryStatement, Expression resource, List<SyntaxToken> separators, boolean isLast) {
    if (isLast) {
      separators.add(firstTokenAfter(resource, TerminalToken.TokenNameSEMICOLON));
    } else {
      int tokenIndex = tokenManager.firstIndexBefore(tryStatement.getBody(), TerminalToken.TokenNameRPAREN);
      while (true) {
        Token token;
        do {
          tokenIndex--;
          token = tokenManager.get(tokenIndex);
        } while (isComment(token));

        if (token.tokenType != TerminalToken.TokenNameSEMICOLON) {
          break;
        }
        separators.add(createSyntaxToken(tokenIndex));
      }
    }
  }

  private List<CatchTree> convertCatchClauses(TryStatement e) {
    List<CatchTree> catches = new ArrayList<>();
    for (Object o : e.catchClauses()) {
      CatchClause c = (CatchClause) o;
      catches.add(new CatchTreeImpl(
        firstTokenIn(c, TerminalToken.TokenNamecatch),
        firstTokenBefore(c.getException(), TerminalToken.TokenNameLPAREN),
        convertVariable(c.getException()),
        firstTokenAfter(c.getException(), TerminalToken.TokenNameRPAREN),
        convertBlock(c.getBody())
      ));
    }
    return catches;
  }

  private ExpressionStatementTreeImpl convertConstructorInvocation(ConstructorInvocation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      e.arguments().isEmpty() ? lastTokenIn(e, TerminalToken.TokenNameLPAREN) : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalToken.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalToken.TokenNameRPAREN)
    );

    IdentifierTreeImpl i = new IdentifierTreeImpl(e.arguments().isEmpty()
      ? lastTokenIn(e, TerminalToken.TokenNamethis)
      : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalToken.TokenNamethis));
    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      i,
      convertTypeArguments(e.typeArguments()),
      arguments
    );
    t.methodBinding = e.resolveConstructorBinding();
    if (t.methodBinding != null) {
      t.typeBinding = t.methodBinding.getDeclaringClass();
      t.methodBinding = excludeRecovery(t.methodBinding, arguments.size());
    }
    i.binding = t.methodBinding;
    usage(i.binding, i);
    return new ExpressionStatementTreeImpl(
      t,
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private ExpressionStatementTreeImpl convertSuperConstructorInvocation(SuperConstructorInvocation e) {
    IdentifierTreeImpl i = new IdentifierTreeImpl(firstTokenIn(e, TerminalToken.TokenNamesuper));
    ExpressionTree methodSelect = i;
    if (e.getExpression() != null) {
      methodSelect = new MemberSelectExpressionTreeImpl(
        convertExpression(e.getExpression()),
        firstTokenAfter(e.getExpression(), TerminalToken.TokenNameDOT),
        i
      );
    }

    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenIn(e, TerminalToken.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalToken.TokenNameRPAREN)
    );

    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      methodSelect,
      convertTypeArguments(e.typeArguments()),
      arguments
    );
    t.methodBinding = e.resolveConstructorBinding();
    if (t.methodBinding != null) {
      t.typeBinding = t.methodBinding.getDeclaringClass();
      t.methodBinding = excludeRecovery(t.methodBinding, arguments.size());
    }
    i.binding = t.methodBinding;
    usage(i.binding, i);
    return new ExpressionStatementTreeImpl(
      t,
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private YieldStatementTreeImpl convertYield(YieldStatement e) {
    InternalSyntaxToken yieldKeyword = null;
    if (!e.isImplicit()) {
      yieldKeyword = firstTokenIn(e, TerminalToken.TokenNameRestrictedIdentifierYield);
    }
    return new YieldStatementTreeImpl(
      yieldKeyword,
      convertExpression(e.getExpression()),
      lastTokenIn(e, TerminalToken.TokenNameSEMICOLON)
    );
  }

  private ExpressionTree convertExpression(Expression node) {
    ExpressionTree t = createExpression(node);
    ((AbstractTypedTree) t).typeBinding = node.resolveTypeBinding();
    return t;
  }

  private ExpressionTree createExpression(Expression node) {
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME:
        return convertSimpleName((SimpleName) node);
      case ASTNode.QUALIFIED_NAME:
        return convertQualifiedName((QualifiedName) node);
      case ASTNode.FIELD_ACCESS:
        return convertFieldAccess((FieldAccess) node);
      case ASTNode.SUPER_FIELD_ACCESS:
        return convertFieldAccess((SuperFieldAccess) node);
      case ASTNode.THIS_EXPRESSION:
        return convertThisExpression((ThisExpression) node);
      case ASTNode.ARRAY_ACCESS:
        return convertArrayAccess((ArrayAccess) node);
      case ASTNode.ARRAY_CREATION:
        return convertArrayCreation((ArrayCreation) node);
      case ASTNode.ARRAY_INITIALIZER:
        return convertArrayInitializer((ArrayInitializer) node);
      case ASTNode.ASSIGNMENT:
        return convertAssignment((Assignment) node);
      case ASTNode.CAST_EXPRESSION:
        return convertTypeCastExpression((CastExpression) node);
      case ASTNode.CLASS_INSTANCE_CREATION:
        return convertClassInstanceCreation((ClassInstanceCreation) node);
      case ASTNode.CONDITIONAL_EXPRESSION:
        return convertConditionalExpression((ConditionalExpression) node);
      case ASTNode.INFIX_EXPRESSION:
        return convertInfixExpression((InfixExpression) node);
      case ASTNode.METHOD_INVOCATION:
        return convertMethodInvocation((MethodInvocation) node);
      case ASTNode.SUPER_METHOD_INVOCATION:
        return convertMethodInvocation((SuperMethodInvocation) node);
      case ASTNode.PARENTHESIZED_EXPRESSION:
        return convertParenthesizedExpression((ParenthesizedExpression) node);
      case ASTNode.POSTFIX_EXPRESSION:
        return convertPostfixExpression((PostfixExpression) node);
      case ASTNode.PREFIX_EXPRESSION:
        return convertPrefixExpression((PrefixExpression) node);
      case ASTNode.INSTANCEOF_EXPRESSION:
        return convertInstanceOf((InstanceofExpression) node);
      case ASTNode.PATTERN_INSTANCEOF_EXPRESSION:
        return convertInstanceOf((PatternInstanceofExpression) node);
      case ASTNode.LAMBDA_EXPRESSION:
        return convertLambdaExpression((LambdaExpression) node);
      case ASTNode.CREATION_REFERENCE:
        return convertMethodReference((CreationReference) node);
      case ASTNode.EXPRESSION_METHOD_REFERENCE:
        return convertMethodReference((ExpressionMethodReference) node);
      case ASTNode.TYPE_METHOD_REFERENCE:
        return convertMethodReference((TypeMethodReference) node);
      case ASTNode.SUPER_METHOD_REFERENCE:
        return convertMethodReference((SuperMethodReference) node);
      case ASTNode.SWITCH_EXPRESSION:
        return convertSwitchExpression((SwitchExpression) node);
      case ASTNode.TYPE_LITERAL:
        return convertTypeLiteral((TypeLiteral) node);
      case ASTNode.NULL_LITERAL:
        return convertLiteral((NullLiteral) node);
      case ASTNode.NUMBER_LITERAL:
        return convertLiteral((NumberLiteral) node);
      case ASTNode.CHARACTER_LITERAL:
        return convertLiteral((CharacterLiteral) node);
      case ASTNode.BOOLEAN_LITERAL:
        return convertLiteral((BooleanLiteral) node);
      case ASTNode.STRING_LITERAL:
        return convertLiteral((StringLiteral) node);
      case ASTNode.TEXT_BLOCK:
        return convertTextBlock((TextBlock) node);
      case ASTNode.NORMAL_ANNOTATION,
        ASTNode.MARKER_ANNOTATION,
        ASTNode.SINGLE_MEMBER_ANNOTATION:
        return convertAnnotation((Annotation) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private IdentifierTreeImpl convertSimpleName(SimpleName e) {
    IdentifierTreeImpl t = createSimpleName(e);
    usage(t.binding, t);
    return t;
  }

  private MemberSelectExpressionTreeImpl convertQualifiedName(QualifiedName e) {
    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    usage(rhs.binding, rhs);
    return new MemberSelectExpressionTreeImpl(
      convertExpression(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
      rhs
    );
  }

  private MemberSelectExpressionTreeImpl convertFieldAccess(FieldAccess e) {
    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    usage(rhs.binding, rhs);
    return new MemberSelectExpressionTreeImpl(
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalToken.TokenNameDOT),
      rhs
    );
  }

  private MemberSelectExpressionTreeImpl convertFieldAccess(SuperFieldAccess e) {
    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    usage(rhs.binding, rhs);
    if (e.getQualifier() == null) {
      // super.name
      return new MemberSelectExpressionTreeImpl(
        unqualifiedKeywordSuper(e),
        firstTokenIn(e, TerminalToken.TokenNameDOT),
        rhs
      );
    }
    // qualifier.super.name
    AbstractTypedTree qualifier = (AbstractTypedTree) convertExpression(e.getQualifier());
    KeywordSuper keywordSuper = new KeywordSuper(firstTokenAfter(e.getQualifier(), TerminalToken.TokenNamesuper), null);
    MemberSelectExpressionTreeImpl qualifiedSuper = new MemberSelectExpressionTreeImpl(
      (ExpressionTree) qualifier,
      firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
      keywordSuper
    );
    if (qualifier.typeBinding != null) {
      keywordSuper.typeBinding = qualifier.typeBinding;
      qualifiedSuper.typeBinding = keywordSuper.typeBinding.getSuperclass();
    }
    return new MemberSelectExpressionTreeImpl(
      qualifiedSuper,
      firstTokenBefore(e.getName(), TerminalToken.TokenNameDOT),
      rhs
    );
  }

  private ExpressionTree convertThisExpression(ThisExpression e) {
    if (e.getQualifier() == null) {
      return new KeywordThis(firstTokenIn(e, TerminalToken.TokenNamethis), null);
    }
    KeywordThis keywordThis = new KeywordThis(
      firstTokenAfter(e.getQualifier(), TerminalToken.TokenNamethis),
      e.resolveTypeBinding()
    );
    return new MemberSelectExpressionTreeImpl(
      convertExpression(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
      keywordThis
    );
  }

  private MemberSelectExpressionTreeImpl convertTypeLiteral(TypeLiteral e) {
    return new MemberSelectExpressionTreeImpl(
      (ExpressionTree) convertType(e.getType()),
      lastTokenIn(e, TerminalToken.TokenNameDOT),
      new IdentifierTreeImpl(
        lastTokenIn(e, TerminalToken.TokenNameclass)
      )
    );
  }

  private ArrayAccessExpressionTreeImpl convertArrayAccess(ArrayAccess e) {
    Expression index = e.getIndex();
    return new ArrayAccessExpressionTreeImpl(
      convertExpression(e.getArray()),
      new ArrayDimensionTreeImpl(
        firstTokenBefore(index, TerminalToken.TokenNameLBRACKET),
        convertExpression(index),
        firstTokenAfter(index, TerminalToken.TokenNameRBRACKET)
      )
    );
  }

  private NewArrayTreeImpl convertArrayCreation(ArrayCreation e) {
    List<ArrayDimensionTree> dimensions = new ArrayList<>();
    for (Object o : e.dimensions()) {
      dimensions.add(new ArrayDimensionTreeImpl(
        firstTokenBefore((Expression) o, TerminalToken.TokenNameLBRACKET),
        convertExpression((Expression) o),
        firstTokenAfter((Expression) o, TerminalToken.TokenNameRBRACKET)
      ));
    }
    InitializerListTreeImpl initializers = InitializerListTreeImpl.emptyList();
    if (e.getInitializer() != null) {
      assert dimensions.isEmpty();

      TypeTree type = convertType(e.getType());
      while (type.is(Tree.Kind.ARRAY_TYPE)) {
        ArrayTypeTree arrayType = (ArrayTypeTree) type;
        ArrayDimensionTreeImpl dimension = new ArrayDimensionTreeImpl(
          arrayType.openBracketToken(),
          null,
          arrayType.closeBracketToken()
        ).completeAnnotations(arrayType.annotations());
        dimensions.add(/* TODO suboptimal */ 0, dimension);
        type = arrayType.type();
      }

      return ((NewArrayTreeImpl) convertExpression(e.getInitializer()))
        .completeWithNewKeyword(firstTokenIn(e, TerminalToken.TokenNamenew))
        .complete(type)
        .completeDimensions(dimensions);
    }
    TypeTree type = convertType(e.getType());
    int index = dimensions.size() - 1;
    while (type.is(Tree.Kind.ARRAY_TYPE)) {
      if (!type.annotations().isEmpty()) {
        ((ArrayDimensionTreeImpl) dimensions.get(index))
          .completeAnnotations(type.annotations());
      }
      index--;
      type = ((ArrayTypeTree) type).type();
    }

    return new NewArrayTreeImpl(dimensions, initializers)
      .complete(type)
      .completeWithNewKeyword(firstTokenIn(e, TerminalToken.TokenNamenew));
  }

  private NewArrayTreeImpl convertArrayInitializer(ArrayInitializer e) {
    InitializerListTreeImpl initializers = InitializerListTreeImpl.emptyList();
    for (int i = 0; i < e.expressions().size(); i++) {
      Expression o = (Expression) e.expressions().get(i);
      initializers.add(convertExpression(o));
      final int commaTokenIndex = firstTokenIndexAfter(o);
      if (tokenManager.get(commaTokenIndex).tokenType == TerminalToken.TokenNameCOMMA) {
        initializers.separators().add(firstTokenAfter(o, TerminalToken.TokenNameCOMMA));
      }
    }
    return new NewArrayTreeImpl(Collections.emptyList(),initializers)
      .completeWithCurlyBraces(
      firstTokenIn(e, TerminalToken.TokenNameLBRACE),
      lastTokenIn(e, TerminalToken.TokenNameRBRACE)
      );
  }

  private AssignmentExpressionTreeImpl convertAssignment(Assignment e) {
    Op op = operators.get(e.getOperator());
    return new AssignmentExpressionTreeImpl(
      op.kind,
      convertExpression(e.getLeftHandSide()),
      firstTokenAfter(e.getLeftHandSide(), op.tokenType),
      convertExpression(e.getRightHandSide())
    );
  }

  private TypeCastExpressionTreeImpl convertTypeCastExpression(CastExpression e) {
    Type type = e.getType();
    if (type.getNodeType() == ASTNode.INTERSECTION_TYPE) {
      List intersectionTypes = ((IntersectionType) type).types();
      QualifiedIdentifierListTreeImpl bounds = QualifiedIdentifierListTreeImpl.emptyList();
      for (int i = 1; i < intersectionTypes.size(); i++) {
        Type o = (Type) intersectionTypes.get(i);
        bounds.add(convertType(o));
        if (i < intersectionTypes.size() - 1) {
          bounds.separators().add(firstTokenAfter(o, TerminalToken.TokenNameAND));
        }
      }
      return new TypeCastExpressionTreeImpl(
        firstTokenBefore(type, TerminalToken.TokenNameLPAREN),
        convertType((Type) intersectionTypes.get(0)),
        firstTokenAfter((Type) intersectionTypes.get(0), TerminalToken.TokenNameAND),
        bounds,
        firstTokenAfter(type, TerminalToken.TokenNameRPAREN),
        convertExpression(e.getExpression())
      );
    }
    return new TypeCastExpressionTreeImpl(
      firstTokenBefore(type, TerminalToken.TokenNameLPAREN),
      convertType(type),
      firstTokenAfter(type, TerminalToken.TokenNameRPAREN),
      convertExpression(e.getExpression())
    );
  }

  private NewClassTreeImpl convertClassInstanceCreation(ClassInstanceCreation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenAfter(e.getType(), TerminalToken.TokenNameLPAREN),
      e.arguments(),
      firstTokenAfter(e.arguments().isEmpty() ? e.getType() : (ASTNode) e.arguments().get(e.arguments().size() - 1), TerminalToken.TokenNameRPAREN)
    );

    ClassTreeImpl classBody = null;
    if (e.getAnonymousClassDeclaration() != null) {
      List<Tree> members = new ArrayList<>();
      for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
        processBodyDeclaration((BodyDeclaration) o, members);
      }
      classBody = new ClassTreeImpl(
        Tree.Kind.CLASS,
        firstTokenIn(e.getAnonymousClassDeclaration(), TerminalToken.TokenNameLBRACE),
        members,
        lastTokenIn(e.getAnonymousClassDeclaration(), TerminalToken.TokenNameRBRACE)
      );
      classBody.typeBinding = e.getAnonymousClassDeclaration().resolveBinding();
      declaration(classBody.typeBinding, classBody);
    }

    NewClassTreeImpl t = new NewClassTreeImpl(
      convertType(e.getType()),
      arguments,
      classBody
    ).completeWithNewKeyword(
      e.getExpression() == null ? firstTokenIn(e, TerminalToken.TokenNamenew) : firstTokenAfter(e.getExpression(), TerminalToken.TokenNamenew)
    ).completeWithTypeArguments(
      convertTypeArguments(e.typeArguments())
    );
    if (e.getExpression() != null) {
      t.completeWithEnclosingExpression(convertExpression(e.getExpression()));
      t.completeWithDotToken(firstTokenAfter(e.getExpression(), TerminalToken.TokenNameDOT));
    }

    IdentifierTreeImpl i = (IdentifierTreeImpl) t.getConstructorIdentifier();
    int nbArguments = arguments.size();
    if (e.getAnonymousClassDeclaration() == null) {
      i.binding = excludeRecovery(e.resolveConstructorBinding(), nbArguments);
    } else {
      i.binding = excludeRecovery(findConstructorForAnonymousClass(e.getAST(), i.typeBinding, e.resolveConstructorBinding()), nbArguments);
    }
    usage(i.binding, i);

    return t;
  }

  private ConditionalExpressionTreeImpl convertConditionalExpression(ConditionalExpression e) {
    return new ConditionalExpressionTreeImpl(
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalToken.TokenNameQUESTION),
      convertExpression(e.getThenExpression()),
      firstTokenAfter(e.getThenExpression(), TerminalToken.TokenNameCOLON),
      convertExpression(e.getElseExpression())
    );
  }

  private BinaryExpressionTreeImpl convertInfixExpression(InfixExpression e) {
    Op op = operators.get(e.getOperator());
    BinaryExpressionTreeImpl t = new BinaryExpressionTreeImpl(
      op.kind,
      convertExpression(e.getLeftOperand()),
      firstTokenAfter(e.getLeftOperand(), op.tokenType),
      convertExpression(e.getRightOperand())
    );
    for (Object o : e.extendedOperands()) {
      Expression e2 = (Expression) o;
      t.typeBinding = e.resolveTypeBinding();
      t = new BinaryExpressionTreeImpl(
        op.kind,
        t,
        firstTokenBefore(e2, op.tokenType),
        convertExpression(e2)
      );
    }
    return t;
  }

  private MethodInvocationTreeImpl convertMethodInvocation(MethodInvocation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenAfter(e.getName(), TerminalToken.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalToken.TokenNameRPAREN)
    );

    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    ExpressionTree memberSelect;
    if (e.getExpression() == null) {
      memberSelect = rhs;
    } else {
      memberSelect = new MemberSelectExpressionTreeImpl(
        convertExpression(e.getExpression()),
        firstTokenAfter(e.getExpression(), TerminalToken.TokenNameDOT),
        rhs
      );
    }
    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      memberSelect,
      convertTypeArguments(e.typeArguments()),
      arguments
    );
    t.methodBinding = excludeRecovery(e.resolveMethodBinding(), arguments.size());
    rhs.binding = t.methodBinding;
    usage(rhs.binding, rhs);
    return t;
  }

  private MethodInvocationTreeImpl convertMethodInvocation(SuperMethodInvocation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenIn(e, TerminalToken.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalToken.TokenNameRPAREN)
    );

    IdentifierTreeImpl rhs = createSimpleName(e.getName());

    ExpressionTree outermostSelect;
    if (e.getQualifier() == null) {
      outermostSelect = new MemberSelectExpressionTreeImpl(
        unqualifiedKeywordSuper(e),
        firstTokenIn(e, TerminalToken.TokenNameDOT),
        rhs
      );
    } else {
      final int firstDotTokenIndex = tokenManager.firstIndexAfter(e.getQualifier(), TerminalToken.TokenNameDOT);
      AbstractTypedTree qualifier = (AbstractTypedTree) convertExpression(e.getQualifier());
      KeywordSuper keywordSuper = new KeywordSuper(firstTokenAfter(e.getQualifier(), TerminalToken.TokenNamesuper), null);
      MemberSelectExpressionTreeImpl qualifiedSuper = new MemberSelectExpressionTreeImpl(
        (ExpressionTree) qualifier,
        createSyntaxToken(firstDotTokenIndex),
        keywordSuper
      );
      if (qualifier.typeBinding != null) {
        keywordSuper.typeBinding = qualifier.typeBinding;
        qualifiedSuper.typeBinding = keywordSuper.typeBinding.getSuperclass();
      }
      outermostSelect = new MemberSelectExpressionTreeImpl(
        qualifiedSuper,
        createSyntaxToken(nextTokenIndex(firstDotTokenIndex, TerminalToken.TokenNameDOT)),
        rhs
      );
    }

    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      outermostSelect,
      null,
      arguments
    );
    t.methodBinding = excludeRecovery(e.resolveMethodBinding(), arguments.size());
    rhs.binding = t.methodBinding;
    usage(rhs.binding, rhs);
    return t;
  }

  private ParenthesizedTreeImpl convertParenthesizedExpression(ParenthesizedExpression e) {
    return new ParenthesizedTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameLPAREN),
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalToken.TokenNameRPAREN)
    );
  }

  private InternalPostfixUnaryExpression convertPostfixExpression(PostfixExpression e) {
    Op op = operators.get(e.getOperator());
    return new InternalPostfixUnaryExpression(
      op.kind,
      convertExpression(e.getOperand()),
      firstTokenAfter(e.getOperand(), op.tokenType)
    );
  }

  private InternalPrefixUnaryExpression convertPrefixExpression(PrefixExpression e) {
    Op op = operators.get(e.getOperator());
    return new InternalPrefixUnaryExpression(
      op.kind,
      firstTokenIn(e, op.tokenType),
      convertExpression(e.getOperand())
    );
  }

  private InstanceOfTreeImpl convertInstanceOf(InstanceofExpression e) {
    Expression leftOperand = e.getLeftOperand();
    InternalSyntaxToken instanceofToken = firstTokenAfter(leftOperand, TerminalToken.TokenNameinstanceof);
    return new InstanceOfTreeImpl(convertExpression(leftOperand), instanceofToken, convertType(e.getRightOperand()));
  }

  private InstanceOfTreeImpl convertInstanceOf(PatternInstanceofExpression e) {
    Expression leftOperand = e.getLeftOperand();
    InternalSyntaxToken instanceofToken = firstTokenAfter(leftOperand, TerminalToken.TokenNameinstanceof);
    return new InstanceOfTreeImpl(convertExpression(leftOperand), instanceofToken, convertPattern(e.getPattern()));
  }

  private LambdaExpressionTreeImpl convertLambdaExpression(LambdaExpression e) {
    List<VariableTree> parameters = new ArrayList<>();
    for (int i = 0; i < e.parameters().size(); i++) {
      VariableDeclaration o = (VariableDeclaration) e.parameters().get(i);
      VariableTreeImpl t;
      if (o.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
        t = new VariableTreeImpl(createSimpleName(o.getName()));
        IVariableBinding variableBinding = o.resolveBinding();
        if (variableBinding != null) {
          t.variableBinding = variableBinding;
          ((InferedTypeTree) t.type()).typeBinding = variableBinding.getType();
          declaration(t.variableBinding, t);
        }
      } else {
        t = convertVariable((SingleVariableDeclaration) o);
      }
      parameters.add(t);
      if (i < e.parameters().size() - 1) {
        t.setEndToken(firstTokenAfter(o, TerminalToken.TokenNameCOMMA));
      }
    }
    ASTNode body = e.getBody();
    var tree = new LambdaExpressionTreeImpl(
      e.hasParentheses() ? firstTokenIn(e, TerminalToken.TokenNameLPAREN) : null,
      parameters,
      e.hasParentheses() ? firstTokenBefore(body, TerminalToken.TokenNameRPAREN) : null,
      firstTokenBefore(body, TerminalToken.TokenNameARROW),
      body.getNodeType() == ASTNode.BLOCK ? convertBlock((Block) body) : convertExpression((Expression) body)
    );
    tree.methodBinding = e.resolveMethodBinding();
    return tree;
  }

  private MethodReferenceTreeImpl convertMethodReference(CreationReference e) {
    MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
      convertType(e.getType()),
      lastTokenIn(e, TerminalToken.TokenNameCOLON_COLON)
    );
    IdentifierTreeImpl i = new IdentifierTreeImpl(lastTokenIn(e, TerminalToken.TokenNamenew));
    i.binding = e.resolveMethodBinding();
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private MethodReferenceTreeImpl convertMethodReference(ExpressionMethodReference e) {
    MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalToken.TokenNameCOLON_COLON)
    );
    IdentifierTreeImpl i = createSimpleName(e.getName());
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private MethodReferenceTreeImpl convertMethodReference(TypeMethodReference e) {
    MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
      convertType(e.getType()),
      firstTokenAfter(e.getType(), TerminalToken.TokenNameCOLON_COLON)
    );
    IdentifierTreeImpl i = createSimpleName(e.getName());
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private MethodReferenceTreeImpl convertMethodReference(SuperMethodReference e) {
    MethodReferenceTreeImpl t;
    if (e.getQualifier() != null) {
      t = new MethodReferenceTreeImpl(
        new MemberSelectExpressionTreeImpl(
          convertExpression(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
          unqualifiedKeywordSuper(e)
        ),
        firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameCOLON_COLON)
      );
    } else {
      t = new MethodReferenceTreeImpl(
        unqualifiedKeywordSuper(e),
        firstTokenIn(e, TerminalToken.TokenNameCOLON_COLON)
      );
    }
    IdentifierTreeImpl i = createSimpleName(e.getName());
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private SwitchExpressionTreeImpl convertSwitchExpression(SwitchExpression e) {
    Expression expr = e.getExpression();
    return new SwitchExpressionTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameswitch),
      firstTokenIn(e, TerminalToken.TokenNameLPAREN),
      convertExpression(expr),
      firstTokenAfter(expr, TerminalToken.TokenNameRPAREN),
      firstTokenAfter(expr, TerminalToken.TokenNameLBRACE),
      convertSwitchStatements(e.statements()),
      lastTokenIn(e, TerminalToken.TokenNameRBRACE)
    );
  }

  private LiteralTreeImpl convertLiteral(NullLiteral e) {
    return new LiteralTreeImpl(Tree.Kind.NULL_LITERAL, firstTokenIn(e, TerminalToken.TokenNamenull));
  }

  private ExpressionTree convertLiteral(NumberLiteral e) {
    int tokenIndex = tokenManager.findIndex(e.getStartPosition(), ANY_TOKEN, true);
    TerminalToken tokenType = tokenManager.get(tokenIndex).tokenType;
    boolean unaryMinus = tokenType == TerminalToken.TokenNameMINUS;
    if (unaryMinus) {
      tokenIndex++;
      tokenType = tokenManager.get(tokenIndex).tokenType;
    }
    ExpressionTree result;
    switch (tokenType) {
      case TokenNameIntegerLiteral:
        result = new LiteralTreeImpl(Tree.Kind.INT_LITERAL, createSyntaxToken(tokenIndex));
        break;
      case TokenNameLongLiteral:
        result = new LiteralTreeImpl(Tree.Kind.LONG_LITERAL, createSyntaxToken(tokenIndex));
        break;
      case TokenNameFloatingPointLiteral:
        result = new LiteralTreeImpl(Tree.Kind.FLOAT_LITERAL, createSyntaxToken(tokenIndex));
        break;
      case TokenNameDoubleLiteral:
        result = new LiteralTreeImpl(Tree.Kind.DOUBLE_LITERAL, createSyntaxToken(tokenIndex));
        break;
      default:
        throw new IllegalStateException();
    }
    ((LiteralTreeImpl) result).typeBinding = e.resolveTypeBinding();
    if (unaryMinus) {
      return new InternalPrefixUnaryExpression(Tree.Kind.UNARY_MINUS, createSyntaxToken(tokenIndex - 1), result);
    }
    return result;
  }

  private LiteralTreeImpl convertLiteral(CharacterLiteral e) {
    return new LiteralTreeImpl(Tree.Kind.CHAR_LITERAL, firstTokenIn(e, TerminalToken.TokenNameCharacterLiteral));
  }

  private LiteralTreeImpl convertLiteral(BooleanLiteral e) {
    InternalSyntaxToken value = firstTokenIn(e, e.booleanValue() ? TerminalToken.TokenNametrue : TerminalToken.TokenNamefalse);
    return new LiteralTreeImpl(Tree.Kind.BOOLEAN_LITERAL, value);
  }

  private LiteralTreeImpl convertLiteral(StringLiteral e) {
    return new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, firstTokenIn(e, TerminalToken.TokenNameStringLiteral));
  }

  private LiteralTreeImpl convertTextBlock(TextBlock e) {
    return new LiteralTreeImpl(Tree.Kind.TEXT_BLOCK, firstTokenIn(e, TerminalToken.TokenNameTextBlock));
  }

  private AnnotationTreeImpl convertAnnotation(Annotation e) {
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    if (e.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
      arguments.add(convertExpression(((SingleMemberAnnotation) e).getValue()));
      arguments.complete(
        firstTokenIn(e, TerminalToken.TokenNameLPAREN),
        lastTokenIn(e, TerminalToken.TokenNameRPAREN)
      );
    } else if (e.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
      for (int i = 0; i < ((NormalAnnotation) e).values().size(); i++) {
        MemberValuePair o = (MemberValuePair) ((NormalAnnotation) e).values().get(i);
        arguments.add(new AssignmentExpressionTreeImpl(
          Tree.Kind.ASSIGNMENT,
          createSimpleName(o.getName()),
          firstTokenAfter(o.getName(), TerminalToken.TokenNameEQUAL),
          convertExpression(o.getValue())
        ));
        if (i < ((NormalAnnotation) e).values().size() - 1) {
          arguments.separators().add(firstTokenAfter(o, TerminalToken.TokenNameCOMMA));
        }
      }
      arguments.complete(
        firstTokenIn(e, TerminalToken.TokenNameLPAREN),
        lastTokenIn(e, TerminalToken.TokenNameRPAREN)
      );
    }
    return new AnnotationTreeImpl(
      firstTokenIn(e, TerminalToken.TokenNameAT),
      (TypeTree) convertExpression(e.getTypeName()),
      arguments
    );
  }

  private KeywordSuper unqualifiedKeywordSuper(ASTNode node) {
    InternalSyntaxToken token = firstTokenIn(node, TerminalToken.TokenNamesuper);
    do {
      if (node instanceof AbstractTypeDeclaration abstractTypeDeclaration) {
        return new KeywordSuper(token, abstractTypeDeclaration.resolveBinding());
      }
      if (node instanceof AnonymousClassDeclaration anonymousClassDeclaration) {
        return new KeywordSuper(token, anonymousClassDeclaration.resolveBinding());
      }
      node = node.getParent();
    } while (true);
  }

  private TypeTree convertType(Type node) {
    switch (node.getNodeType()) {
      case ASTNode.PRIMITIVE_TYPE:
        return convertPrimitiveType((PrimitiveType) node);
      case ASTNode.SIMPLE_TYPE:
        return convertSimpleType((SimpleType) node);
      case ASTNode.UNION_TYPE:
        return convertUnionType((UnionType) node);
      case ASTNode.ARRAY_TYPE:
        return convertArrayType((ArrayType) node);
      case ASTNode.PARAMETERIZED_TYPE:
        return convertParameterizedType((ParameterizedType) node);
      case ASTNode.QUALIFIED_TYPE:
        return convertQualifiedType((QualifiedType) node);
      case ASTNode.NAME_QUALIFIED_TYPE:
        return convertNamedQualifiedType((NameQualifiedType) node);
      case ASTNode.WILDCARD_TYPE:
        return convertWildcardType((WildcardType) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private JavaTree.PrimitiveTypeTreeImpl convertPrimitiveType(PrimitiveType e) {
    final JavaTree.PrimitiveTypeTreeImpl t;
    switch (e.getPrimitiveTypeCode().toString()) {
      case "byte":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNamebyte));
        break;
      case "short":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNameshort));
        break;
      case "char":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNamechar));
        break;
      case "int":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNameint));
        break;
      case "long":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNamelong));
        break;
      case "float":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNamefloat));
        break;
      case "double":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNamedouble));
        break;
      case "boolean":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNameboolean));
        break;
      case "void":
        t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalToken.TokenNamevoid));
        break;
      default:
        throw new IllegalStateException(e.getPrimitiveTypeCode().toString());
    }
    t.complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private JavaTree.AnnotatedTypeTree convertSimpleType(SimpleType e) {
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Object o : e.annotations()) {
      annotations.add((AnnotationTree) convertExpression(((Annotation) o)));
    }
    JavaTree.AnnotatedTypeTree t = e.isVar() ? convertVarType(e) : (JavaTree.AnnotatedTypeTree) convertExpression(e.getName());
    t.complete(annotations);
    // typeBinding is assigned by convertVarType or convertExpression
    return t;
  }

  private JavaTree.UnionTypeTreeImpl convertUnionType(UnionType e) {
    QualifiedIdentifierListTreeImpl alternatives = QualifiedIdentifierListTreeImpl.emptyList();
    for (int i = 0; i < e.types().size(); i++) {
      Type o = (Type) e.types().get(i);
      alternatives.add(convertType(o));
      if (i < e.types().size() - 1) {
        alternatives.separators().add(firstTokenAfter(o, TerminalToken.TokenNameOR));
      }
    }
    JavaTree.UnionTypeTreeImpl t = new JavaTree.UnionTypeTreeImpl(alternatives);
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private TypeTree convertArrayType(ArrayType e) {
    @Nullable ITypeBinding elementTypeBinding = e.getElementType().resolveBinding();
    TypeTree t = convertType(e.getElementType());
    int tokenIndex = tokenManager.firstIndexAfter(e.getElementType(), TerminalToken.TokenNameLBRACKET);
    for (int i = 0; i < e.dimensions().size(); i++) {
      if (i > 0) {
        tokenIndex = nextTokenIndex(tokenIndex, TerminalToken.TokenNameLBRACKET);
      }
      t = new JavaTree.ArrayTypeTreeImpl(
        t,
        (List) convertAnnotations(((Dimension) e.dimensions().get(i)).annotations()),
        createSyntaxToken(tokenIndex),
        createSyntaxToken(nextTokenIndex(tokenIndex, TerminalToken.TokenNameRBRACKET))
      );
      if (elementTypeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) t).typeBinding = elementTypeBinding.createArrayType(i + 1);
      }
    }
    return t;
  }

  private JavaTree.ParameterizedTypeTreeImpl convertParameterizedType(ParameterizedType e) {
    LineColumnConverter.Pos pos = lineColumnConverter.toPos(e.getStartPosition() + e.getLength() - 1);
    JavaTree.ParameterizedTypeTreeImpl t = new JavaTree.ParameterizedTypeTreeImpl(
      convertType(e.getType()),
      convertTypeArguments(
        firstTokenAfter(e.getType(), TerminalToken.TokenNameLESS),
        e.typeArguments(),
        new InternalSyntaxToken(
          pos.line(),
          pos.columnOffset(),
          ">",
          /* TODO */ Collections.emptyList(),
          false
        )
      )
    );
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private MemberSelectExpressionTreeImpl convertQualifiedType(QualifiedType e) {
    MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
      (ExpressionTree) convertType(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
      createSimpleName(e.getName())
    );
    ((IdentifierTreeImpl) t.identifier()).complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private MemberSelectExpressionTreeImpl convertNamedQualifiedType(NameQualifiedType e) {
    MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
      convertExpression(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalToken.TokenNameDOT),
      createSimpleName(e.getName())
    );
    ((IdentifierTreeImpl) t.identifier()).complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private JavaTree.WildcardTreeImpl convertWildcardType(WildcardType e) {
    final InternalSyntaxToken questionToken = e.annotations().isEmpty()
      ? firstTokenIn(e, TerminalToken.TokenNameQUESTION)
      : firstTokenAfter((ASTNode) e.annotations().get(e.annotations().size() - 1), TerminalToken.TokenNameQUESTION);
    JavaTree.WildcardTreeImpl t;
    Type bound = e.getBound();
    if (bound == null) {
      t = new JavaTree.WildcardTreeImpl(questionToken);
    } else {
      t = new JavaTree.WildcardTreeImpl(
        e.isUpperBound() ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
        e.isUpperBound() ? firstTokenBefore(bound, TerminalToken.TokenNameextends) : firstTokenBefore(bound, TerminalToken.TokenNamesuper),
        convertType(bound)
      ).complete(questionToken);
    }
    t.complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  @Nullable
  private static IMethodBinding excludeRecovery(@Nullable IMethodBinding methodBinding, int arguments) {
    if (methodBinding == null) {
      return null;
    }
    if (methodBinding.isVarargs()) {
      if (arguments + 1 < methodBinding.getParameterTypes().length) {
        return null;
      }
    } else {
      if (arguments != methodBinding.getParameterTypes().length) {
        return null;
      }
    }
    return methodBinding;
  }

  @Nullable
  private static IMethodBinding findConstructorForAnonymousClass(AST ast, @Nullable ITypeBinding typeBinding, @Nullable IMethodBinding methodBinding) {
    if (typeBinding == null || methodBinding == null) {
      return null;
    }
    if (typeBinding.isInterface()) {
      typeBinding = ast.resolveWellKnownType("java.lang.Object");
    }
    for (IMethodBinding m : typeBinding.getDeclaredMethods()) {
      if (methodBinding.isSubsignature(m)) {
        return m;
      }
    }
    return null;
  }

  private List<AnnotationTree> convertAnnotations(List<?> e) {
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Object o : e) {
      annotations.add((AnnotationTree) convertExpression(
        ((Annotation) o)
      ));
    }
    return annotations;
  }

  private ModifiersTreeImpl convertModifiers(List<?> source) {
    List<ModifierTree> modifiers = new ArrayList<>();
    for (Object o : source) {
      modifiers.add(convertModifier((IExtendedModifier) o));
    }
    return new ModifiersTreeImpl(modifiers);
  }

  private ModifierTree convertModifier(IExtendedModifier node) {
    switch (((ASTNode) node).getNodeType()) {
      case ASTNode.NORMAL_ANNOTATION,
        ASTNode.MARKER_ANNOTATION,
        ASTNode.SINGLE_MEMBER_ANNOTATION:
        return (AnnotationTree) convertExpression((Expression) node);
      case ASTNode.MODIFIER:
        return convertModifier((org.eclipse.jdt.core.dom.Modifier) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(((ASTNode) node).getNodeType()).toString());
    }
  }

  private ModifierTree convertModifier(org.eclipse.jdt.core.dom.Modifier node) {
    switch (node.getKeyword().toString()) {
      case "public":
        return new ModifierKeywordTreeImpl(Modifier.PUBLIC, firstTokenIn(node, TerminalToken.TokenNamepublic));
      case "protected":
        return new ModifierKeywordTreeImpl(Modifier.PROTECTED, firstTokenIn(node, TerminalToken.TokenNameprotected));
      case "private":
        return new ModifierKeywordTreeImpl(Modifier.PRIVATE, firstTokenIn(node, TerminalToken.TokenNameprivate));
      case "static":
        return new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn(node, TerminalToken.TokenNamestatic));
      case "abstract":
        return new ModifierKeywordTreeImpl(Modifier.ABSTRACT, firstTokenIn(node, TerminalToken.TokenNameabstract));
      case "final":
        return new ModifierKeywordTreeImpl(Modifier.FINAL, firstTokenIn(node, TerminalToken.TokenNamefinal));
      case "native":
        return new ModifierKeywordTreeImpl(Modifier.NATIVE, firstTokenIn(node, TerminalToken.TokenNamenative));
      case "synchronized":
        return new ModifierKeywordTreeImpl(Modifier.SYNCHRONIZED, firstTokenIn(node, TerminalToken.TokenNamesynchronized));
      case "transient":
        return new ModifierKeywordTreeImpl(Modifier.TRANSIENT, firstTokenIn(node, TerminalToken.TokenNametransient));
      case "volatile":
        return new ModifierKeywordTreeImpl(Modifier.VOLATILE, firstTokenIn(node, TerminalToken.TokenNamevolatile));
      case "strictfp":
        return new ModifierKeywordTreeImpl(Modifier.STRICTFP, firstTokenIn(node, TerminalToken.TokenNamestrictfp));
      case "default":
        return new ModifierKeywordTreeImpl(Modifier.DEFAULT, firstTokenIn(node, TerminalToken.TokenNamedefault));
      case "sealed":
        return new ModifierKeywordTreeImpl(Modifier.SEALED, firstTokenIn(node, TerminalToken.TokenNameRestrictedIdentifiersealed));
      case "non-sealed": {
        return new ModifierKeywordTreeImpl(Modifier.NON_SEALED, firstTokenIn(node, TerminalToken.TokenNamenon_sealed));
      }
      default:
        throw new IllegalStateException(node.getKeyword().toString());
    }
  }

  private static final TerminalToken ANY_TOKEN = TerminalToken.TokenNameInvalid;

  private static final Map<Object, Op> operators = new HashMap<>();

  private static class Op {
    final Tree.Kind kind;

    /**
     * {@link TerminalToken}
     */
    final TerminalToken tokenType;

    Op(Tree.Kind kind, TerminalToken tokenType) {
      this.kind = kind;
      this.tokenType = tokenType;
    }
  }

  static {
    operators.put(PrefixExpression.Operator.PLUS, new Op(Tree.Kind.UNARY_PLUS, TerminalToken.TokenNamePLUS));
    operators.put(PrefixExpression.Operator.MINUS, new Op(Tree.Kind.UNARY_MINUS, TerminalToken.TokenNameMINUS));
    operators.put(PrefixExpression.Operator.NOT, new Op(Tree.Kind.LOGICAL_COMPLEMENT, TerminalToken.TokenNameNOT));
    operators.put(PrefixExpression.Operator.COMPLEMENT, new Op(Tree.Kind.BITWISE_COMPLEMENT, TerminalToken.TokenNameTWIDDLE));
    operators.put(PrefixExpression.Operator.DECREMENT, new Op(Tree.Kind.PREFIX_DECREMENT, TerminalToken.TokenNameMINUS_MINUS));
    operators.put(PrefixExpression.Operator.INCREMENT, new Op(Tree.Kind.PREFIX_INCREMENT, TerminalToken.TokenNamePLUS_PLUS));

    operators.put(PostfixExpression.Operator.DECREMENT, new Op(Tree.Kind.POSTFIX_DECREMENT, TerminalToken.TokenNameMINUS_MINUS));
    operators.put(PostfixExpression.Operator.INCREMENT, new Op(Tree.Kind.POSTFIX_INCREMENT, TerminalToken.TokenNamePLUS_PLUS));

    operators.put(InfixExpression.Operator.TIMES, new Op(Tree.Kind.MULTIPLY, TerminalToken.TokenNameMULTIPLY));
    operators.put(InfixExpression.Operator.DIVIDE, new Op(Tree.Kind.DIVIDE, TerminalToken.TokenNameDIVIDE));
    operators.put(InfixExpression.Operator.REMAINDER, new Op(Tree.Kind.REMAINDER, TerminalToken.TokenNameREMAINDER));
    operators.put(InfixExpression.Operator.PLUS, new Op(Tree.Kind.PLUS, TerminalToken.TokenNamePLUS));
    operators.put(InfixExpression.Operator.MINUS, new Op(Tree.Kind.MINUS, TerminalToken.TokenNameMINUS));
    operators.put(InfixExpression.Operator.LEFT_SHIFT, new Op(Tree.Kind.LEFT_SHIFT, TerminalToken.TokenNameLEFT_SHIFT));
    operators.put(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, new Op(Tree.Kind.RIGHT_SHIFT, TerminalToken.TokenNameRIGHT_SHIFT));
    operators.put(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, new Op(Tree.Kind.UNSIGNED_RIGHT_SHIFT, TerminalToken.TokenNameUNSIGNED_RIGHT_SHIFT));
    operators.put(InfixExpression.Operator.LESS, new Op(Tree.Kind.LESS_THAN, TerminalToken.TokenNameLESS));
    operators.put(InfixExpression.Operator.GREATER, new Op(Tree.Kind.GREATER_THAN, TerminalToken.TokenNameGREATER));
    operators.put(InfixExpression.Operator.LESS_EQUALS, new Op(Tree.Kind.LESS_THAN_OR_EQUAL_TO, TerminalToken.TokenNameLESS_EQUAL));
    operators.put(InfixExpression.Operator.GREATER_EQUALS, new Op(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, TerminalToken.TokenNameGREATER_EQUAL));
    operators.put(InfixExpression.Operator.EQUALS, new Op(Tree.Kind.EQUAL_TO, TerminalToken.TokenNameEQUAL_EQUAL));
    operators.put(InfixExpression.Operator.NOT_EQUALS, new Op(Tree.Kind.NOT_EQUAL_TO, TerminalToken.TokenNameNOT_EQUAL));
    operators.put(InfixExpression.Operator.XOR, new Op(Tree.Kind.XOR, TerminalToken.TokenNameXOR));
    operators.put(InfixExpression.Operator.OR, new Op(Tree.Kind.OR, TerminalToken.TokenNameOR));
    operators.put(InfixExpression.Operator.AND, new Op(Tree.Kind.AND, TerminalToken.TokenNameAND));
    operators.put(InfixExpression.Operator.CONDITIONAL_OR, new Op(Tree.Kind.CONDITIONAL_OR, TerminalToken.TokenNameOR_OR));
    operators.put(InfixExpression.Operator.CONDITIONAL_AND, new Op(Tree.Kind.CONDITIONAL_AND, TerminalToken.TokenNameAND_AND));

    operators.put(Assignment.Operator.ASSIGN, new Op(Tree.Kind.ASSIGNMENT, TerminalToken.TokenNameEQUAL));
    operators.put(Assignment.Operator.PLUS_ASSIGN, new Op(Tree.Kind.PLUS_ASSIGNMENT, TerminalToken.TokenNamePLUS_EQUAL));
    operators.put(Assignment.Operator.MINUS_ASSIGN, new Op(Tree.Kind.MINUS_ASSIGNMENT, TerminalToken.TokenNameMINUS_EQUAL));
    operators.put(Assignment.Operator.TIMES_ASSIGN, new Op(Tree.Kind.MULTIPLY_ASSIGNMENT, TerminalToken.TokenNameMULTIPLY_EQUAL));
    operators.put(Assignment.Operator.DIVIDE_ASSIGN, new Op(Tree.Kind.DIVIDE_ASSIGNMENT, TerminalToken.TokenNameDIVIDE_EQUAL));
    operators.put(Assignment.Operator.BIT_AND_ASSIGN, new Op(Tree.Kind.AND_ASSIGNMENT, TerminalToken.TokenNameAND_EQUAL));
    operators.put(Assignment.Operator.BIT_OR_ASSIGN, new Op(Tree.Kind.OR_ASSIGNMENT, TerminalToken.TokenNameOR_EQUAL));
    operators.put(Assignment.Operator.BIT_XOR_ASSIGN, new Op(Tree.Kind.XOR_ASSIGNMENT, TerminalToken.TokenNameXOR_EQUAL));
    operators.put(Assignment.Operator.REMAINDER_ASSIGN, new Op(Tree.Kind.REMAINDER_ASSIGNMENT, TerminalToken.TokenNameREMAINDER_EQUAL));
    operators.put(Assignment.Operator.LEFT_SHIFT_ASSIGN, new Op(Tree.Kind.LEFT_SHIFT_ASSIGNMENT, TerminalToken.TokenNameLEFT_SHIFT_EQUAL));
    operators.put(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN, new Op(Tree.Kind.RIGHT_SHIFT_ASSIGNMENT, TerminalToken.TokenNameRIGHT_SHIFT_EQUAL));
    operators.put(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, new Op(Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT, TerminalToken.TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL));
  }

}
