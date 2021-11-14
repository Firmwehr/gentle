package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.parser.tokens.CommentToken;
import com.github.firmwehr.gentle.parser.tokens.EofToken;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;
import com.github.firmwehr.gentle.parser.tokens.IntegerLiteralToken;
import com.github.firmwehr.gentle.parser.tokens.Keyword;
import com.github.firmwehr.gentle.parser.tokens.KeywordToken;
import com.github.firmwehr.gentle.parser.tokens.Operator;
import com.github.firmwehr.gentle.parser.tokens.OperatorToken;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.parser.tokens.WhitespaceToken;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceSpan;
import com.github.firmwehr.gentle.util.string.SimpleStringTable;
import com.github.firmwehr.gentle.util.string.StringTable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Lexer {

	private final StringTable stringTable = new SimpleStringTable();
	private final StringReader reader;
	private final boolean onlyRelevantTokens;

	public Lexer(Source source, boolean onlyRelevantTokens) {
		this.reader = new StringReader(source);
		this.onlyRelevantTokens = onlyRelevantTokens;
	}

	public List<Token> lex() throws LexerException {
		List<Token> tokens = new ArrayList<>();

		while (reader.canRead()) {
			if (isDigit(reader.peek())) {
				tokens.add(readNumber());
				continue;
			}
			if (isIdentifierStart(reader.peek())) {
				tokens.add(readIdentOrKeyword());
				continue;
			}
			if (Character.isWhitespace(reader.peek())) {
				int start = reader.getPosition();
				reader.readWhitespace();
				if (!onlyRelevantTokens) {
					tokens.add(new WhitespaceToken(new SourceSpan(start, reader.getPosition())));
				}
				continue;
			}
			if (reader.canRead(2) && reader.peek(2).equals("/*")) {
				Token comment = readMultilineComment();
				if (!onlyRelevantTokens) {
					tokens.add(comment);
				}
				continue;
			}
			if (reader.canRead(2) && reader.peek(2).equals("//")) {
				Token comment = readSingleLineComment();
				if (!onlyRelevantTokens) {
					tokens.add(comment);
				}
				continue;
			}
			tokens.add(readOperator("Expected keyword, operator, identifier, integer or comment"));
		}
		tokens.add(new EofToken(new SourceSpan(reader.getPosition(), reader.getPosition())));

		return tokens;
	}

	private Token readNumber() {
		int start = reader.getPosition();
		if (reader.peek() == '0') {
			reader.readChar();
			return new IntegerLiteralToken(new SourceSpan(start, reader.getPosition()), BigInteger.ZERO);
		}
		BigInteger integer = new BigInteger(reader.readWhile(this::isDigit));
		return new IntegerLiteralToken(new SourceSpan(start, reader.getPosition()), integer);
	}

	private Token readIdentOrKeyword() throws LexerException {
		if (!isIdentifierPart(reader.peek())) {
			throw new LexerException("Expected an identifier start", reader);
		}
		int start = reader.getPosition();
		String read = reader.readChar() + reader.readWhile(this::isIdentifierPart);

		Optional<Token> keywordToken = getKeywordToken(start, reader.getPosition(), read);
		return keywordToken.orElseGet(
			() -> new IdentToken(new SourceSpan(start, reader.getPosition()), stringTable.deduplicate(read)));
	}

	private Token readOperator(String errorMessage) throws LexerException {
		int start = reader.getPosition();

		Operator operator = switch (reader.readChar()) {
			case '!' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.NOT_EQUAL;
				}
				yield Operator.LOGICAL_NOT;
			}
			case '%' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_MODULO;
				}
				yield Operator.MODULO;
			}
			case '&' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_BITWISE_AND;
				}
				if (reader.peek() == '&') {
					reader.readChar();
					yield Operator.LOGICAL_AND;
				}
				yield Operator.BITWISE_AND;
			}
			case '*' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_MULTIPLY;
				}
				yield Operator.MULTIPLY;
			}
			case '+' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ADD_AND_ASSIGN;
				}
				if (reader.peek() == '+') {
					reader.readChar();
					yield Operator.INCREMENT;
				}
				yield Operator.PLUS;
			}
			case '-' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_SUBTRACT;
				}
				if (reader.peek() == '-') {
					reader.readChar();
					yield Operator.DECREMENT;
				}
				yield Operator.MINUS;
			}
			case '/' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_DIVIDE;
				}
				yield Operator.DIVIDE;
			}
			case '<' -> {
				if (reader.peek() == '<') {
					reader.readChar();
					if (reader.peek() == '=') {
						reader.readChar();
						yield Operator.ASSIGN_LEFT_SHIFT;
					}
					yield Operator.LEFT_SHIFT;
				}
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.LESS_OR_EQUAL;
				}
				yield Operator.LESS_THAN;
			}
			case '=' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.EQUAL;
				}
				yield Operator.ASSIGN;
			}
			case '>' -> {
				if (reader.peek() == '>') {
					reader.readChar();
					if (reader.peek() == '>') {
						reader.readChar();
						if (reader.peek() == '=') {
							reader.readChar();
							yield Operator.ASSIGN_UNSIGNED_RIGHT_SHIFT;
						}
						yield Operator.UNSIGNED_RIGHT_SHIFT;
					}
					if (reader.peek() == '=') {
						reader.readChar();
						yield Operator.ASSIGN_SIGNED_RIGHT_SHIFT;
					}
					yield Operator.SIGNED_RIGHT_SHIFT;
				}
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.GREATER_OR_EQUAL;
				}
				yield Operator.GREATER_THAN;
			}
			case '^' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_BITWISE_XOR;
				}
				yield Operator.BITWISE_XOR;
			}
			case '|' -> {
				if (reader.peek() == '|') {
					reader.readChar();
					yield Operator.LOGICAL_OR;
				}
				if (reader.peek() == '=') {
					reader.readChar();
					yield Operator.ASSIGN_BITWISE_OR;
				}
				yield Operator.BITWISE_OR;
			}
			case '~' -> Operator.BITWISE_NOT;
			case '{' -> Operator.LEFT_BRACE;
			case '}' -> Operator.RIGHT_BRACE;
			case '[' -> Operator.LEFT_BRACKET;
			case ']' -> Operator.RIGHT_BRACKET;
			case '?' -> Operator.QUESTION_MARK;
			case ':' -> Operator.COLON;
			case ';' -> Operator.SEMICOLON;
			case '.' -> Operator.DOT;
			case ',' -> Operator.COMMA;
			case '(' -> Operator.LEFT_PAREN;
			case ')' -> Operator.RIGHT_PAREN;
			default -> {
				// We consumed it in the switch, so we un-consume it now so the error offset matches
				reader.unreadChar();
				throw new LexerException(errorMessage, reader);
			}
		};

		return new OperatorToken(new SourceSpan(start, reader.getPosition()), operator);
	}

	private Token readMultilineComment() throws LexerException {
		int start = reader.getPosition();
		reader.assertRead("/*");
		String text = reader.assertReadUntil("*/");
		text = text.substring(0, text.length() - 2);
		return new CommentToken(new SourceSpan(start, reader.getPosition()), text, CommentToken.Type.MULTI_LINE);
	}

	private Token readSingleLineComment() throws LexerException {
		int start = reader.getPosition();
		reader.assertRead("//");
		String text = reader.readLine();
		return new CommentToken(new SourceSpan(start, reader.getPosition()), text, CommentToken.Type.SINGLE_LINE);
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private boolean isIdentifierStart(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
	}

	private boolean isIdentifierPart(char c) {
		return isIdentifierStart(c) || isDigit(c);
	}

	private Optional<Token> getKeywordToken(int start, int end, String input) {
		Keyword keyword = switch (input) {
			case "abstract" -> Keyword.ABSTRACT;
			case "assert" -> Keyword.ASSERT;
			case "boolean" -> Keyword.BOOLEAN;
			case "break" -> Keyword.BREAK;
			case "byte" -> Keyword.BYTE;
			case "case" -> Keyword.CASE;
			case "catch" -> Keyword.CATCH;
			case "char" -> Keyword.CHAR;
			case "class" -> Keyword.CLASS;
			case "const" -> Keyword.CONST;
			case "continue" -> Keyword.CONTINUE;
			case "default" -> Keyword.DEFAULT;
			case "double" -> Keyword.DOUBLE;
			case "do" -> Keyword.DO;
			case "else" -> Keyword.ELSE;
			case "enum" -> Keyword.ENUM;
			case "extends" -> Keyword.EXTENDS;
			case "false" -> Keyword.FALSE;
			case "finally" -> Keyword.FINALLY;
			case "final" -> Keyword.FINAL;
			case "float" -> Keyword.FLOAT;
			case "for" -> Keyword.FOR;
			case "goto" -> Keyword.GOTO;
			case "if" -> Keyword.IF;
			case "implements" -> Keyword.IMPLEMENTS;
			case "import" -> Keyword.IMPORT;
			case "instanceof" -> Keyword.INSTANCEOF;
			case "interface" -> Keyword.INTERFACE;
			case "int" -> Keyword.INT;
			case "long" -> Keyword.LONG;
			case "native" -> Keyword.NATIVE;
			case "new" -> Keyword.NEW;
			case "null" -> Keyword.NULL;
			case "package" -> Keyword.PACKAGE;
			case "private" -> Keyword.PRIVATE;
			case "protected" -> Keyword.PROTECTED;
			case "public" -> Keyword.PUBLIC;
			case "return" -> Keyword.RETURN;
			case "short" -> Keyword.SHORT;
			case "static" -> Keyword.STATIC;
			case "strictfp" -> Keyword.STRICTFP;
			case "super" -> Keyword.SUPER;
			case "switch" -> Keyword.SWITCH;
			case "synchronized" -> Keyword.SYNCHRONIZED;
			case "this" -> Keyword.THIS;
			case "throws" -> Keyword.THROWS;
			case "throw" -> Keyword.THROW;
			case "transient" -> Keyword.TRANSIENT;
			case "true" -> Keyword.TRUE;
			case "try" -> Keyword.TRY;
			case "void" -> Keyword.VOID;
			case "volatile" -> Keyword.VOLATILE;
			case "while" -> Keyword.WHILE;
			default -> null;
		};

		if (keyword == null) {
			return Optional.empty();
		}

		return Optional.of(new KeywordToken(new SourceSpan(start, end), keyword));
	}
}
