package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.tokens.Keyword;
import com.github.firmwehr.gentle.parser.tokens.Operator;
import com.github.firmwehr.gentle.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

		while (tokens.peek().isKeyword(Keyword.CLASS)) {
			classDeclarations.add(parseClassDeclaration());
		}

		return new Program(classDeclarations);
	}

	private ClassDeclaration parseClassDeclaration() throws ParseException {
		tokens.expectKeyword(Keyword.CLASS);

		Ident name = Ident.fromToken(tokens.expectIdent());

		tokens.expectOperator(Operator.LEFT_BRACE);
		// FIXME Parse class contents
		tokens.expectOperator(Operator.RIGHT_BRACE);

		return new ClassDeclaration(name, List.of(), List.of(), List.of());
	}
}
