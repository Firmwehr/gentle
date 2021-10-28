package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.Token;
import com.github.firmwehr.gentle.lexer.tokens.TokenComment;
import com.github.firmwehr.gentle.lexer.tokens.TokenEndOfFile;
import com.github.firmwehr.gentle.lexer.tokens.TokenIdentifier;
import com.github.firmwehr.gentle.lexer.tokens.TokenIntegerLiteral;
import com.github.firmwehr.gentle.lexer.tokens.TokenKeyword;
import com.github.firmwehr.gentle.lexer.tokens.TokenWhitespace;
import com.github.firmwehr.gentle.util.TokenTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public enum TokenType {

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
	ASSIGN_BITWISE_AND("&="),
	LOGICAL_AND("&&"),
	BITWISE_AND("&"),
	LEFT_BRACKET("["),
	RIGHT_BRACKET("]"),
	ASSIGN_BITWISE_XOR("^="),
	BITWISE_XOR("^"),
	LEFT_BRACE("{"),
	RIGHT_BRACE("}"),
	BITWISE_NOT("~"),
	ASSIGN_BITWISE_OR("|="),
	LOGICAL_OR("||"),
	BITWISE_OR("|"),

	EOF(TokenEndOfFile::create),
	WHITESPACE(TokenWhitespace::create),
	COMMENT(TokenComment::create),
	INTEGER_LITERAL(TokenIntegerLiteral::create),
	IDENTIFIER(TokenIdentifier::create);

	private static final TokenTrie<TokenType> KEYWORD_TRIE = new TokenTrie<>();
	private static final List<TokenType> NON_KEYWORD_LIST = new ArrayList<>();

	static {
		for (var tokenType : values()) {
			tokenType.keyword.ifPresentOrElse(s -> KEYWORD_TRIE.put(s, tokenType),
				() -> NON_KEYWORD_LIST.add(tokenType));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TokenType.class);

	private final ParserBinding parser;

	private final Optional<String> keyword;

	TokenType(TokenFactory factory) {
		this.keyword = Optional.empty();
		parser = (reader, tokenType) -> factory.attemptParse(reader);
	}

	// special handling for keywords since these are already bound to their type by their factory
	TokenType(String keyword) {
		this.keyword = Optional.of(keyword);
		parser = (reader, tokenType) -> TokenKeyword.create(reader, tokenType, keyword);
	}

	public static ParsedToken parseNextToken(LexReader reader) throws LexerException {

		// check trie for keywords first
		var keywordReader = reader.fork(); // fork reader in case next token is not keyword
		var traversal = KEYWORD_TRIE.startTraversal();
		Optional<TokenType> maybeTokenType = Optional.empty();
		while (true) {
			// end of input means aborting traversal (might still be valid token)
			if (keywordReader.isEndOfInput()) {
				break;
			}

			// try to continue traversal with current codepoint
			int cp = keywordReader.peek();
			if (!traversal.advance(cp)) {
				break;
			}

			// traversal successfull, advance reader and store current element in case next round ends traversal
			maybeTokenType = traversal.current();
			keywordReader.consume();
		}

		// if no valid token type is found during traversal, this might still be empty
		if (maybeTokenType.isPresent()) {
			// found matching keyword, parse it
			keywordReader = reader.fork(); // we need a new fork since the old one had already advanced behind keyword
			var tokenType = maybeTokenType.get();
			var token = tokenType.attemptParse(keywordReader);

			/* you might believe that parsing the token is optional at this point but actually just because we have
			 * succesfully identified a keyword does not mean that it will also parse successfully.
			 *
			 * Take the identifier "charisma" for example. It starts with "char" and does not complete into any other
			 * keyword. But it's not a keyword. So we still have to call the keyword specific parse logic to check.
			 *
			 * If parsing the token fails then we still have to advance to the non keyword list.
			 */
			if (token.isPresent()) {
				return new ParsedToken(token.get(), keywordReader);
			}
		}

		// if trie traversal did not yield token, input must be non-keyword (or invalid)
		for (var tokenType : NON_KEYWORD_LIST) {
			// copy reader for each attempt
			var childReader = reader.fork();
			var maybeToken = tokenType.attemptParse(childReader);

			if (maybeToken.isEmpty()) {  // no match
				continue;
			}

			var token = maybeToken.get();
			return new ParsedToken(token, childReader);
		}

		throw new LexerException("unable to find suitable token", reader);
	}

	public boolean isKeyword() {
		return keyword.isPresent();
	}

	private Optional<Token> attemptParse(LexReader reader) {
		try {
			var token = parser.callParser(reader, this);
			if (token.tokenType() != this) {
				throw new Error(
					"Descriptor for %s created token of wrong tokenType, please check enum definition".formatted(this));
			}
			return Optional.of(token);
		} catch (LexerException e) {
			//LOGGER.trace("rejecting {}: {}", this, e.getMessage());
			return Optional.empty();
		}
	}

	public Optional<String> keyword() {
		return keyword;
	}

	public record ParsedToken(
		Token token,
		LexReader reader
	) {
	}

	@FunctionalInterface
	public interface TokenFactory {

		Token attemptParse(LexReader reader) throws LexerException;
	}

	@FunctionalInterface
	private interface ParserBinding {

		Token callParser(LexReader reader, TokenType tokenType) throws LexerException;
	}
}
