package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.Field;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.MainMethod;
import com.github.firmwehr.gentle.parser.ast.Method;
import com.github.firmwehr.gentle.parser.ast.Parameter;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.basictype.BasicType;
import com.github.firmwehr.gentle.parser.ast.basictype.BooleanType;
import com.github.firmwehr.gentle.parser.ast.basictype.IdentType;
import com.github.firmwehr.gentle.parser.ast.basictype.IntType;
import com.github.firmwehr.gentle.parser.ast.basictype.VoidType;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperatorExpression;
import com.github.firmwehr.gentle.parser.ast.expression.BooleanLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.expression.IdentExpression;
import com.github.firmwehr.gentle.parser.ast.expression.IntegerLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.expression.LocalMethodCallExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NewArrayExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NewObjectExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NullExpression;
import com.github.firmwehr.gentle.parser.ast.expression.PostfixExpression;
import com.github.firmwehr.gentle.parser.ast.expression.ThisExpression;
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperatorExpression;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.ArrayAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.FieldAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.MethodInvocationOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;
import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.BlockStatement;
import com.github.firmwehr.gentle.parser.ast.statement.EmptyStatement;
import com.github.firmwehr.gentle.parser.ast.statement.ExpressionStatement;
import com.github.firmwehr.gentle.parser.ast.statement.IfStatement;
import com.github.firmwehr.gentle.parser.ast.statement.LocalVariableDeclarationStatement;
import com.github.firmwehr.gentle.parser.ast.statement.ReturnStatement;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.ast.statement.WhileStatement;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;
import com.github.firmwehr.gentle.parser.tokens.IntegerLiteralToken;
import com.github.firmwehr.gentle.parser.tokens.Keyword;
import com.github.firmwehr.gentle.parser.tokens.Operator;
import com.github.firmwehr.gentle.parser.tokens.OperatorToken;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class Parser {
	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

	private final Tokens tokens;

	public Parser(Tokens tokens) {
		this.tokens = tokens;
	}

	public static Parser fromLexer(Source source, Lexer lexer) throws LexerException {
		return new Parser(Tokens.fromLexer(source, lexer));
	}

	public Program parse() throws ParseException {
		List<ClassDeclaration> classDeclarations = new ArrayList<>();

		while (tokens.expectingKeyword(Keyword.CLASS).peek().isKeyword(Keyword.CLASS)) {
			classDeclarations.add(parseClassDeclaration());
		}

		tokens.expectEof();

		return new Program(classDeclarations);
	}


	private ClassDeclaration parseClassDeclaration() throws ParseException {
		tokens.expectKeyword(Keyword.CLASS);

		Ident name = parseIdent();

		tokens.expectOperator(Operator.LEFT_BRACE);

		List<Field> fields = new ArrayList<>();
		List<Method> methods = new ArrayList<>();
		List<MainMethod> mainMethods = new ArrayList<>();
		while (tokens.expectingKeyword(Keyword.PUBLIC).peek().isKeyword(Keyword.PUBLIC)) {
			parseClassMember(fields, methods, mainMethods);
		}

		tokens.expectOperator(Operator.RIGHT_BRACE);

		return new ClassDeclaration(name, fields, methods, mainMethods);
	}

	private void parseClassMember(List<Field> fields, List<Method> methods, List<MainMethod> mainMethods)
		throws ParseException {

		tokens.expectKeyword(Keyword.PUBLIC);

		if (tokens.expectingKeyword(Keyword.STATIC).peek().isKeyword(Keyword.STATIC)) {
			mainMethods.add(parseMainMethodRest());
		} else {
			// This can either be a field or a method, and we don't know until after the ident
			Type type = parseType();
			Ident ident = parseIdent();

			if (tokens.expectingOperator(Operator.SEMICOLON).peek().isOperator(Operator.SEMICOLON)) {
				tokens.take();
				fields.add(new Field(type, ident));
			} else if (tokens.expectingOperator(Operator.LEFT_PAREN).peek().isOperator(Operator.LEFT_PAREN)) {
				methods.add(parseMethodRest(type, ident));
			} else {
				tokens.error();
			}
		}

	}

	private MainMethod parseMainMethodRest() throws ParseException {
		tokens.expectKeyword(Keyword.STATIC);
		tokens.expectKeyword(Keyword.VOID);

		Ident name = parseIdent();

		tokens.expectOperator(Operator.LEFT_PAREN);

		Parameter parameter = parseParameter();

		tokens.expectOperator(Operator.RIGHT_PAREN);
		parseOptionalMethodRest();

		Block block = parseBlock();

		return new MainMethod(name, parameter, block);
	}

	private Method parseMethodRest(Type type, Ident ident) throws ParseException {
		tokens.expectOperator(Operator.LEFT_PAREN);

		List<Parameter> parameters = new ArrayList<>();
		if (!tokens.expectingOperator(Operator.RIGHT_PAREN).peek().isOperator(Operator.RIGHT_PAREN)) {
			parameters.add(parseParameter());
		}
		while (tokens.expectingOperator(Operator.COMMA).peek().isOperator(Operator.COMMA)) {
			tokens.take();
			parameters.add(parseParameter());
		}

		tokens.expectOperator(Operator.RIGHT_PAREN);
		parseOptionalMethodRest();

		Block block = parseBlock();

		return new Method(type, ident, parameters, block);
	}

	private Parameter parseParameter() throws ParseException {
		Type type = parseType();
		Ident ident = parseIdent();
		return new Parameter(type, ident);
	}

	private void parseOptionalMethodRest() throws ParseException {
		if (tokens.expectingKeyword(Keyword.THROWS).peek().isKeyword(Keyword.THROWS)) {
			tokens.take();
			tokens.expectIdent();
		}
	}

	private Block parseBlock() throws ParseException {
		tokens.expectOperator(Operator.LEFT_BRACE);

		List<BlockStatement> statements = new ArrayList<>();
		while (!tokens.expectingOperator(Operator.RIGHT_BRACE).peek().isOperator(Operator.RIGHT_BRACE)) {
			statements.add(parseBlockStatement());
		}

		tokens.expectOperator(Operator.RIGHT_BRACE);

		return new Block(statements);
	}

	private BlockStatement parseBlockStatement() throws ParseException {
		expectingBasicType(); // Local variable declaration statement
		expectingStatement();

		Token token = tokens.peek();

		// We have a local variable declaration statement exactly if we have...
		// - a basic type (int, boolean, void)
		// - an ident followed by another ident
		// - an ident followed by '[' and ']'
		boolean basicType =
			token.isKeyword(Keyword.INT) || token.isKeyword(Keyword.BOOLEAN) || token.isKeyword(Keyword.VOID);
		boolean doubleIdents = token.isIdent() && tokens.peek(1).isIdent();
		boolean arrayType = token.isIdent() && tokens.peek(1).isOperator(Operator.LEFT_BRACKET) &&
			tokens.peek(2).isOperator(Operator.RIGHT_BRACKET);

		if (basicType || doubleIdents || arrayType) {
			return parseLocalVariableDeclarationStatement();
		} else {
			return parseStatement().asBlockStatement();
		}
	}

	private LocalVariableDeclarationStatement parseLocalVariableDeclarationStatement() throws ParseException {
		Type type = parseType();
		Ident name = parseIdent();

		Optional<Expression> value;
		if (tokens.expectingOperator(Operator.ASSIGN).peek().isOperator(Operator.ASSIGN)) {
			tokens.take();
			value = Optional.of(parseExpression());
		} else {
			value = Optional.empty();
		}

		tokens.expectOperator(Operator.SEMICOLON);

		return new LocalVariableDeclarationStatement(type, name, value);
	}

	private void expectingStatement() {
		tokens.expectingOperator(Operator.LEFT_BRACE)
			.expectingOperator(Operator.SEMICOLON)
			.expectingKeyword(Keyword.IF)
			.expectingKeyword(Keyword.WHILE)
			.expectingKeyword(Keyword.RETURN);
		expectingExpression();
	}

	private Statement parseStatement() throws ParseException {
		expectingStatement();

		Token token = tokens.peek();
		if (token.isOperator(Operator.LEFT_BRACE)) {
			return parseBlock();
		} else if (token.isOperator(Operator.SEMICOLON)) {
			tokens.take();
			return new EmptyStatement();
		} else if (token.isKeyword(Keyword.IF)) {
			return parseIfStatement();
		} else if (token.isKeyword(Keyword.WHILE)) {
			return parseWhileStatement();
		} else if (token.isKeyword(Keyword.RETURN)) {
			return parseReturnStatement();
		} else {
			return parseExpressionStatement();
		}
	}

	private IfStatement parseIfStatement() throws ParseException {
		tokens.expectKeyword(Keyword.IF);
		tokens.expectOperator(Operator.LEFT_PAREN);

		Expression condition = parseExpression();

		tokens.expectOperator(Operator.RIGHT_PAREN);

		Statement body = parseStatement();

		Optional<Statement> elseBody;
		if (tokens.expectingKeyword(Keyword.ELSE).peek().isKeyword(Keyword.ELSE)) {
			tokens.take();
			elseBody = Optional.of(parseStatement());
		} else {
			elseBody = Optional.empty();
		}

		return new IfStatement(condition, body, elseBody);
	}

	private WhileStatement parseWhileStatement() throws ParseException {
		tokens.expectKeyword(Keyword.WHILE);
		tokens.expectOperator(Operator.LEFT_PAREN);

		Expression condition = parseExpression();

		tokens.expectOperator(Operator.RIGHT_PAREN);

		Statement body = parseStatement();

		return new WhileStatement(condition, body);
	}

	private ReturnStatement parseReturnStatement() throws ParseException {
		tokens.expectKeyword(Keyword.RETURN);

		Optional<Expression> returnValue;
		if (!tokens.expectingOperator(Operator.SEMICOLON).peek().isOperator(Operator.SEMICOLON)) {
			returnValue = Optional.of(parseExpression());
		} else {
			returnValue = Optional.empty();
		}

		tokens.expectOperator(Operator.SEMICOLON);

		return new ReturnStatement(returnValue);
	}

	private ExpressionStatement parseExpressionStatement() throws ParseException {
		Expression expression = parseExpression();

		tokens.expectOperator(Operator.SEMICOLON);

		return new ExpressionStatement(expression);
	}

	private void expectingExpression() {
		expectingPrimaryExpression();
		tokens.expectingOperator(Operator.LOGICAL_NOT).expectingOperator(Operator.MINUS);
	}

	private Expression parseExpression() throws ParseException {
		return parseExpressionWithPrecedence(0);
	}

	@SuppressWarnings("InfiniteRecursion")
	private Expression parseExpressionWithPrecedence(int precedence) throws ParseException {
		Expression expression = parseUnaryExpression();

		while (true) {
			Optional<BinaryOperator> operator = peekOptionalBinaryOperator();
			if (operator.isEmpty()) {
				break;
			}

			int opPrec = operator.get().getPrecedence();
			if (opPrec < precedence) {
				break;
			}

			tokens.take();

			BinaryOperator.Associativity opAssoc = operator.get().getAssociativity();
			int newPrecedence = switch (opAssoc) {
				case LEFT -> opPrec + 1;
				case RIGHT -> opPrec;
			};
			Expression rhs = parseExpressionWithPrecedence(newPrecedence);

			expression = new BinaryOperatorExpression(expression, rhs, operator.get());
		}

		return expression;
	}

	private Optional<BinaryOperator> peekOptionalBinaryOperator() {
		Token token = tokens.expecting("binary operator").peek();
		if (token instanceof OperatorToken operatorToken) {
			return BinaryOperator.fromOperator(operatorToken.operator());
		} else {
			return Optional.empty();
		}
	}

	private Expression parseUnaryExpression() throws ParseException {
		expectingExpression();
		if (tokens.peek().isOperator(Operator.LOGICAL_NOT)) {
			tokens.take();
			return new UnaryOperatorExpression(UnaryOperator.LOGICAL_NOT, parseUnaryExpression());
		} else if (tokens.peek().isOperator(Operator.MINUS)) {
			tokens.take();
			return new UnaryOperatorExpression(UnaryOperator.NEGATION, parseUnaryExpression());
		} else {
			return parsePostfixExpression();
		}
	}

	private Expression parsePostfixExpression() throws ParseException {
		Expression expression = parsePrimaryExpression();

		while (true) {
			Optional<PostfixOp> op = parseOptionalPostfixOp();
			if (op.isPresent()) {
				expression = new PostfixExpression(expression, op.get());
			} else {
				break;
			}
		}

		return expression;
	}

	private Optional<PostfixOp> parseOptionalPostfixOp() throws ParseException {
		Token token = tokens.expectingOperator(Operator.DOT).expectingOperator(Operator.LEFT_BRACKET).peek();

		if (token.isOperator(Operator.DOT)) {
			tokens.take();
			Ident name = parseIdent();
			if (tokens.expectingOperator(Operator.LEFT_PAREN).peek().isOperator(Operator.LEFT_PAREN)) {
				return Optional.of(new MethodInvocationOp(name, parseParenthesisedArguments()));
			} else {
				return Optional.of(new FieldAccessOp(name));
			}
		} else if (token.isOperator(Operator.LEFT_BRACKET)) {
			tokens.take();
			Expression index = parseExpression();
			tokens.expectOperator(Operator.RIGHT_BRACKET);
			return Optional.of(new ArrayAccessOp(index));
		} else {
			return Optional.empty();
		}
	}

	private List<Expression> parseParenthesisedArguments() throws ParseException {
		tokens.expectOperator(Operator.LEFT_PAREN);

		List<Expression> arguments = new ArrayList<>();
		if (!tokens.expectingOperator(Operator.RIGHT_PAREN).peek().isOperator(Operator.RIGHT_PAREN)) {
			arguments.add(parseExpression());

			while (tokens.expectingOperator(Operator.COMMA).peek().isOperator(Operator.COMMA)) {
				tokens.take();
				arguments.add(parseExpression());
			}
		}

		tokens.expectOperator(Operator.RIGHT_PAREN);

		return arguments;
	}

	private void expectingPrimaryExpression() {
		tokens.expectingKeyword(Keyword.NULL)
			.expecting("boolean literal")
			.expectingIntegerLiteral()
			.expectingIdent()
			.expectingKeyword(Keyword.THIS)
			.expectingOperator(Operator.LEFT_PAREN)
			.expectingKeyword(Keyword.NEW);
	}

	private Expression parsePrimaryExpression() throws ParseException {
		expectingPrimaryExpression();

		Token token = tokens.peek();
		Optional<IntegerLiteralToken> integerLiteralToken = token.asIntegerLiteralToken();
		Optional<IdentToken> identToken = token.asIdentToken();

		if (token.isKeyword(Keyword.NULL)) {
			tokens.take();
			return new NullExpression();
		} else if (token.isKeyword(Keyword.TRUE)) {
			tokens.take();
			return new BooleanLiteralExpression(true);
		} else if (token.isKeyword(Keyword.FALSE)) {
			tokens.take();
			return new BooleanLiteralExpression(false);
		} else if (integerLiteralToken.isPresent()) {
			tokens.take();
			return new IntegerLiteralExpression(integerLiteralToken.get().value());
		} else if (identToken.isPresent()) {
			tokens.take();
			if (tokens.expectingOperator(Operator.LEFT_PAREN).peek().isOperator(Operator.LEFT_PAREN)) {
				return new LocalMethodCallExpression(Ident.fromToken(identToken.get()), parseParenthesisedArguments());
			} else {
				return new IdentExpression(Ident.fromToken(identToken.get()));
			}
		} else if (token.isKeyword(Keyword.THIS)) {
			tokens.take();
			return new ThisExpression();
		} else if (token.isOperator(Operator.LEFT_PAREN)) {
			tokens.take();
			Expression expression = parseExpression();
			tokens.expectOperator(Operator.RIGHT_PAREN);
			return expression;
		} else if (token.isKeyword(Keyword.NEW)) {
			return parseNewObjectExpressionOrNewArrayExpression();
		} else {
			return tokens.error();
		}
	}

	private Expression parseNewObjectExpressionOrNewArrayExpression() throws ParseException {
		tokens.expectKeyword(Keyword.NEW);

		Token token = tokens.expectingIdent().expecting("basic type").peek();
		Optional<IdentToken> identToken = token.asIdentToken();

		if (identToken.isPresent() && tokens.peek(1).isOperator(Operator.LEFT_PAREN)) {
			tokens.take();
			Ident name = Ident.fromToken(identToken.get());
			tokens.expectOperator(Operator.LEFT_PAREN);
			tokens.expectOperator(Operator.RIGHT_PAREN);
			return new NewObjectExpression(name);
		} else {
			BasicType basicType = parseBasicType();
			tokens.expectOperator(Operator.LEFT_BRACKET);
			Expression size = parseExpression();
			tokens.expectOperator(Operator.RIGHT_BRACKET);

			int arrayLevel = 1;
			while (tokens.expectingOperator(Operator.LEFT_BRACKET).peek().isOperator(Operator.LEFT_BRACKET)) {
				tokens.take();
				tokens.expectOperator(Operator.RIGHT_BRACKET);
				arrayLevel++;
			}

			return new NewArrayExpression(new Type(basicType, arrayLevel), size);
		}
	}

	private Type parseType() throws ParseException {
		BasicType basicType = parseBasicType();

		int arrayLevel = 0;
		while (tokens.expectingOperator(Operator.LEFT_BRACKET).peek().isOperator(Operator.LEFT_BRACKET)) {
			tokens.take();
			tokens.expectOperator(Operator.RIGHT_BRACKET);
			arrayLevel++;
		}

		return new Type(basicType, arrayLevel);
	}

	private void expectingBasicType() {
		tokens.expectingIdent().expecting("basic type");
	}

	private BasicType parseBasicType() throws ParseException {
		Token token = tokens.expectingIdent().expecting("basic type").peek();
		Optional<IdentToken> identToken = token.asIdentToken();

		BasicType type;
		if (identToken.isPresent()) {
			type = new IdentType(Ident.fromToken(identToken.get()));
		} else if (token.isKeyword(Keyword.INT)) {
			type = new IntType();
		} else if (token.isKeyword(Keyword.BOOLEAN)) {
			type = new BooleanType();
		} else if (token.isKeyword(Keyword.VOID)) {
			type = new VoidType();
		} else {
			type = tokens.error();
		}
		tokens.take();

		return type;
	}

	private Ident parseIdent() throws ParseException {
		return Ident.fromToken(tokens.expectIdent());
	}
}
