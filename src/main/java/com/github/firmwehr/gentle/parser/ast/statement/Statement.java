package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.List;
import java.util.Optional;

public sealed interface Statement extends PrettyPrint
	permits Block, EmptyStatement, IfStatement, ExpressionStatement, WhileStatement, ReturnStatement {

	static Block newBlock() {
		return new Block(List.of());
	}

	static EmptyStatement newEmpty() {
		return new EmptyStatement();
	}

	static ExpressionStatement newExpr(Expression expression) {
		return new ExpressionStatement(expression);
	}

	static IfStatement newIf(Expression condition, Statement body) {
		return new IfStatement(condition, body, Optional.empty());
	}

	static IfStatement newIf(Expression condition, Statement body, Statement elseBody) {
		return new IfStatement(condition, body, Optional.of(elseBody));
	}

	static ReturnStatement newReturn() {
		return new ReturnStatement(Optional.empty(), SourceSpan.dummy());
	}

	static ReturnStatement newReturn(Expression returnValue) {
		return new ReturnStatement(Optional.of(returnValue), SourceSpan.dummy());
	}

	static WhileStatement newWhile(Expression condition, Statement body) {
		return new WhileStatement(condition, body);
	}

	BlockStatement asBlockStatement();
}
