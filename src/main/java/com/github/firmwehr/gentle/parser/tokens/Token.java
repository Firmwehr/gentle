package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.lexer.tokens.TokenComment;
import com.github.firmwehr.gentle.lexer.tokens.TokenEndOfFile;
import com.github.firmwehr.gentle.lexer.tokens.TokenIdentifier;
import com.github.firmwehr.gentle.lexer.tokens.TokenIntegerLiteral;
import com.github.firmwehr.gentle.lexer.tokens.TokenKeyword;
import com.github.firmwehr.gentle.lexer.tokens.TokenWhitespace;
import com.github.firmwehr.gentle.source.HasSourceSpan;

import java.util.Optional;

public sealed interface Token extends HasSourceSpan
	permits WhitespaceToken, CommentToken, KeywordToken, OperatorToken, IdentToken, IntegerLiteralToken, EofToken {

	String format();

	static Token fromLexerToken(com.github.firmwehr.gentle.lexer.tokens.Token token) {
		return switch (token) {
			case TokenComment comment -> new CommentToken(comment.sourceSpan(), comment.text());
			case TokenEndOfFile eof -> new EofToken(eof.sourceSpan());
			case TokenIdentifier ident -> new IdentToken(ident.sourceSpan(), ident.id());
			case TokenIntegerLiteral lit -> new IntegerLiteralToken(lit.sourceSpan(), lit.number());
			case TokenWhitespace whitespace -> new WhitespaceToken(whitespace.sourceSpan());
			case TokenKeyword keyword -> switch (keyword.tokenType()) {
				// Keywords
				case ABSTRACT -> new KeywordToken(keyword.sourceSpan(), Keyword.ABSTRACT);
				case ASSERT -> new KeywordToken(keyword.sourceSpan(), Keyword.ASSERT);
				case BOOLEAN -> new KeywordToken(keyword.sourceSpan(), Keyword.BOOLEAN);
				case BREAK -> new KeywordToken(keyword.sourceSpan(), Keyword.BREAK);
				case BYTE -> new KeywordToken(keyword.sourceSpan(), Keyword.BYTE);
				case CASE -> new KeywordToken(keyword.sourceSpan(), Keyword.CASE);
				case CATCH -> new KeywordToken(keyword.sourceSpan(), Keyword.CATCH);
				case CHAR -> new KeywordToken(keyword.sourceSpan(), Keyword.CHAR);
				case CLASS -> new KeywordToken(keyword.sourceSpan(), Keyword.CLASS);
				case CONST -> new KeywordToken(keyword.sourceSpan(), Keyword.CONST);
				case CONTINUE -> new KeywordToken(keyword.sourceSpan(), Keyword.CONTINUE);
				case DEFAULT -> new KeywordToken(keyword.sourceSpan(), Keyword.DEFAULT);
				case DOUBLE -> new KeywordToken(keyword.sourceSpan(), Keyword.DOUBLE);
				case DO -> new KeywordToken(keyword.sourceSpan(), Keyword.DO);
				case ELSE -> new KeywordToken(keyword.sourceSpan(), Keyword.ELSE);
				case ENUM -> new KeywordToken(keyword.sourceSpan(), Keyword.ENUM);
				case EXTENDS -> new KeywordToken(keyword.sourceSpan(), Keyword.EXTENDS);
				case FALSE -> new KeywordToken(keyword.sourceSpan(), Keyword.FALSE);
				case FINALLY -> new KeywordToken(keyword.sourceSpan(), Keyword.FINALLY);
				case FINAL -> new KeywordToken(keyword.sourceSpan(), Keyword.FINAL);
				case FLOAT -> new KeywordToken(keyword.sourceSpan(), Keyword.FLOAT);
				case FOR -> new KeywordToken(keyword.sourceSpan(), Keyword.FOR);
				case GOTO -> new KeywordToken(keyword.sourceSpan(), Keyword.GOTO);
				case IF -> new KeywordToken(keyword.sourceSpan(), Keyword.IF);
				case IMPLEMENTS -> new KeywordToken(keyword.sourceSpan(), Keyword.IMPLEMENTS);
				case IMPORT -> new KeywordToken(keyword.sourceSpan(), Keyword.IMPORT);
				case INSTANCEOF -> new KeywordToken(keyword.sourceSpan(), Keyword.INSTANCEOF);
				case INTERFACE -> new KeywordToken(keyword.sourceSpan(), Keyword.INTERFACE);
				case INT -> new KeywordToken(keyword.sourceSpan(), Keyword.INT);
				case LONG -> new KeywordToken(keyword.sourceSpan(), Keyword.LONG);
				case NATIVE -> new KeywordToken(keyword.sourceSpan(), Keyword.NATIVE);
				case NEW -> new KeywordToken(keyword.sourceSpan(), Keyword.NEW);
				case NULL -> new KeywordToken(keyword.sourceSpan(), Keyword.NULL);
				case PACKAGE -> new KeywordToken(keyword.sourceSpan(), Keyword.PACKAGE);
				case PRIVATE -> new KeywordToken(keyword.sourceSpan(), Keyword.PRIVATE);
				case PROTECTED -> new KeywordToken(keyword.sourceSpan(), Keyword.PROTECTED);
				case PUBLIC -> new KeywordToken(keyword.sourceSpan(), Keyword.PUBLIC);
				case RETURN -> new KeywordToken(keyword.sourceSpan(), Keyword.RETURN);
				case SHORT -> new KeywordToken(keyword.sourceSpan(), Keyword.SHORT);
				case STATIC -> new KeywordToken(keyword.sourceSpan(), Keyword.STATIC);
				case STRICTFP -> new KeywordToken(keyword.sourceSpan(), Keyword.STRICTFP);
				case SUPER -> new KeywordToken(keyword.sourceSpan(), Keyword.SUPER);
				case SWITCH -> new KeywordToken(keyword.sourceSpan(), Keyword.SWITCH);
				case SYNCHRONIZED -> new KeywordToken(keyword.sourceSpan(), Keyword.SYNCHRONIZED);
				case THIS -> new KeywordToken(keyword.sourceSpan(), Keyword.THIS);
				case THROWS -> new KeywordToken(keyword.sourceSpan(), Keyword.THROWS);
				case THROW -> new KeywordToken(keyword.sourceSpan(), Keyword.THROW);
				case TRANSIENT -> new KeywordToken(keyword.sourceSpan(), Keyword.TRANSIENT);
				case TRUE -> new KeywordToken(keyword.sourceSpan(), Keyword.TRUE);
				case TRY -> new KeywordToken(keyword.sourceSpan(), Keyword.TRY);
				case VOID -> new KeywordToken(keyword.sourceSpan(), Keyword.VOID);
				case VOLATILE -> new KeywordToken(keyword.sourceSpan(), Keyword.VOLATILE);
				case WHILE -> new KeywordToken(keyword.sourceSpan(), Keyword.WHILE);
				// Operators
				case NOT_EQUALS -> new OperatorToken(keyword.sourceSpan(), Operator.NOT_EQUAL);
				case LOGICAL_NOT -> new OperatorToken(keyword.sourceSpan(), Operator.LOGICAL_NOT);
				case LEFT_PAREN -> new OperatorToken(keyword.sourceSpan(), Operator.LEFT_PAREN);
				case RIGHT_PAREN -> new OperatorToken(keyword.sourceSpan(), Operator.RIGHT_PAREN);
				case ASSIGN_MULTIPLY -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_MULTIPLY);
				case MULTIPLY -> new OperatorToken(keyword.sourceSpan(), Operator.MULTIPLY);
				case POSTFIX_INCREMENT -> new OperatorToken(keyword.sourceSpan(), Operator.INCREMENT);
				case ASSIGN_ADD -> new OperatorToken(keyword.sourceSpan(), Operator.ADD_AND_ASSIGN);
				case ADD -> new OperatorToken(keyword.sourceSpan(), Operator.PLUS);
				case COMMA -> new OperatorToken(keyword.sourceSpan(), Operator.COMMA);
				case ASSIGN_SUBTRACT -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_SUBTRACT);
				case POSTFIX_DECREMENT -> new OperatorToken(keyword.sourceSpan(), Operator.DECREMENT);
				case SUBTRACT -> new OperatorToken(keyword.sourceSpan(), Operator.MINUS);
				case DOT -> new OperatorToken(keyword.sourceSpan(), Operator.DOT);
				case ASSIGN_DIVIDE -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_DIVIDE);
				case DIVIDE -> new OperatorToken(keyword.sourceSpan(), Operator.DIVIDE);
				case COLON -> new OperatorToken(keyword.sourceSpan(), Operator.COLON);
				case SEMICOLON -> new OperatorToken(keyword.sourceSpan(), Operator.SEMICOLON);
				case ASSIGN_BITSHIFT_LEFT -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_LEFT_SHIFT);
				case BITSHIFT_LEFT -> new OperatorToken(keyword.sourceSpan(), Operator.LEFT_SHIFT);
				case LESS_THAN_EQUALS -> new OperatorToken(keyword.sourceSpan(), Operator.LESS_OR_EQUAL);
				case LESS_THAN -> new OperatorToken(keyword.sourceSpan(), Operator.LESS_THAN);
				case EQUALS -> new OperatorToken(keyword.sourceSpan(), Operator.EQUAL);
				case ASSIGN -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN);
				case GREATER_THAN_EQUALS -> new OperatorToken(keyword.sourceSpan(), Operator.GREATER_OR_EQUAL);
				case ASSIGN_BITSHIFT_RIGHT -> new OperatorToken(keyword.sourceSpan(),
					Operator.ASSIGN_SIGNED_RIGHT_SHIFT);
				case ASSIGN_BITSHIFT_RIGHT_ZEROFILL -> new OperatorToken(keyword.sourceSpan(),
					Operator.ASSIGN_UNSIGNED_RIGHT_SHIFT);
				case BITSHIFT_RIGHT_ZEROFILL -> new OperatorToken(keyword.sourceSpan(), Operator.UNSIGNED_RIGHT_SHIFT);
				case BITSHIFT_RIGHT -> new OperatorToken(keyword.sourceSpan(), Operator.SIGNED_RIGHT_SHIFT);
				case GREATER_THAN -> new OperatorToken(keyword.sourceSpan(), Operator.GREATER_THAN);
				case QUESTION_MARK -> new OperatorToken(keyword.sourceSpan(), Operator.QUESTION_MARK);
				case ASSIGN_MODULO -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_MODULO);
				case MODULO -> new OperatorToken(keyword.sourceSpan(), Operator.MODULO);
				case ASSIGN_BITWISE_AND -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_BITWISE_AND);
				case LOGICAL_AND -> new OperatorToken(keyword.sourceSpan(), Operator.LOGICAL_AND);
				case BITWISE_AND -> new OperatorToken(keyword.sourceSpan(), Operator.BITWISE_AND);
				case LEFT_BRACKET -> new OperatorToken(keyword.sourceSpan(), Operator.LEFT_BRACKET);
				case RIGHT_BRACKET -> new OperatorToken(keyword.sourceSpan(), Operator.RIGHT_BRACKET);
				case ASSIGN_BITWISE_XOR -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_BITWISE_XOR);
				case BITWISE_XOR -> new OperatorToken(keyword.sourceSpan(), Operator.BITWISE_XOR);
				case LEFT_BRACE -> new OperatorToken(keyword.sourceSpan(), Operator.LEFT_BRACE);
				case RIGHT_BRACE -> new OperatorToken(keyword.sourceSpan(), Operator.RIGHT_BRACE);
				case BITWISE_NOT -> new OperatorToken(keyword.sourceSpan(), Operator.BITWISE_NOT);
				case ASSIGN_BITWISE_OR -> new OperatorToken(keyword.sourceSpan(), Operator.ASSIGN_BITWISE_OR);
				case LOGICAL_OR -> new OperatorToken(keyword.sourceSpan(), Operator.LOGICAL_OR);
				case BITWISE_OR -> new OperatorToken(keyword.sourceSpan(), Operator.BITWISE_OR);
				// Other stuff
				default -> throw new IllegalArgumentException("Invalid keyword token type " + keyword.tokenType());
			};
		};
	}

	default boolean isEof() {
		return this instanceof EofToken;
	}

	default boolean isKeyword(Keyword keyword) {
		return this instanceof KeywordToken t && t.keyword() == keyword;
	}

	default boolean isOperator(Operator operator) {
		return this instanceof OperatorToken t && t.operator() == operator;
	}

	default Optional<IdentToken> asIdentToken() {
		if (this instanceof IdentToken ident) {
			return Optional.of(ident);
		} else {
			return Optional.empty();
		}
	}

	default boolean isIdent() {
		return asIdentToken().isPresent();
	}

	default Optional<IntegerLiteralToken> asIntegerLiteralToken() {
		if (this instanceof IntegerLiteralToken integerLiteral) {
			return Optional.of(integerLiteral);
		} else {
			return Optional.empty();
		}
	}

	default boolean isIntegerLiteral() {
		return asIntegerLiteralToken().isPresent();
	}
}
