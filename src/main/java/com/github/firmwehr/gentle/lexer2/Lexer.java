package com.github.firmwehr.gentle.lexer2;

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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public class Lexer {

	private final SortedMap<Integer, List<Operator>> operatorsByLength;

	private final StringReader reader;

	public Lexer(Source source) {
		this.reader = new StringReader(source.getContent());

		this.operatorsByLength = new TreeMap<>(Comparator.reverseOrder());
		for (Operator value : Operator.values()) {
			operatorsByLength.computeIfAbsent(value.getName().length(), (ignored) -> new ArrayList<>()).add(value);
		}
	}

	public List<Token> lex() throws LexException {
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
				reader.readWhitespace();
				tokens.add(new WhitespaceToken(null));
				continue;
			}
			if (reader.canRead(2) && reader.peek(2).equals("/*")) {
				tokens.add(readComment());
				continue;
			}
			tokens.add(readOperator("Expected an operator, integer, identifier or comment"));
		}
		tokens.add(new EofToken(null));

		return tokens;
	}

	private Token readNumber() {
		if (reader.peek() == '0') {
			reader.readChar();
			return new IntegerLiteralToken(null, BigInteger.ZERO);
		}
		return new IntegerLiteralToken(null, new BigInteger(reader.readWhile(this::isDigit)));
	}

	private Token readIdentOrKeyword() throws LexException {
		if (!isIdentifierPart(reader.peek())) {
			throw new LexException("Expected an identifier start", reader);
		}
		String read = reader.readChar() + reader.readWhile(this::isIdentifierPart);

		Optional<Token> keywordToken = getKeywordToken(read);
		return keywordToken.orElseGet(() -> new IdentToken(null, read.intern()));
	}

	private Token readOperator(String errorMessage) throws LexException {
		return switch (reader.readChar()) {
			case '!' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.NOT_EQUAL);
				}
				yield new OperatorToken(null, Operator.LOGICAL_NOT);
			}
			case '%' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_MODULO);
				}
				yield new OperatorToken(null, Operator.MODULO);
			}
			case '&' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_BITWISE_AND);
				}
				if (reader.peek() == '&') {
					reader.readChar();
					yield new OperatorToken(null, Operator.LOGICAL_AND);
				}
				yield new OperatorToken(null, Operator.BITWISE_AND);
			}
			case '*' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_MULTIPLY);
				}
				yield new OperatorToken(null, Operator.MULTIPLY);
			}
			case '+' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ADD_AND_ASSIGN);
				}
				if (reader.peek() == '+') {
					reader.readChar();
					yield new OperatorToken(null, Operator.INCREMENT);
				}
				yield new OperatorToken(null, Operator.PLUS);
			}
			case '-' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_SUBTRACT);
				}
				if (reader.peek() == '-') {
					reader.readChar();
					yield new OperatorToken(null, Operator.DECREMENT);
				}
				yield new OperatorToken(null, Operator.MINUS);
			}
			case '/' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_DIVIDE);
				}
				yield new OperatorToken(null, Operator.DIVIDE);
			}
			case '<' -> {
				if (reader.peek() == '<') {
					reader.readChar();
					if (reader.peek() == '=') {
						reader.readChar();
						yield new OperatorToken(null, Operator.ASSIGN_LEFT_SHIFT);
					}
					yield new OperatorToken(null, Operator.LEFT_SHIFT);
				}
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.LESS_OR_EQUAL);
				}
				yield new OperatorToken(null, Operator.LESS_THAN);
			}
			case '=' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.EQUAL);
				}
				yield new OperatorToken(null, Operator.ASSIGN);
			}
			case '>' -> {
				if (reader.peek() == '>') {
					reader.readChar();
					if (reader.peek() == '>') {
						reader.readChar();
						if (reader.peek() == '=') {
							reader.readChar();
							yield new OperatorToken(null, Operator.ASSIGN_UNSIGNED_RIGHT_SHIFT);
						}
						yield new OperatorToken(null, Operator.UNSIGNED_RIGHT_SHIFT);
					}
					if (reader.peek() == '=') {
						reader.readChar();
						yield new OperatorToken(null, Operator.ASSIGN_SIGNED_RIGHT_SHIFT);
					}
					yield new OperatorToken(null, Operator.SIGNED_RIGHT_SHIFT);
				}
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.GREATER_OR_EQUAL);
				}
				yield new OperatorToken(null, Operator.GREATER_THAN);
			}
			case '^' -> {
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_BITWISE_XOR);
				}
				yield new OperatorToken(null, Operator.BITWISE_XOR);
			}
			case '|' -> {
				if (reader.peek() == '|') {
					reader.readChar();
					yield new OperatorToken(null, Operator.LOGICAL_OR);
				}
				if (reader.peek() == '=') {
					reader.readChar();
					yield new OperatorToken(null, Operator.ASSIGN_BITWISE_OR);
				}
				yield new OperatorToken(null, Operator.BITWISE_OR);
			}
			case '~' -> new OperatorToken(null, Operator.BITWISE_NOT);
			case '{' -> new OperatorToken(null, Operator.LEFT_BRACE);
			case '}' -> new OperatorToken(null, Operator.RIGHT_BRACE);
			case '[' -> new OperatorToken(null, Operator.LEFT_BRACKET);
			case ']' -> new OperatorToken(null, Operator.RIGHT_BRACKET);
			case '?' -> new OperatorToken(null, Operator.QUESTION_MARK);
			case ':' -> new OperatorToken(null, Operator.COLON);
			case ';' -> new OperatorToken(null, Operator.SEMICOLON);
			case '.' -> new OperatorToken(null, Operator.DOT);
			case ',' -> new OperatorToken(null, Operator.COMMA);
			case '(' -> new OperatorToken(null, Operator.LEFT_PAREN);
			case ')' -> new OperatorToken(null, Operator.RIGHT_PAREN);
			default -> {
				throw new LexException(errorMessage, reader);
			}
		};
	}

	private Token readComment() throws LexException {
		reader.assertRead("/*");
		String text = reader.assertReadUntil("*/");
		text = text.substring(0, text.length() - 2);
		return new CommentToken(null, text);
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

	private Optional<Token> getKeywordToken(String input) {
		return switch (input) {
			case "abstract" -> Optional.of(new KeywordToken(null, Keyword.ABSTRACT));
			case "assert" -> Optional.of(new KeywordToken(null, Keyword.ASSERT));
			case "boolean" -> Optional.of(new KeywordToken(null, Keyword.BOOLEAN));
			case "break" -> Optional.of(new KeywordToken(null, Keyword.BREAK));
			case "byte" -> Optional.of(new KeywordToken(null, Keyword.BYTE));
			case "case" -> Optional.of(new KeywordToken(null, Keyword.CASE));
			case "catch" -> Optional.of(new KeywordToken(null, Keyword.CATCH));
			case "char" -> Optional.of(new KeywordToken(null, Keyword.CHAR));
			case "class" -> Optional.of(new KeywordToken(null, Keyword.CLASS));
			case "const" -> Optional.of(new KeywordToken(null, Keyword.CONST));
			case "continue" -> Optional.of(new KeywordToken(null, Keyword.CONTINUE));
			case "default" -> Optional.of(new KeywordToken(null, Keyword.DEFAULT));
			case "double" -> Optional.of(new KeywordToken(null, Keyword.DOUBLE));
			case "do" -> Optional.of(new KeywordToken(null, Keyword.DO));
			case "else" -> Optional.of(new KeywordToken(null, Keyword.ELSE));
			case "enum" -> Optional.of(new KeywordToken(null, Keyword.ENUM));
			case "extends" -> Optional.of(new KeywordToken(null, Keyword.EXTENDS));
			case "false" -> Optional.of(new KeywordToken(null, Keyword.FALSE));
			case "finally" -> Optional.of(new KeywordToken(null, Keyword.FINALLY));
			case "final" -> Optional.of(new KeywordToken(null, Keyword.FINAL));
			case "float" -> Optional.of(new KeywordToken(null, Keyword.FLOAT));
			case "for" -> Optional.of(new KeywordToken(null, Keyword.FOR));
			case "goto" -> Optional.of(new KeywordToken(null, Keyword.GOTO));
			case "if" -> Optional.of(new KeywordToken(null, Keyword.IF));
			case "implements" -> Optional.of(new KeywordToken(null, Keyword.IMPLEMENTS));
			case "import" -> Optional.of(new KeywordToken(null, Keyword.IMPORT));
			case "instanceof" -> Optional.of(new KeywordToken(null, Keyword.INSTANCEOF));
			case "interface" -> Optional.of(new KeywordToken(null, Keyword.INTERFACE));
			case "int" -> Optional.of(new KeywordToken(null, Keyword.INT));
			case "long" -> Optional.of(new KeywordToken(null, Keyword.LONG));
			case "native" -> Optional.of(new KeywordToken(null, Keyword.NATIVE));
			case "new" -> Optional.of(new KeywordToken(null, Keyword.NEW));
			case "null" -> Optional.of(new KeywordToken(null, Keyword.NULL));
			case "package" -> Optional.of(new KeywordToken(null, Keyword.PACKAGE));
			case "private" -> Optional.of(new KeywordToken(null, Keyword.PRIVATE));
			case "protected" -> Optional.of(new KeywordToken(null, Keyword.PROTECTED));
			case "public" -> Optional.of(new KeywordToken(null, Keyword.PUBLIC));
			case "return" -> Optional.of(new KeywordToken(null, Keyword.RETURN));
			case "short" -> Optional.of(new KeywordToken(null, Keyword.SHORT));
			case "static" -> Optional.of(new KeywordToken(null, Keyword.STATIC));
			case "strictfp" -> Optional.of(new KeywordToken(null, Keyword.STRICTFP));
			case "super" -> Optional.of(new KeywordToken(null, Keyword.SUPER));
			case "switch" -> Optional.of(new KeywordToken(null, Keyword.SWITCH));
			case "synchronized" -> Optional.of(new KeywordToken(null, Keyword.SYNCHRONIZED));
			case "this" -> Optional.of(new KeywordToken(null, Keyword.THIS));
			case "throws" -> Optional.of(new KeywordToken(null, Keyword.THROWS));
			case "throw" -> Optional.of(new KeywordToken(null, Keyword.THROW));
			case "transient" -> Optional.of(new KeywordToken(null, Keyword.TRANSIENT));
			case "true" -> Optional.of(new KeywordToken(null, Keyword.TRUE));
			case "try" -> Optional.of(new KeywordToken(null, Keyword.TRY));
			case "void" -> Optional.of(new KeywordToken(null, Keyword.VOID));
			case "volatile" -> Optional.of(new KeywordToken(null, Keyword.VOLATILE));
			case "while" -> Optional.of(new KeywordToken(null, Keyword.WHILE));
			default -> Optional.empty();
		};
	}
}
