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
import com.github.firmwehr.gentle.parser.ast.expression.ArrayAccessExpression;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperatorExpression;
import com.github.firmwehr.gentle.parser.ast.expression.BooleanLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.expression.FieldAccessExpression;
import com.github.firmwehr.gentle.parser.ast.expression.IdentExpression;
import com.github.firmwehr.gentle.parser.ast.expression.IntegerLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.expression.LocalMethodCallExpression;
import com.github.firmwehr.gentle.parser.ast.expression.MethodInvocationExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NewArrayExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NewObjectExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NullExpression;
import com.github.firmwehr.gentle.parser.ast.expression.ThisExpression;
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperatorExpression;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class Parser {

	private final Tokens tokens;

	public Parser(Tokens tokens) {
		this.tokens = tokens;
	}

	public static Parser fromLexer(Source source, Lexer lexer) throws LexerException {
		return new Parser(Tokens.fromLexer(source, lexer));
	}

	public Program parse() throws ParseException {
		List<ClassDeclaration> classDeclarations = new ArrayList<>();

		while (tokens.expecting(ExpectedToken.CLASS).peek().isKeyword(Keyword.CLASS)) {
			classDeclarations.add(parseClassDeclaration());
		}

		tokens.expecting(ExpectedToken.EOF).expectEof();

		return new Program(classDeclarations);
	}


	private ClassDeclaration parseClassDeclaration() throws ParseException {
		tokens.expecting(ExpectedToken.CLASS).expectKeyword(Keyword.CLASS);

		Ident name = parseIdent();

		tokens.expecting(ExpectedToken.LEFT_BRACE).expectOperator(Operator.LEFT_BRACE);

		List<Field> fields = new ArrayList<>();
		List<Method> methods = new ArrayList<>();
		List<MainMethod> mainMethods = new ArrayList<>();
		while (tokens.expecting(ExpectedToken.PUBLIC).peek().isKeyword(Keyword.PUBLIC)) {
			parseClassMember(fields, methods, mainMethods);
		}

		tokens.expecting(ExpectedToken.RIGHT_BRACE).expectOperator(Operator.RIGHT_BRACE);

		return new ClassDeclaration(name, fields, methods, mainMethods);
	}

	private void parseClassMember(List<Field> fields, List<Method> methods, List<MainMethod> mainMethods)
		throws ParseException {

		tokens.expecting(ExpectedToken.PUBLIC).expectKeyword(Keyword.PUBLIC);

		if (tokens.expecting(ExpectedToken.STATIC).peek().isKeyword(Keyword.STATIC)) {
			mainMethods.add(parseMainMethodRest());
		} else {
			// This can either be a field or a method, and we don't know until after the ident
			Type type = parseType();
			Ident ident = parseIdent();

			tokens.expecting(ExpectedToken.SEMICOLON).expecting(ExpectedToken.LEFT_PAREN);
			if (tokens.peek().isOperator(Operator.SEMICOLON)) {
				tokens.take();
				fields.add(new Field(type, ident));
			} else if (tokens.peek().isOperator(Operator.LEFT_PAREN)) {
				methods.add(parseMethodRest(type, ident));
			} else {
				tokens.error();
			}
		}
	}

	private MainMethod parseMainMethodRest() throws ParseException {
		tokens.expecting(ExpectedToken.STATIC).expectKeyword(Keyword.STATIC);
		tokens.expecting(ExpectedToken.VOID).expectKeyword(Keyword.VOID);

		Ident name = parseIdent();

		tokens.expecting(ExpectedToken.LEFT_PAREN).expectOperator(Operator.LEFT_PAREN);

		Parameter parameter = parseParameter();

		tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);
		parseOptionalMethodRest();

		Block block = parseBlock();

		return new MainMethod(name, parameter, block);
	}

	private Method parseMethodRest(Type type, Ident ident) throws ParseException {
		tokens.expecting(ExpectedToken.LEFT_PAREN).expectOperator(Operator.LEFT_PAREN);

		List<Parameter> parameters = new ArrayList<>();
		if (!tokens.expecting(ExpectedToken.RIGHT_PAREN).peek().isOperator(Operator.RIGHT_PAREN)) {
			parameters.add(parseParameter());

			while (tokens.expecting(ExpectedToken.COMMA).peek().isOperator(Operator.COMMA)) {
				tokens.take();
				parameters.add(parseParameter());
			}
		}

		tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);
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
		if (tokens.expecting(ExpectedToken.THROWS).peek().isKeyword(Keyword.THROWS)) {
			tokens.take();
			tokens.expecting(ExpectedToken.IDENTIFIER).expectIdent();
		}
	}

	private Block parseBlock() throws ParseException {
		tokens.expecting(ExpectedToken.LEFT_BRACE).expectOperator(Operator.LEFT_BRACE);

		List<BlockStatement> statements = new ArrayList<>();
		while (!tokens.expecting(ExpectedToken.RIGHT_BRACE).peek().isOperator(Operator.RIGHT_BRACE)) {
			statements.add(parseBlockStatement());
		}

		tokens.expecting(ExpectedToken.RIGHT_BRACE).expectOperator(Operator.RIGHT_BRACE);

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
		if (tokens.expecting(ExpectedToken.ASSIGN).peek().isOperator(Operator.ASSIGN)) {
			tokens.take();
			value = Optional.of(parseExpression());
		} else {
			value = Optional.empty();
		}

		tokens.expecting(ExpectedToken.SEMICOLON).expectOperator(Operator.SEMICOLON);

		return new LocalVariableDeclarationStatement(type, name, value);
	}

	private void expectingStatement() {
		tokens.expecting(ExpectedToken.LEFT_BRACE)
			.expecting(ExpectedToken.SEMICOLON)
			.expecting(ExpectedToken.IF)
			.expecting(ExpectedToken.WHILE)
			.expecting(ExpectedToken.RETURN);
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
		tokens.expecting(ExpectedToken.IF).expectKeyword(Keyword.IF);
		tokens.expecting(ExpectedToken.LEFT_PAREN).expectOperator(Operator.LEFT_PAREN);

		Expression condition = parseExpression();

		tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);

		Statement body = parseStatement();

		Optional<Statement> elseBody;
		if (tokens.expecting(ExpectedToken.ELSE).peek().isKeyword(Keyword.ELSE)) {
			tokens.take();
			elseBody = Optional.of(parseStatement());
		} else {
			elseBody = Optional.empty();
		}

		return new IfStatement(condition, body, elseBody);
	}

	private WhileStatement parseWhileStatement() throws ParseException {
		tokens.expecting(ExpectedToken.WHILE).expectKeyword(Keyword.WHILE);
		tokens.expecting(ExpectedToken.LEFT_PAREN).expectOperator(Operator.LEFT_PAREN);

		Expression condition = parseExpression();

		tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);

		Statement body = parseStatement();

		return new WhileStatement(condition, body);
	}

	private ReturnStatement parseReturnStatement() throws ParseException {
		tokens.expecting(ExpectedToken.RETURN).expectKeyword(Keyword.RETURN);

		Optional<Expression> returnValue;
		if (tokens.expecting(ExpectedToken.SEMICOLON).peek().isOperator(Operator.SEMICOLON)) {
			returnValue = Optional.empty();
		} else {
			returnValue = Optional.of(parseExpression());
		}

		tokens.expecting(ExpectedToken.SEMICOLON).expectOperator(Operator.SEMICOLON);

		return new ReturnStatement(returnValue);
	}

	private ExpressionStatement parseExpressionStatement() throws ParseException {
		Expression expression = parseExpression();

		tokens.expecting(ExpectedToken.SEMICOLON).expectOperator(Operator.SEMICOLON);

		return new ExpressionStatement(expression);
	}

	private void expectingExpression() {
		expectingPrimaryExpression();
		tokens.expecting(ExpectedToken.LOGICAL_NOT).expecting(ExpectedToken.MINUS);
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
		Token token = tokens.expecting(ExpectedToken.BINARY_OPERATOR).peek();
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
			Optional<Expression> postfixedExpression = parseOptionalPostfixOp(expression);
			if (postfixedExpression.isPresent()) {
				expression = postfixedExpression.get();
			} else {
				break;
			}
		}

		return expression;
	}

	private Optional<Expression> parseOptionalPostfixOp(Expression expression) throws ParseException {
		Token token = tokens.expecting(ExpectedToken.DOT).expecting(ExpectedToken.LEFT_BRACKET).peek();

		if (token.isOperator(Operator.DOT)) {
			tokens.take();
			Ident name = parseIdent();
			if (tokens.expecting(ExpectedToken.LEFT_PAREN).peek().isOperator(Operator.LEFT_PAREN)) {
				return Optional.of(new MethodInvocationExpression(expression, name, parseParenthesisedArguments()));
			} else {
				return Optional.of(new FieldAccessExpression(expression, name));
			}
		} else if (token.isOperator(Operator.LEFT_BRACKET)) {
			tokens.take();
			Expression index = parseExpression();
			tokens.expecting(ExpectedToken.RIGHT_BRACKET).expectOperator(Operator.RIGHT_BRACKET);
			return Optional.of(new ArrayAccessExpression(expression, index));
		} else {
			return Optional.empty();
		}
	}

	private List<Expression> parseParenthesisedArguments() throws ParseException {
		tokens.expecting(ExpectedToken.LEFT_PAREN).expectOperator(Operator.LEFT_PAREN);

		List<Expression> arguments = new ArrayList<>();
		if (!tokens.expecting(ExpectedToken.RIGHT_PAREN).peek().isOperator(Operator.RIGHT_PAREN)) {
			arguments.add(parseExpression());

			while (tokens.expecting(ExpectedToken.COMMA).peek().isOperator(Operator.COMMA)) {
				tokens.take();
				arguments.add(parseExpression());
			}
		}

		tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);

		return arguments;
	}

	private void expectingPrimaryExpression() {
		tokens.expecting(ExpectedToken.NULL)
			.expecting(ExpectedToken.BOOLEAN_LITERAL)
			.expecting(ExpectedToken.INTEGER_LITERAL)
			.expecting(ExpectedToken.IDENTIFIER)
			.expecting(ExpectedToken.THIS)
			.expecting(ExpectedToken.LEFT_PAREN)
			.expecting(ExpectedToken.NEW);
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
			if (tokens.expecting(ExpectedToken.LEFT_PAREN).peek().isOperator(Operator.LEFT_PAREN)) {
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
			tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);
			return expression;
		} else if (token.isKeyword(Keyword.NEW)) {
			return parseNewObjectExpressionOrNewArrayExpression();
		} else {
			return tokens.error();
		}
	}

	private Expression parseNewObjectExpressionOrNewArrayExpression() throws ParseException {
		tokens.expecting(ExpectedToken.NEW).expectKeyword(Keyword.NEW);

		Token token = tokens.expecting(ExpectedToken.IDENTIFIER).expecting(ExpectedToken.BASIC_TYPE).peek();
		Optional<IdentToken> identToken = token.asIdentToken();

		if (identToken.isPresent() && tokens.peek(1).isOperator(Operator.LEFT_PAREN)) {
			tokens.take();
			Ident name = Ident.fromToken(identToken.get());
			tokens.expecting(ExpectedToken.LEFT_PAREN).expectOperator(Operator.LEFT_PAREN);
			tokens.expecting(ExpectedToken.RIGHT_PAREN).expectOperator(Operator.RIGHT_PAREN);
			return new NewObjectExpression(name);
		} else {
			BasicType basicType = parseBasicType();
			tokens.expecting(ExpectedToken.LEFT_BRACKET).expectOperator(Operator.LEFT_BRACKET);
			Expression size = parseExpression();
			tokens.expecting(ExpectedToken.RIGHT_BRACKET).expectOperator(Operator.RIGHT_BRACKET);

			int arrayLevel = 1;
			// This loop checks for the closing bracket in addition to the opening bracket because if there is
			// anything between the brackets, it is considered an array access and not part of the NewArrayExpression.
			//
			// For example, the expression `new int[1][2]` is parsed as a NewArrayExpression `new int[1]` followed by
			// a PostfixExpression with ArrayAccess `[2]` as its single PostfixOp. This means that `new int[1][2]` is
			// equivalent to `(new int[1])[2]`.
			while (tokens.expecting(ExpectedToken.LEFT_BRACKET).peek().isOperator(Operator.LEFT_BRACKET) &&
				tokens.peek(1).isOperator(Operator.RIGHT_BRACKET)) {

				tokens.take();
				tokens.take();
				arrayLevel++;
			}

			return new NewArrayExpression(new Type(basicType, arrayLevel), size);
		}
	}

	private Type parseType() throws ParseException {
		BasicType basicType = parseBasicType();

		int arrayLevel = 0;
		while (tokens.expecting(ExpectedToken.LEFT_BRACKET).peek().isOperator(Operator.LEFT_BRACKET)) {
			tokens.take();
			tokens.expecting(ExpectedToken.RIGHT_BRACKET).expectOperator(Operator.RIGHT_BRACKET);
			arrayLevel++;
		}

		return new Type(basicType, arrayLevel);
	}

	private void expectingBasicType() {
		tokens.expecting(ExpectedToken.IDENTIFIER).expecting(ExpectedToken.BASIC_TYPE);
	}

	private BasicType parseBasicType() throws ParseException {
		Token token = tokens.expecting(ExpectedToken.IDENTIFIER).expecting(ExpectedToken.BASIC_TYPE).peek();
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
		return Ident.fromToken(tokens.expecting(ExpectedToken.IDENTIFIER).expectIdent());
	}
}
