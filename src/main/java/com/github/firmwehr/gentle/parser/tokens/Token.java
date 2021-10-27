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

	static Token fromLexerToken(com.github.firmwehr.gentle.lexer.tokens.Token token) {
		return switch (token) {
			case TokenComment comment -> {
				var type = switch (comment.type()) {
					case SINGLE_LINE -> CommentToken.CommentType.LINE;
					case STAR -> CommentToken.CommentType.BLOCK;
				};
				yield new CommentToken(comment.position().span(), type, comment.text());
			}
			case TokenEndOfFile eof -> new EofToken(eof.position().span());
			case TokenIdentifier ident -> new IdentToken(ident.position().span(), ident.id());
			case TokenIntegerLiteral lit -> new IntegerLiteralToken(lit.position().span(), lit.number());
			case TokenWhitespace whitespace -> new WhitespaceToken(whitespace.position().span());
			case TokenKeyword keyword -> switch (keyword.tokenType()) {
				// Keywords
				case ABSTRACT -> new KeywordToken(keyword.position().span(), Keyword.ABSTRACT);
				case ASSERT -> new KeywordToken(keyword.position().span(), Keyword.ASSERT);
				case BOOLEAN -> new KeywordToken(keyword.position().span(), Keyword.BOOLEAN);
				case BREAK -> new KeywordToken(keyword.position().span(), Keyword.BREAK);
				case BYTE -> new KeywordToken(keyword.position().span(), Keyword.BYTE);
				case CASE -> new KeywordToken(keyword.position().span(), Keyword.CASE);
				case CATCH -> new KeywordToken(keyword.position().span(), Keyword.CATCH);
				case CHAR -> new KeywordToken(keyword.position().span(), Keyword.CHAR);
				case CLASS -> new KeywordToken(keyword.position().span(), Keyword.CLASS);
				case CONST -> new KeywordToken(keyword.position().span(), Keyword.CONST);
				case CONTINUE -> new KeywordToken(keyword.position().span(), Keyword.CONTINUE);
				case DEFAULT -> new KeywordToken(keyword.position().span(), Keyword.DEFAULT);
				case DOUBLE -> new KeywordToken(keyword.position().span(), Keyword.DOUBLE);
				case DO -> new KeywordToken(keyword.position().span(), Keyword.DO);
				case ELSE -> new KeywordToken(keyword.position().span(), Keyword.ELSE);
				case ENUM -> new KeywordToken(keyword.position().span(), Keyword.ENUM);
				case EXTENDS -> new KeywordToken(keyword.position().span(), Keyword.EXTENDS);
				case FALSE -> new KeywordToken(keyword.position().span(), Keyword.FALSE);
				case FINALLY -> new KeywordToken(keyword.position().span(), Keyword.FINALLY);
				case FINAL -> new KeywordToken(keyword.position().span(), Keyword.FINAL);
				case FLOAT -> new KeywordToken(keyword.position().span(), Keyword.FLOAT);
				case FOR -> new KeywordToken(keyword.position().span(), Keyword.FOR);
				case GOTO -> new KeywordToken(keyword.position().span(), Keyword.GOTO);
				case IF -> new KeywordToken(keyword.position().span(), Keyword.IF);
				case IMPLEMENTS -> new KeywordToken(keyword.position().span(), Keyword.IMPLEMENTS);
				case IMPORT -> new KeywordToken(keyword.position().span(), Keyword.IMPORT);
				case INSTANCEOF -> new KeywordToken(keyword.position().span(), Keyword.INSTANCEOF);
				case INTERFACE -> new KeywordToken(keyword.position().span(), Keyword.INTERFACE);
				case INT -> new KeywordToken(keyword.position().span(), Keyword.INT);
				case LONG -> new KeywordToken(keyword.position().span(), Keyword.LONG);
				case NATIVE -> new KeywordToken(keyword.position().span(), Keyword.NATIVE);
				case NEW -> new KeywordToken(keyword.position().span(), Keyword.NEW);
				case NULL -> new KeywordToken(keyword.position().span(), Keyword.NULL);
				case PACKAGE -> new KeywordToken(keyword.position().span(), Keyword.PACKAGE);
				case PRIVATE -> new KeywordToken(keyword.position().span(), Keyword.PRIVATE);
				case PROTECTED -> new KeywordToken(keyword.position().span(), Keyword.PROTECTED);
				case PUBLIC -> new KeywordToken(keyword.position().span(), Keyword.PUBLIC);
				case RETURN -> new KeywordToken(keyword.position().span(), Keyword.RETURN);
				case SHORT -> new KeywordToken(keyword.position().span(), Keyword.SHORT);
				case STATIC -> new KeywordToken(keyword.position().span(), Keyword.STATIC);
				case STRICTFP -> new KeywordToken(keyword.position().span(), Keyword.STRICTFP);
				case SUPER -> new KeywordToken(keyword.position().span(), Keyword.SUPER);
				case SWITCH -> new KeywordToken(keyword.position().span(), Keyword.SWITCH);
				case SYNCHRONIZED -> new KeywordToken(keyword.position().span(), Keyword.SYNCHRONIZED);
				case THIS -> new KeywordToken(keyword.position().span(), Keyword.THIS);
				case THROWS -> new KeywordToken(keyword.position().span(), Keyword.THROWS);
				case THROW -> new KeywordToken(keyword.position().span(), Keyword.THROW);
				case TRANSIENT -> new KeywordToken(keyword.position().span(), Keyword.TRANSIENT);
				case TRUE -> new KeywordToken(keyword.position().span(), Keyword.TRUE);
				case TRY -> new KeywordToken(keyword.position().span(), Keyword.TRY);
				case VOID -> new KeywordToken(keyword.position().span(), Keyword.VOID);
				case VOLATILE -> new KeywordToken(keyword.position().span(), Keyword.VOLATILE);
				case WHILE -> new KeywordToken(keyword.position().span(), Keyword.WHILE);
				// Operators
				case NOT_EQUALS -> new OperatorToken(keyword.position().span(), Operator.NOT_EQUAL);
				case LOGICAL_NOT -> new OperatorToken(keyword.position().span(), Operator.LOGICAL_NOT);
				case LEFT_PAREN -> new OperatorToken(keyword.position().span(), Operator.LEFT_PAREN);
				case RIGHT_PAREN -> new OperatorToken(keyword.position().span(), Operator.RIGHT_PAREN);
				case ASSIGN_MULTIPLY -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_MULTIPLY);
				case MULTIPLY -> new OperatorToken(keyword.position().span(), Operator.MULTIPLY);
				case POSTFIX_INCREMENT -> new OperatorToken(keyword.position().span(), Operator.INCREMENT);
				case ASSIGN_ADD -> new OperatorToken(keyword.position().span(), Operator.ADD_AND_ASSIGN);
				case ADD -> new OperatorToken(keyword.position().span(), Operator.PLUS);
				case COMMA -> new OperatorToken(keyword.position().span(), Operator.COMMA);
				case ASSIGN_SUBTRACT -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_SUBTRACT);
				case POSTFIX_DECREMENT -> new OperatorToken(keyword.position().span(), Operator.DECREMENT);
				case SUBTRACT -> new OperatorToken(keyword.position().span(), Operator.MINUS);
				case DOT -> new OperatorToken(keyword.position().span(), Operator.DOT);
				case ASSIGN_DIVIDE -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_DIVIDE);
				case DIVIDE -> new OperatorToken(keyword.position().span(), Operator.DIVIDE);
				case COLON -> new OperatorToken(keyword.position().span(), Operator.COLON);
				case SEMICOLON -> new OperatorToken(keyword.position().span(), Operator.SEMICOLON);
				case ASSIGN_BITSHIFT_LEFT -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_LEFT_SHIFT);
				case BITSHIFT_LEFT -> new OperatorToken(keyword.position().span(), Operator.LEFT_SHIFT);
				case LESS_THAN_EQUALS -> new OperatorToken(keyword.position().span(), Operator.LESS_OR_EQUAL);
				case LESS_THAN -> new OperatorToken(keyword.position().span(), Operator.LESS_THAN);
				case EQUALS -> new OperatorToken(keyword.position().span(), Operator.EQUAL);
				case ASSIGN -> new OperatorToken(keyword.position().span(), Operator.ASSIGN);
				case GREATER_THAN_EQUALS -> new OperatorToken(keyword.position().span(), Operator.GREATER_OR_EQUAL);
				case ASSIGN_BITSHIFT_RIGHT -> new OperatorToken(keyword.position().span(),
					Operator.ASSIGN_SIGNED_RIGHT_SHIFT);
				case ASSIGN_BITSHIFT_RIGHT_ZEROFILL -> new OperatorToken(keyword.position().span(),
					Operator.ASSIGN_UNSIGNED_RIGHT_SHIFT);
				case BITSHIFT_RIGHT_ZEROFILL -> new OperatorToken(keyword.position().span(),
					Operator.UNSIGNED_RIGHT_SHIFT);
				case BITSHIFT_RIGHT -> new OperatorToken(keyword.position().span(), Operator.SIGNED_RIGHT_SHIFT);
				case GREATER_THAN -> new OperatorToken(keyword.position().span(), Operator.GREATER_THAN);
				case QUESTION_MARK -> new OperatorToken(keyword.position().span(), Operator.QUESTION_MARK);
				case ASSIGN_MODULO -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_MODULO);
				case MODULO -> new OperatorToken(keyword.position().span(), Operator.MODULO);
				case ASSIGN_BITWISE_AND -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_BITWISE_AND);
				case LOGICAL_AND -> new OperatorToken(keyword.position().span(), Operator.LOGICAL_AND);
				case BITWISE_AND -> new OperatorToken(keyword.position().span(), Operator.BITWISE_AND);
				case LEFT_BRACKET -> new OperatorToken(keyword.position().span(), Operator.LEFT_BRACKET);
				case RIGHT_BRACKET -> new OperatorToken(keyword.position().span(), Operator.RIGHT_BRACKET);
				case ASSIGN_BITWISE_XOR -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_BITWISE_XOR);
				case BITWISE_XOR -> new OperatorToken(keyword.position().span(), Operator.BITWISE_XOR);
				case LEFT_BRACE -> new OperatorToken(keyword.position().span(), Operator.LEFT_BRACE);
				case RIGHT_BRACE -> new OperatorToken(keyword.position().span(), Operator.RIGHT_BRACE);
				case BITWISE_NOT -> new OperatorToken(keyword.position().span(), Operator.BITWISE_NOT);
				case ASSIGN_BITWISE_OR -> new OperatorToken(keyword.position().span(), Operator.ASSIGN_BITWISE_OR);
				case LOGICAL_OR -> new OperatorToken(keyword.position().span(), Operator.LOGICAL_OR);
				case BITWISE_OR -> new OperatorToken(keyword.position().span(), Operator.BITWISE_OR);
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
}
