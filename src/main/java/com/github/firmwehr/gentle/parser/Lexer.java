package com.github.firmwehr.gentle.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class Lexer {
	
	// All keywords and all symbols are handled very similarly.
	// Therefore we save their string->tokentype mappings in these collections
	public static final HashMap<String, TokenType> KEYWORDS = new HashMap<>();
	// Can't be final if set in static block apparently
	public static final int MAX_KEYWORD_LENGTH;
	// Tries are useful for reading symbols character by character, see lexSymbolOrError()
	public static final SymbolTrie<TokenType> SYMBOLS = new SymbolTrie<>();
	
	static {
		Lexer.KEYWORDS.put("abstract", TokenType.ABSTRACT);
		Lexer.KEYWORDS.put("assert", TokenType.ASSERT);
		Lexer.KEYWORDS.put("boolean", TokenType.BOOLEAN);
		Lexer.KEYWORDS.put("break", TokenType.BREAK);
		Lexer.KEYWORDS.put("byte", TokenType.BYTE);
		Lexer.KEYWORDS.put("case", TokenType.CASE);
		Lexer.KEYWORDS.put("catch", TokenType.CATCH);
		Lexer.KEYWORDS.put("char", TokenType.CHAR);
		Lexer.KEYWORDS.put("class", TokenType.CLASS);
		Lexer.KEYWORDS.put("const", TokenType.CONST);
		Lexer.KEYWORDS.put("continue", TokenType.CONTINUE);
		Lexer.KEYWORDS.put("default", TokenType.DEFAULT);
		Lexer.KEYWORDS.put("double", TokenType.DOUBLE);
		Lexer.KEYWORDS.put("do", TokenType.DO);
		Lexer.KEYWORDS.put("else", TokenType.ELSE);
		Lexer.KEYWORDS.put("enum", TokenType.ENUM);
		Lexer.KEYWORDS.put("extends", TokenType.EXTENDS);
		Lexer.KEYWORDS.put("false", TokenType.FALSE);
		Lexer.KEYWORDS.put("finally", TokenType.FINALLY);
		Lexer.KEYWORDS.put("final", TokenType.FINAL);
		Lexer.KEYWORDS.put("float", TokenType.FLOAT);
		Lexer.KEYWORDS.put("for", TokenType.FOR);
		Lexer.KEYWORDS.put("goto", TokenType.GOTO);
		Lexer.KEYWORDS.put("if", TokenType.IF);
		Lexer.KEYWORDS.put("implements", TokenType.IMPLEMENTS);
		Lexer.KEYWORDS.put("import", TokenType.IMPORT);
		Lexer.KEYWORDS.put("instanceof", TokenType.INSTANCEOF);
		Lexer.KEYWORDS.put("interface", TokenType.INTERFACE);
		Lexer.KEYWORDS.put("int", TokenType.INT);
		Lexer.KEYWORDS.put("long", TokenType.LONG);
		Lexer.KEYWORDS.put("native", TokenType.NATIVE);
		Lexer.KEYWORDS.put("new", TokenType.NEW);
		Lexer.KEYWORDS.put("null", TokenType.NULL);
		Lexer.KEYWORDS.put("package", TokenType.PACKAGE);
		Lexer.KEYWORDS.put("private", TokenType.PRIVATE);
		Lexer.KEYWORDS.put("protected", TokenType.PROTECTED);
		Lexer.KEYWORDS.put("public", TokenType.PUBLIC);
		Lexer.KEYWORDS.put("return", TokenType.RETURN);
		Lexer.KEYWORDS.put("short", TokenType.SHORT);
		Lexer.KEYWORDS.put("static", TokenType.STATIC);
		Lexer.KEYWORDS.put("strictfp", TokenType.STRICTFP);
		Lexer.KEYWORDS.put("super", TokenType.SUPER);
		Lexer.KEYWORDS.put("switch", TokenType.SWITCH);
		Lexer.KEYWORDS.put("synchronized", TokenType.SYNCHRONIZED);
		Lexer.KEYWORDS.put("this", TokenType.THIS);
		Lexer.KEYWORDS.put("throws", TokenType.THROWS);
		Lexer.KEYWORDS.put("throw", TokenType.THROW);
		Lexer.KEYWORDS.put("transient", TokenType.TRANSIENT);
		Lexer.KEYWORDS.put("true", TokenType.TRUE);
		Lexer.KEYWORDS.put("try", TokenType.TRY);
		Lexer.KEYWORDS.put("void", TokenType.VOID);
		Lexer.KEYWORDS.put("volatile", TokenType.VOLATILE);
		Lexer.KEYWORDS.put("while", TokenType.WHILE);
		
		MAX_KEYWORD_LENGTH = Lexer.KEYWORDS.keySet().stream().mapToInt(kw -> kw.length()).max().getAsInt();
		
		// The order of the symbols is the same as in sprachbericht.pdf.
		// The order is not relevant for lookup speed.
		// This way it's easier to check whether all symbols are handled tho.
		Lexer.SYMBOLS.put("!=", TokenType.NOT_EQUALS);
		Lexer.SYMBOLS.put("!", TokenType.LOGICAL_NOT);
		Lexer.SYMBOLS.put("(", TokenType.LEFT_PAREN);
		Lexer.SYMBOLS.put(")", TokenType.RIGHT_PAREN);
		Lexer.SYMBOLS.put("*=", TokenType.ASSIGN_MULTIPLY);
		Lexer.SYMBOLS.put("*", TokenType.MULTIPLY);
		Lexer.SYMBOLS.put("++", TokenType.POSTFIX_INCREMENT);
		Lexer.SYMBOLS.put("+=", TokenType.ASSIGN_ADD);
		Lexer.SYMBOLS.put("+", TokenType.ADD);
		Lexer.SYMBOLS.put(",", TokenType.COMMA);
		Lexer.SYMBOLS.put("-=", TokenType.ASSIGN_SUBTRACT);
		Lexer.SYMBOLS.put("--", TokenType.POSTFIX_DECREMENT);
		Lexer.SYMBOLS.put("-", TokenType.SUBTRACT);
		Lexer.SYMBOLS.put(".", TokenType.DOT);
		// There is an impostor among us: This "symbol" needs to be handled separately
		Lexer.SYMBOLS.put("/*", TokenType.COMMENT);
		Lexer.SYMBOLS.put("/=", TokenType.ASSIGN_DIVIDE);
		Lexer.SYMBOLS.put("/", TokenType.DIVIDE);
		Lexer.SYMBOLS.put(":", TokenType.COLON);
		Lexer.SYMBOLS.put(";", TokenType.SEMICOLON);
		Lexer.SYMBOLS.put("<<=", TokenType.ASSIGN_BITSHIFT_LEFT);
		Lexer.SYMBOLS.put("<<", TokenType.BITSHIFT_LEFT);
		Lexer.SYMBOLS.put("<=", TokenType.LESS_THAN);
		Lexer.SYMBOLS.put("<", TokenType.LESS_THAN_EQUALS);
		Lexer.SYMBOLS.put("==", TokenType.EQUALS);
		Lexer.SYMBOLS.put("=", TokenType.ASSIGN);
		Lexer.SYMBOLS.put(">=", TokenType.GREATER_THAN_EQUALS);
		Lexer.SYMBOLS.put(">>=", TokenType.ASSIGN_BITSHIFT_RIGHT);
		Lexer.SYMBOLS.put(">>>=", TokenType.ASSIGN_BITSHIFT_RIGHT_ZEROFILL);
		Lexer.SYMBOLS.put(">>>", TokenType.BITSHIFT_RIGHT_ZEROFILL);
		Lexer.SYMBOLS.put(">>", TokenType.BITSHIFT_RIGHT);
		Lexer.SYMBOLS.put(">", TokenType.GREATER_THAN);
		Lexer.SYMBOLS.put("?", TokenType.QUESTION_MARK);
		Lexer.SYMBOLS.put("%=", TokenType.ASSIGN_MODULO);
		Lexer.SYMBOLS.put("%", TokenType.MODULO);
		Lexer.SYMBOLS.put("&=", TokenType.ASSIGN_BITWISE_AND);
		Lexer.SYMBOLS.put("&&", TokenType.LOGICAL_AND);
		Lexer.SYMBOLS.put("&", TokenType.BITWISE_AND);
		Lexer.SYMBOLS.put("[", TokenType.LEFT_BRACKET);
		Lexer.SYMBOLS.put("]", TokenType.RIGHT_BRACKET);
		Lexer.SYMBOLS.put("^=", TokenType.ASSIGN_BITWISE_XOR);
		Lexer.SYMBOLS.put("^", TokenType.BITWISE_XOR);
		Lexer.SYMBOLS.put("{", TokenType.LEFT_BRACE);
		Lexer.SYMBOLS.put("}", TokenType.RIGHT_BRACE);
		Lexer.SYMBOLS.put("~", TokenType.BITWISE_NOT);
		Lexer.SYMBOLS.put("|=", TokenType.ASSIGN_BITWISE_OR);
		Lexer.SYMBOLS.put("||", TokenType.LOGICAL_OR);
		Lexer.SYMBOLS.put("|", TokenType.BITWISE_OR);
	}
	
	// Used to read the input character by character
	private final BufferedReader input;
	// Used to incrementally build the token text for each consumed character
	private final StringBuilder sb;
	// This field is only ever set using BufferedReader#read()
	// Therefore it is either a character or -1 (meaning EOF was reached)
	private int lookahead;
	// line and column describe the position of the character in lookahead
	private final int line;
	private final int column;
	
	public Lexer(BufferedReader input) throws IOException {
		this.input = input;
		
		this.sb = new StringBuilder();
		this.lookahead = this.input.read();
		this.line = 0;
		this.column = 0;
	}
	
	// Character classification functions
	
	private static boolean isWhitespace(int c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r';
	}
	
	private static boolean isIdentifierStart(int c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}
	
	private static boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}
	
	// lex() is the method you'd use in a parser, returning only tokens that are semantically relevant
	// lexIncludingWhitespaceAndComments() returns all tokens in the CST
	// the remaining lex* methods are private and for internal use only (they may have unexpected conditions!)
	// All lex* methods potentially consume characters in this.input
	
	// Invariant: lex().type() != WHITESPACE && lex().type() != COMMENT
	public Token lex() throws IOException {
		while (true) {
			Token t = this.lexIncludingWhitespaceAndComments();
			if (t.type() != TokenType.WHITESPACE && t.type() != TokenType.COMMENT) {
				return t;
			}
		}
	}
	
	public Token lexIncludingWhitespaceAndComments() throws IOException {
		this.resetSb();
		
		if (this.lookahead == -1) {
			return this.emit(TokenType.EOF);
		}
		
		if (Lexer.isWhitespace(this.lookahead)) {
			return this.lexWhitespace();
		}
		
		if (Lexer.isIdentifierStart(this.lookahead)) {
			return this.lexIdentifierOrKeyword();
		}
		
		if (Lexer.isDigit(this.lookahead)) {
			return this.lexIntegerLiteral();
		}
		
		// Symbol parsing logic is heavy, moved to its own method
		return this.lexSymbolCommentOrError();
	}
	
	private Token lexWhitespace() throws IOException {
		while (Lexer.isWhitespace(this.lookahead)) {
			this.consume();
		}
		return this.emit(TokenType.WHITESPACE);
	}
	
	private Token lexIdentifierOrKeyword() throws IOException {
		while (Lexer.isIdentifierStart(this.lookahead) || Lexer.isDigit(this.lookahead)) {
			this.consume();
		}
		
		return this.emitIdentifierOrKeyword();
	}
	
	// TODO: Literals start with [1-9]
	private Token lexIntegerLiteral() throws IOException {
		while (Lexer.isDigit(this.lookahead)) {
			this.consume();
		}
		return this.emit(TokenType.INTEGER_LITERAL);
	}
	
	private Token lexSymbolCommentOrError() throws IOException {
		// Greedily read the next symbol using the symbol trie
		SymbolTrie.STNode<TokenType> node = Lexer.SYMBOLS.getRoot();
		// The casts are kind of ugly but needed as this.lookahead is an int
		while (node.getChildren().containsKey((char) this.lookahead)) {
			node = node.getChildren().get((char) this.lookahead);
			this.consume();
		}
		
		// Still at the root node ==> no symbol was read
		if (node.getContent().isEmpty()) {
			throw new RuntimeException("Symbol or comment expected");
		}
		// Comment "operator" ==> special case, read rest of comment
		if (node.getContent().get() == TokenType.COMMENT) {
			return this.lexCommentRest();
		}
		// Otherwise, the node contains a valid symbol
		return this.emit(node.getContent().get());
	}
	
	// TODO: This can probably be improved somehow, seems ugly
	// Reads input until */ is encountered
	private Token lexCommentRest() throws IOException {
		while (true) {
			switch (this.lookahead) {
				case '*':
					this.consume();
					switch (this.lookahead) {
						case '/':
							this.consume();
							return this.emit(TokenType.COMMENT);
						default:
							// **Don't consume next token, restart loop**
							continue;
					}
				case -1:
					throw new RuntimeException("Unterminated comment");
			}
			this.consume();
		}
	}
	
	// Helper methods for lex* methods
	
	private Token emit(TokenType type) {
		return new Token(type, this.sb.toString());
	}
	
	private Token emitIdentifierOrKeyword() {
		String text = this.sb.toString();
		
		if (text.length() <= Lexer.MAX_KEYWORD_LENGTH && Lexer.KEYWORDS.containsKey(text)) {
			return new Token(Lexer.KEYWORDS.get(text), text);
		}
		
		return new Token(TokenType.IDENTIFIER, text);
	}
	
	// TODO: This method is called whenever a new token is started
	// TODO: Remember the position so it can be passed to the new token in emit()
	private void resetSb() {
		// Only resets the length, not the capacity.
		this.sb.setLength(0);
	}
	
	private void consume() throws IOException {
		this.sb.append((char) this.lookahead);
		this.lookahead = this.input.read();
	}
}