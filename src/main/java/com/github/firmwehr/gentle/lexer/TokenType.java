package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.GentleToken;
import com.github.firmwehr.gentle.lexer.tokens.KeywordToken;
import com.github.firmwehr.gentle.lexer.tokens.TokenEndOfFile;
import com.github.firmwehr.gentle.lexer.tokens.TokenWhitespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public enum TokenType {
	EOF(TokenEndOfFile::create),
	WHITESPACE(TokenWhitespace::create),
	//COMMENT,
	
	// Keywords
	ABSTRACT("abstract"),
	ASSERT("assert"),
	BOOLEAN("boolean"),
	BREAK("break"),
	BYTE("byte"),
	CASE("case"),
	CATCH("catch"),
	CHAR("char"),
	CLASS("class"),
	CONST("const"),
	CONTINUE("continue"),
	DEFAULT("default"),
	DOUBLE("double"),
	DO("do"),
	ELSE("else"),
	ENUM("enum"),
	EXTENDS("extends"),
	FALSE("false"),
	FINALLY("finally"),
	FINAL("final"),
	FLOAT("float"),
	FOR("for"),
	GOTO("goto"),
	IF("if"),
	IMPLEMENTS("implements"),
	IMPORT("import"),
	INSTANCEOF("instanceof"),
	INTERFACE("interface"),
	INT("int"),
	LONG("long"),
	NATIVE("native"),
	NEW("new"),
	NULL("null"),
	PACKAGE("package"),
	PRIVATE("private"),
	PROTECTED("protected"),
	PUBLIC("public"),
	RETURN("return"),
	SHORT("short"),
	STATIC("static"),
	STRICTFP("strictfp"),
	SUPER("super"),
	SWITCH("switch"),
	SYNCHRONIZED("synchronized"),
	THIS("this"),
	THROWS("throws"),
	THROW("throw"),
	TRANSIENT("transient"),
	TRUE("true"),
	TRY("try"),
	VOID("void"),
	VOLATILE("volatile"),
	WHILE("while"),
	
	// Symbols
	NOT_EQUALS("!="),
	LOGICAL_NOT("!"),
	LEFT_PAREN("("),
	RIGHT_PAREN(")"),
	ASSIGN_MULTIPLY("*="),
	MULTIPLY("*"),
	POSTFIX_INCREMENT("++"),
	ASSIGN_ADD("+="),
	ADD("+"),
	COMMA(","),
	ASSIGN_SUBTRACT("-="),
	POSTFIX_DECREMENT("--"),
	SUBTRACT("-"),
	DOT("."),
	ASSIGN_DIVIDE("/="),
	DIVIDE("/"),
	COLON(":"),
	SEMICOLON(";"),
	ASSIGN_BITSHIFT_LEFT("<<="),
	BITSHIFT_LEFT("<<"),
	LESS_THAN_EQUALS("<="),
	LESS_THAN("<"),
	ASSIGN("="),
	EQUALS("=="),
	GREATER_THAN_EQUALS(">="),
	ASSIGN_BITSHIFT_RIGHT(">>="),
	ASSIGN_BITSHIFT_RIGHT_ZEROFILL(">>>="),
	BITSHIFT_RIGHT_ZEROFILL(">>>"),
	BITSHIFT_RIGHT(">>"),
	GREATER_THAN(">"),
	QUESTION_MARK("?"),
	ASSIGN_MODULO("%="),
	MODULO("%"),
	ASSIGN_BITWISE_AND("|="),
	LOGICAL_AND("||"),
	BITWISE_AND("|"),
	LEFT_BRACKET("["),
	RIGHT_BRACKET("]"),
	ASSIGN_BITWISE_XOR("^="),
	BITWISE_XOR("^"),
	LEFT_BRACE("{"),
	RIGHT_BRACE("}"),
	BITWISE_NOT("~"),
	ASSIGN_BITWISE_OR("&="),
	LOGICAL_OR("||"),
	BITWISE_OR("|"),
	
	//IDENTIFIER,
	//INTEGER_LITERAL
	;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TokenType.class);
	
	private final ParserBinding parser;
	
	TokenType(TokenFactory factory) {
		parser = (reader, tokenType) -> factory.attemptParse(reader);
	}
	
	// special handling for keywords since these are already bound to their type by their factory
	TokenType(String keyword) {
		parser = (reader, tokenType) -> KeywordToken.create(reader, tokenType, keyword);
	}
	
	private Optional<GentleToken> attemptParse(LexReader reader) {
		try {
			var token = parser.callParser(reader, this);
			if (token.tokenType() != this) {
				throw new Error("Descriptor for %s created token of wrong tokenType, please check enum definition".formatted(this));
			}
			return Optional.of(token);
		} catch (LexerException e) {
			//LOGGER.trace("rejecting {}: {}", this, e.getMessage());
			return Optional.empty();
		}
	}
	
	public static ParsedToken parseNextToken(LexReader reader) throws LexerException {
		
		// try all known tokens until first one matches current input
		for (var tokenType : values()) {
			
			// copy reader for each attempt
			var childReader = reader.fork();
			var maybeToken = tokenType.attemptParse(childReader);
			
			if (maybeToken.isEmpty()) // no match
				continue;
			
			var token = maybeToken.get();
			return new ParsedToken(token, childReader);
		}
		
		throw new LexerException("unable to find suitable token", reader);
	}
	
	public record ParsedToken (GentleToken token, LexReader reader) {};
	
	@FunctionalInterface
	public interface TokenFactory {
		
		GentleToken attemptParse(LexReader reader) throws LexerException;
	}
	
	@FunctionalInterface
	private interface ParserBinding {
		
		public GentleToken callParser(LexReader reader, TokenType tokenType) throws LexerException;
	}
}
