package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ast.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Parser {
	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

	private final Tokens tokens;

	public Parser(Tokens tokens) {
		this.tokens = tokens;
	}

	public static Parser fromLexer(Lexer lexer) throws LexerException {
		return new Parser(Tokens.fromLexer(lexer));
	}

	public Program parse() {
		// FIXME Implement
		LOGGER.info("Parsing, bleep bloop");
		return new Program(List.of());
	}
}
