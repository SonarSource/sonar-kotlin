/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sonarsource.analyzer.commons.TokenLocation;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.ModifierTree.Kind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token.Type;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.impl.AnnotationImpl;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.CatchTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.ExceptionHandlingTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.FunctionInvocationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.IfTreeImpl;
import org.sonarsource.slang.impl.ImportDeclarationTreeImpl;
import org.sonarsource.slang.impl.IntegerLiteralTreeImpl;
import org.sonarsource.slang.impl.JumpTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.LoopTreeImpl;
import org.sonarsource.slang.impl.MatchCaseTreeImpl;
import org.sonarsource.slang.impl.MatchTreeImpl;
import org.sonarsource.slang.impl.MemberSelectTreeImpl;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.PackageDeclarationTreeImpl;
import org.sonarsource.slang.impl.ParameterTreeImpl;
import org.sonarsource.slang.impl.ParenthesizedExpressionTreeImpl;
import org.sonarsource.slang.impl.PlaceHolderTreeImpl;
import org.sonarsource.slang.impl.ReturnTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.ThrowTreeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;
import org.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import org.sonarsource.slang.impl.VariableDeclarationTreeImpl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.sonarsource.slang.api.LoopTree.LoopKind.DOWHILE;
import static org.sonarsource.slang.api.LoopTree.LoopKind.FOR;
import static org.sonarsource.slang.api.LoopTree.LoopKind.WHILE;
import static org.sonarsource.slang.api.ModifierTree.Kind.OVERRIDE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PUBLIC;
import static org.sonarsource.slang.api.UnaryExpressionTree.Operator.DECREMENT;
import static org.sonarsource.slang.api.UnaryExpressionTree.Operator.INCREMENT;
import static org.sonarsource.slang.api.UnaryExpressionTree.Operator.MINUS;
import static org.sonarsource.slang.api.UnaryExpressionTree.Operator.NEGATE;
import static org.sonarsource.slang.api.UnaryExpressionTree.Operator.PLUS;

public class SLangConverter implements ASTConverter {

  private static final Set<Integer> KEYWORD_TOKEN_TYPES = new HashSet<>(Arrays.asList(
    SLangParser.ELSE,
    SLangParser.FUN,
    SLangParser.IF,
    SLangParser.MATCH,
    SLangParser.NATIVE,
    SLangParser.PRIVATE,
    SLangParser.PUBLIC,
    SLangParser.RETURN,
    SLangParser.THIS));

  @Override
  public Tree parse(String slangCode) {
    CommonTokenStream antlrTokens = getTokenStream(slangCode);

    List<Comment> comments = new ArrayList<>();
    List<org.sonarsource.slang.api.Token> tokens = new ArrayList<>();

    for (int index = 0; index < antlrTokens.size(); index++) {
      Token token = antlrTokens.get(index);
      TextRange textRange = getSlangTextRange(token);
      if (token.getChannel() == 1) {
        comments.add(comment(token, textRange));
      } else {
        Type type = Type.OTHER;
        if (KEYWORD_TOKEN_TYPES.contains(token.getType())) {
          type = Type.KEYWORD;
        } else if (token.getType() == SLangParser.StringLiteral) {
          type = Type.STRING_LITERAL;
        }
        tokens.add(new TokenImpl(textRange, token.getText(), type));
      }
    }

    // We can not re-use the same SlangParser to visit the tree multiples times, we have to parse a second time for annotations.
    // This is not optimal, but this converter is used only for tests, it won't impact production's performance.
    SLangParser parserAnnotation = new SLangParser(getTokenStream(slangCode));
    parserAnnotation.setErrorHandler(new ErrorStrategy());

    SlangParseTreeAnnotationsVisitor annotationsVisitor = new SlangParseTreeAnnotationsVisitor();
    annotationsVisitor.visit(parserAnnotation.slangFile());

    SLangParser parser = new SLangParser(antlrTokens);
    parser.setErrorHandler(new ErrorStrategy());

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor(comments, tokens, annotationsVisitor.annotations);
    return slangVisitor.visit(parser.slangFile());
  }

  private static CommonTokenStream getTokenStream(String slangCode) {
    SLangLexer lexer = new SLangLexer(CharStreams.fromString(slangCode));
    CommonTokenStream antlrTokens = new CommonTokenStream(lexer);
    antlrTokens.fill();
    return antlrTokens;
  }

  private static CommentImpl comment(Token token, TextRange range) {
    String text = token.getText();
    String contentText;
    TextPointer contentEnd;
    if (text.startsWith("//")) {
      contentText = text.substring(2);
      contentEnd = range.end();
    } else {
      contentText = text.substring(2, text.length() - 2);
      contentEnd = new TextPointerImpl(range.end().line(), range.end().lineOffset() - 2);
    }
    TextPointer contentStart = new TextPointerImpl(range.start().line(), range.start().lineOffset() + 2);
    return new CommentImpl(token.getText(), contentText, range, new TextRangeImpl(contentStart, contentEnd));
  }

  private static final Map<String, UnaryExpressionTree.Operator> UNARY_OPERATOR_MAP = unaryOperatorMap();
  private static final Map<String, Operator> BINARY_OPERATOR_MAP = binaryOperatorMap();
  private static final Map<String, AssignmentExpressionTree.Operator> ASSIGNMENT_OPERATOR_MAP = assignmentOperatorMap();

  private static Map<String, UnaryExpressionTree.Operator> unaryOperatorMap() {
    Map<String, UnaryExpressionTree.Operator> map = new HashMap<>();
    map.put("!", NEGATE);
    map.put("+", PLUS);
    map.put("-", MINUS);
    map.put("++", INCREMENT);
    map.put("--", DECREMENT);
    return Collections.unmodifiableMap(map);
  }

  private static Map<String, Operator> binaryOperatorMap() {
    Map<String, Operator> map = new HashMap<>();
    map.put("&&", Operator.CONDITIONAL_AND);
    map.put("||", Operator.CONDITIONAL_OR);
    map.put(">", Operator.GREATER_THAN);
    map.put(">=", Operator.GREATER_THAN_OR_EQUAL_TO);
    map.put("<", Operator.LESS_THAN);
    map.put("<=", Operator.LESS_THAN_OR_EQUAL_TO);
    map.put("==", Operator.EQUAL_TO);
    map.put("!=", Operator.NOT_EQUAL_TO);
    map.put("+", Operator.PLUS);
    map.put("-", Operator.MINUS);
    map.put("*", Operator.TIMES);
    map.put("/", Operator.DIVIDED_BY);
    return Collections.unmodifiableMap(map);
  }

  private static Map<String, AssignmentExpressionTree.Operator> assignmentOperatorMap() {
    Map<String, AssignmentExpressionTree.Operator> map = new HashMap<>();
    map.put("=", AssignmentExpressionTree.Operator.EQUAL);
    map.put("+=", AssignmentExpressionTree.Operator.PLUS_EQUAL);
    return Collections.unmodifiableMap(map);
  }

  private static TextRange getSlangTextRange(Token matchToken) {
    TokenLocation location = new TokenLocation(matchToken.getLine(), matchToken.getCharPositionInLine(), matchToken.getText());
    return new TextRangeImpl(location.startLine(), location.startLineOffset(), location.endLine(), location.endLineOffset());
  }

  private static class SlangParseTreeAnnotationsVisitor extends SLangBaseVisitor<Tree> {

    List<Annotation> annotations = new ArrayList<>();

    @Override
    public Tree visitAnnotation(SLangParser.AnnotationContext ctx) {
      String simpleName = ctx.identifier().Identifier().toString();

      List<String> argumentsText = new ArrayList<>();
      SLangParser.AnnotationParametersContext argument = ctx.annotationParameters();
      if (argument != null) {
        argumentsText.addAll(argument.annotationParameter().stream().map(RuleContext::getText).collect(Collectors.toList()));
      }

      annotations.add(new AnnotationImpl(simpleName, argumentsText,
          new TextRangeImpl(SLangParseTreeVisitor.startOf(ctx.start), SLangParseTreeVisitor.endOf(ctx.stop))));

      return super.visitAnnotation(ctx);
    }
  }

  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {

    private final TreeMetaDataProvider metaDataProvider;

    public SLangParseTreeVisitor(List<Comment> comments, List<org.sonarsource.slang.api.Token> tokens, List<Annotation> annotations) {
      metaDataProvider = new TreeMetaDataProvider(comments, tokens, annotations);
    }

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      // Special case for text range here, as last token is <EOF> which has length 5, so we only go up to the start of the <EOF> token
      TextRangeImpl textRange = new TextRangeImpl(startOf(ctx.start), new TextPointerImpl(ctx.stop.getLine(), ctx.stop.getCharPositionInLine()));
      List<Tree> typeDeclarations = list(ctx.typeDeclaration());
      List<Tree> allDeclarations = new ArrayList<>();
      if (ctx.packageDeclaration() != null) {
        allDeclarations.add(visit(ctx.packageDeclaration()));
      }
      allDeclarations.addAll(list(ctx.importDeclaration()));
      allDeclarations.addAll(typeDeclarations);
      org.sonarsource.slang.api.Token firstCpdToken = typeDeclarations.isEmpty() ? null : typeDeclarations.get(0).metaData().tokens().get(0);
      return new TopLevelTreeImpl(meta(textRange), allDeclarations, metaDataProvider.allComments(), firstCpdToken);
    }

    @Override
    public Tree visitPackageDeclaration(SLangParser.PackageDeclarationContext ctx) {
      return new PackageDeclarationTreeImpl(meta(ctx), singletonList(visit(ctx.identifier())));
    }

    @Override
    public Tree visitImportDeclaration(SLangParser.ImportDeclarationContext ctx) {
      return new ImportDeclarationTreeImpl(meta(ctx), singletonList(visit(ctx.identifier())));
    }

    @Override
    public Tree visitTypeDeclaration(SLangParser.TypeDeclarationContext ctx) {
      if (ctx.methodDeclaration() != null) {
        return visit(ctx.methodDeclaration());
      } else if (ctx.classDeclaration() != null) {
        return visit(ctx.classDeclaration());
      } else {
        return visit(ctx.controlBlock());
      }
    }

    @Override
    public Tree visitClassDeclaration(SLangParser.ClassDeclarationContext ctx) {
      List<Tree> children = new ArrayList<>();
      IdentifierTree identifier = null;

      if (ctx.identifier() != null) {
        identifier = (IdentifierTree) visit(ctx.identifier());
        children.add(identifier);
      }

      children.addAll(list(ctx.typeDeclaration()));

      NativeTree classDecl = new NativeTreeImpl(meta(ctx), new SNativeKind(ctx), children);
      return new ClassDeclarationTreeImpl(meta(ctx), identifier, classDecl);
    }

    @Override
    public Tree visitNativeExpression(SLangParser.NativeExpressionContext ctx) {
      return nativeTree(ctx, ctx.nativeBlock());
    }

    @Override
    public Tree visitParenthesizedExpression(SLangParser.ParenthesizedExpressionContext ctx) {
      return new ParenthesizedExpressionTreeImpl(
        meta(ctx),
        visit(ctx.statement()),
        toSlangToken(ctx.LPAREN().getSymbol()),
        toSlangToken(ctx.RPAREN().getSymbol()));
    }

    @Override
    public Tree visitMethodDeclaration(SLangParser.MethodDeclarationContext ctx) {
      List<Tree> modifiers = list(ctx.methodModifier());
      Tree returnType = null;
      IdentifierTree name = null;
      SLangParser.MethodHeaderContext methodHeaderContext = ctx.methodHeader();
      SLangParser.SimpleTypeContext resultContext = methodHeaderContext.simpleType();
      SLangParser.IdentifierContext identifier = methodHeaderContext.methodDeclarator().identifier();
      if (resultContext != null) {
        returnType = new IdentifierTreeImpl(meta(resultContext), resultContext.getText());
      }
      boolean isConstructor = false;
      if (identifier != null) {
        name = (IdentifierTree) visit(identifier);
        isConstructor = "constructor".equals(name.name());
      }

      List<Tree> convertedParameters = new ArrayList<>();
      SLangParser.FormalParameterListContext formalParameterListContext = methodHeaderContext.methodDeclarator().formalParameterList();
      if (formalParameterListContext != null) {
        SLangParser.FormalParametersContext formalParameters = formalParameterListContext.formalParameters();
        if (formalParameters != null) {
          convertedParameters.addAll(list(formalParameters.formalParameter()));
        }
        convertedParameters.add(visit(formalParameterListContext.lastFormalParameter()));
      }

      return new FunctionDeclarationTreeImpl(meta(ctx), modifiers, isConstructor, returnType, name, convertedParameters, (BlockTree) visit(ctx.methodBody()), emptyList());
    }

    @Override
    public Tree visitMethodModifier(SLangParser.MethodModifierContext ctx) {
      Kind modifierKind = PUBLIC;
      if (ctx.PRIVATE() != null) {
        modifierKind = PRIVATE;
      } else if (ctx.OVERRIDE() != null) {
        modifierKind = OVERRIDE;
      } else if(ctx.nativeExpression() != null) {
        return visit(ctx.nativeExpression());
      }
      return new ModifierTreeImpl(meta(ctx), modifierKind);
    }

    @Override
    public Tree visitMethodInvocation(SLangParser.MethodInvocationContext ctx) {
      List<Tree> arguments = new ArrayList<>();
      SLangParser.ArgumentListContext argumentListContext = ctx.argumentList();
      if (argumentListContext != null) {
        arguments.addAll(list(argumentListContext.statement()));
      }

      return new FunctionInvocationTreeImpl(meta(ctx), visit(ctx.memberSelect()), arguments);
    }

    @Override
    public Tree visitMemberSelect(SLangParser.MemberSelectContext ctx) {
      Tree id = visit(ctx.identifier());
      if (ctx.memberSelect() != null) {
        IdentifierTree identifier = (IdentifierTree) id;
        return new MemberSelectTreeImpl(meta(ctx), visit(ctx.memberSelect()), identifier);
      } else {
        return id;
      }
    }

    @Override
    public Tree visitMethodBody(SLangParser.MethodBodyContext ctx) {
      if (ctx.SEMICOLON() != null) {
        return null;
      }
      return visit(ctx.block());
    }

    @Override
    public Tree visitFormalParameter(SLangParser.FormalParameterContext ctx) {
      IdentifierTree tree = (IdentifierTree) visit(ctx.variableDeclaratorId().identifier());
      Tree type = null;
      Tree defaultValue = null;
      List<Tree> modifiers = list(ctx.parameterModifier());

      if (ctx.simpleType() != null) {
        type = new IdentifierTreeImpl(meta(ctx.simpleType()), ctx.simpleType().getText());
      }
      if(ctx.expression() != null) {
        defaultValue = visit(ctx.expression());
      }
      return new ParameterTreeImpl(meta(ctx), tree, type, defaultValue, modifiers);
    }

    @Override
    public Tree visitLastFormalParameter(SLangParser.LastFormalParameterContext ctx) {
      return visit(ctx.formalParameter());
    }

    @Override
    public Tree visitDeclaration(SLangParser.DeclarationContext ctx) {
      IdentifierTree identifier = (IdentifierTree) visit(ctx.identifier());
      Tree type = null;
      if (ctx.simpleType() != null) {
        type = new IdentifierTreeImpl(meta(ctx.simpleType()), ctx.simpleType().getText());
      }
      Tree initializer = null;
      if (ctx.expression() != null) {
        initializer = visit(ctx.expression());
      }

      boolean isVal = ctx.declarationModifier().VAL() != null;
      return new VariableDeclarationTreeImpl(meta(ctx), identifier, type, initializer, isVal);
    }

    @Override
    public Tree visitBlock(SLangParser.BlockContext ctx) {
      return new BlockTreeImpl(meta(ctx), list(ctx.statement()));
    }

    @Override
    public Tree visitIfExpression(SLangParser.IfExpressionContext ctx) {
      org.sonarsource.slang.api.Token ifToken = toSlangToken(ctx.IF().getSymbol());
      org.sonarsource.slang.api.Token elseToken = null;
      Tree elseBranch = null;
      if (ctx.controlBlock().size() > 1) {
        elseBranch = visit(ctx.controlBlock(1));
        elseToken = toSlangToken(ctx.ELSE().getSymbol());
      }
      Tree thenBranch = visit(ctx.controlBlock(0));
      return new IfTreeImpl(
        meta(ctx),
        visit(ctx.statement()),
        thenBranch,
        elseBranch,
        ifToken,
        elseToken);
    }

    @Override
    public Tree visitMatchExpression(SLangParser.MatchExpressionContext ctx) {
      List<MatchCaseTree> cases = new ArrayList<>();
      for (SLangParser.MatchCaseContext matchCaseContext : ctx.matchCase()) {
        cases.add((MatchCaseTree) visit(matchCaseContext));
      }
      TreeMetaData meta = meta(ctx);
      Tree expression = ctx.statement() == null ? null : visit(ctx.statement());
      return new MatchTreeImpl(
        meta,
        expression,
        cases,
        toSlangToken(ctx.MATCH().getSymbol()));
    }

    @Override
    public Tree visitMatchCase(SLangParser.MatchCaseContext ctx) {
      Tree expression = ctx.statement() == null ? null : visit(ctx.statement());
      Tree body = ctx.controlBlock() == null ? null : visit(ctx.controlBlock());
      return new MatchCaseTreeImpl(meta(ctx), expression, body);
    }

    @Override
    public Tree visitForLoop(SLangParser.ForLoopContext ctx) {
      Tree condition = ctx.declaration() == null ? null : visit(ctx.declaration());
      Tree body = visit(ctx.controlBlock());
      return new LoopTreeImpl(meta(ctx), condition, body, FOR, toSlangToken(ctx.FOR().getSymbol()));
    }

    @Override
    public Tree visitWhileLoop(SLangParser.WhileLoopContext ctx) {
      Tree condition = visit(ctx.statement());
      Tree body = visit(ctx.controlBlock());
      return new LoopTreeImpl(meta(ctx), condition, body, WHILE, toSlangToken(ctx.WHILE().getSymbol()));
    }

    @Override
    public Tree visitDoWhileLoop(SLangParser.DoWhileLoopContext ctx) {
      Tree condition = visit(ctx.statement());
      Tree body = visit(ctx.controlBlock());
      return new LoopTreeImpl(meta(ctx), condition, body, DOWHILE, toSlangToken(ctx.DO().getSymbol()));
    }

    @Override
    public Tree visitCatchBlock(SLangParser.CatchBlockContext ctx) {
      ParameterTree parameter = ctx.formalParameter() == null ? null : (ParameterTree) visit(ctx.formalParameter());
      Tree body = visit(ctx.block());
      return new CatchTreeImpl(meta(ctx), parameter, body, toSlangToken(ctx.CATCH().getSymbol()));
    }

    @Override
    public Tree visitTryExpression(SLangParser.TryExpressionContext ctx) {
      Tree tryBlock = visit(ctx.block());
      List<CatchTree> catchTreeList = new ArrayList<>();
      for (SLangParser.CatchBlockContext catchBlockContext : ctx.catchBlock()) {
        catchTreeList.add((CatchTree) visit(catchBlockContext));
      }
      org.sonarsource.slang.api.Token tryToken = toSlangToken(ctx.TRY().getSymbol());
      Tree finallyBlock = ctx.finallyBlock() == null ? null : visit(ctx.finallyBlock());
      return new ExceptionHandlingTreeImpl(meta(ctx), tryBlock, tryToken, catchTreeList, finallyBlock);
    }

    @Override
    public Tree visitNativeBlock(SLangParser.NativeBlockContext ctx) {
      return nativeTree(ctx, ctx.statement());
    }

    @Override
    public Tree visitAssignment(SLangParser.AssignmentContext ctx) {
      Tree leftHandSide = visit(ctx.expression());
      Tree statementOrExpression = assignmentTree(ctx.statement(), ctx.assignmentOperator());
      AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATOR_MAP.get(ctx.assignmentOperator(0).getText());
      return new AssignmentExpressionTreeImpl(meta(ctx), operator, leftHandSide, statementOrExpression);
    }

    @Override
    public Tree visitDisjunction(SLangParser.DisjunctionContext ctx) {
      return binaryTree(ctx.conjunction(), ctx.disjunctionOperator());
    }

    @Override
    public Tree visitConjunction(SLangParser.ConjunctionContext ctx) {
      return binaryTree(ctx.equalityComparison(), ctx.conjunctionOperator());
    }

    @Override
    public Tree visitEqualityComparison(SLangParser.EqualityComparisonContext ctx) {
      return binaryTree(ctx.comparison(), ctx.equalityOperator());
    }

    @Override
    public Tree visitComparison(SLangParser.ComparisonContext ctx) {
      return binaryTree(ctx.additiveExpression(), ctx.comparisonOperator());
    }

    @Override
    public Tree visitAdditiveExpression(SLangParser.AdditiveExpressionContext ctx) {
      return binaryTree(ctx.multiplicativeExpression(), ctx.additiveOperator());
    }

    @Override
    public Tree visitMultiplicativeExpression(SLangParser.MultiplicativeExpressionContext ctx) {
      return binaryTree(ctx.unaryExpression(), ctx.multiplicativeOperator());
    }

    @Override
    public Tree visitUnaryExpression(SLangParser.UnaryExpressionContext ctx) {
      if (ctx.unaryOperator() == null) {
        return visit(ctx.atomicExpression());
      } else {
        Tree operand = visit(ctx.unaryExpression());
        return new UnaryExpressionTreeImpl(meta(ctx), UNARY_OPERATOR_MAP.get(ctx.unaryOperator().getText()), operand);
      }
    }

    @Override
    public Tree visitLiteral(SLangParser.LiteralContext ctx) {
      if (ctx.StringLiteral() != null) {
        return new StringLiteralTreeImpl(meta(ctx), ctx.getText());
      } else if (ctx.IntegerLiteral() != null) {
        return new IntegerLiteralTreeImpl(meta(ctx), ctx.getText());
      } else {
        return new LiteralTreeImpl(meta(ctx), ctx.getText());
      }
    }

    @Override
    public Tree visitIdentifier(SLangParser.IdentifierContext ctx) {
      if("_".equals(ctx.getText())) {
        return new PlaceHolderTreeImpl(meta(ctx), toSlangToken(ctx.getStart()));
      } else {
        return new IdentifierTreeImpl(meta(ctx), ctx.getText());
      }
    }

    @Override
    public Tree visitBreakExpression(SLangParser.BreakExpressionContext ctx) {
      IdentifierTree label = null;
      if (ctx.label() != null) {
        label = (IdentifierTree) visit(ctx.label());
      }
      return new JumpTreeImpl(meta(ctx), toSlangToken(ctx.BREAK().getSymbol()), JumpTree.JumpKind.BREAK, label);
    }

    @Override
    public Tree visitContinueExpression(SLangParser.ContinueExpressionContext ctx) {
      IdentifierTree label = null;
      if (ctx.label() != null) {
        label = (IdentifierTree) visit(ctx.label());
      }
      return new JumpTreeImpl(meta(ctx), toSlangToken(ctx.CONTINUE().getSymbol()), JumpTree.JumpKind.CONTINUE, label);
    }

    @Override
    public Tree visitReturnExpression(SLangParser.ReturnExpressionContext ctx) {
      Tree returnBody = null;
      if (ctx.statement() != null) {
        returnBody = visit(ctx.statement());
      }
      return new ReturnTreeImpl(meta(ctx), toSlangToken(ctx.RETURN().getSymbol()), returnBody);
    }

    @Override
    public Tree visitThrowExpression(SLangParser.ThrowExpressionContext ctx) {
      Tree throwBody = null;
      if (ctx.statement() != null) {
        throwBody = visit(ctx.statement());
      }
      return new ThrowTreeImpl(meta(ctx), toSlangToken(ctx.THROW().getSymbol()), throwBody);
    }

    private static TextPointer startOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine());
    }

    private static TextPointer endOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine() + token.getText().length());
    }

    private TreeMetaData meta(ParserRuleContext ctx) {
      return meta(new TextRangeImpl(startOf(ctx.start), endOf(ctx.stop)));
    }

    private TreeMetaData meta(Tree first, Tree last) {
      return meta(new TextRangeImpl(first.metaData().textRange().start(), last.metaData().textRange().end()));
    }

    private TreeMetaData meta(TextRange textRange) {
      return metaDataProvider.metaData(textRange);
    }

    private NativeTree nativeTree(ParserRuleContext ctx, List<? extends ParseTree> rawChildren) {
      List<Tree> children = list(rawChildren);
      return new NativeTreeImpl(meta(ctx), new SNativeKind(ctx), children);
    }

    private List<Tree> list(List<? extends ParseTree> rawChildren) {
      return rawChildren
        .stream()
        .map(this::visit)
        .collect(toList());
    }

    private Tree binaryTree(List<? extends ParseTree> operands, List<? extends ParserRuleContext> operators) {
      Tree result = visit(operands.get(operands.size() - 1));
      for (int i = operands.size() - 2; i >= 0; i--) {
        Tree left = visit(operands.get(i));
        Operator operator = BINARY_OPERATOR_MAP.get(operators.get(i).getText());
        result = new BinaryExpressionTreeImpl(meta(left, result), operator, operatorToken(operators.get(i)), left, result);
      }
      return result;
    }

    private Tree assignmentTree(List<? extends ParseTree> expressions, List<? extends ParseTree> operators) {
      Tree result = visit(expressions.get(expressions.size() - 1));
      for (int i = expressions.size() - 2; i >= 0; i--) {
        Tree left = visit(expressions.get(i));
        AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATOR_MAP.get(operators.get(i).getText());
        result = new AssignmentExpressionTreeImpl(meta(left, result), operator, left, result);
      }
      return result;
    }

    private static org.sonarsource.slang.api.Token toSlangToken(Token antlrToken) {
      TextRange textRange = getSlangTextRange(antlrToken);
      return new TokenImpl(textRange, antlrToken.getText(), Type.KEYWORD);
    }

    private static org.sonarsource.slang.api.Token operatorToken(ParserRuleContext parserRuleContext) {
      TextRange textRange = TextRanges.merge(Arrays.asList(
        getSlangTextRange(parserRuleContext.start),
        getSlangTextRange(parserRuleContext.stop)));
      return new TokenImpl(textRange, parserRuleContext.getText(), Type.OTHER);
    }

  }
}
