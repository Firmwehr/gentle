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
import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.type.ArrayType;
import com.github.firmwehr.gentle.parser.ast.type.BooleanType;
import com.github.firmwehr.gentle.parser.ast.type.IdentType;
import com.github.firmwehr.gentle.parser.ast.type.IntType;
import com.github.firmwehr.gentle.parser.ast.type.Type;
import com.github.firmwehr.gentle.parser.ast.type.VoidType;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;
import com.github.firmwehr.gentle.parser.tokens.Keyword;
import com.github.firmwehr.gentle.parser.tokens.Operator;
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

		Type parameterType = parseType();
		Ident parameterName = parseIdent();

		tokens.expectOperator(Operator.RIGHT_PAREN);
		parseOptionalMethodRest();

		Block block = parseBlock();

		return new MainMethod(name, parameterType, parameterName, block);
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
		// FIXME Implement properly
		tokens.expectOperator(Operator.LEFT_BRACE);
		tokens.expectOperator(Operator.RIGHT_BRACE);
		return new Block(List.of());
	}

	private Type parseType() throws ParseException {
		Token typeToken = tokens.expectingIdent().expecting("basic type").peek();
		Optional<IdentToken> typeIdentToken = typeToken.asIdentToken();

		Type type;
		if (typeIdentToken.isPresent()) {
			type = new IdentType(Ident.fromToken(typeIdentToken.get()));
		} else if (typeToken.isKeyword(Keyword.INT)) {
			type = new IntType();
		} else if (typeToken.isKeyword(Keyword.BOOLEAN)) {
			type = new BooleanType();
		} else if (typeToken.isKeyword(Keyword.VOID)) {
			type = new VoidType();
		} else {
			type = tokens.error();
		}
		tokens.take();

		while (tokens.expectingOperator(Operator.LEFT_BRACKET).peek().isOperator(Operator.LEFT_BRACKET)) {
			tokens.expectOperator(Operator.LEFT_BRACKET);
			tokens.expectOperator(Operator.RIGHT_BRACKET);
			type = new ArrayType(type);
		}

		return type;
	}

	private Ident parseIdent() throws ParseException {
		return Ident.fromToken(tokens.expectIdent());
	}

}
