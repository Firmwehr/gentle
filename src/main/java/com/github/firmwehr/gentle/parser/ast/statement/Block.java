package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ExprWithParens;
import com.github.firmwehr.gentle.parser.Util;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Block(List<BlockStatement> statements) implements Statement, BlockStatement {
	public Block then(BlockStatement statement) {
		return new Block(Util.copyAndAppend(statements, statement));
	}

	public Block thenLocalVar(Type type, String name) {
		return then(new LocalVariableDeclarationStatement(type, Ident.dummy(name), Optional.empty()));
	}

	public Block thenLocalVar(Type type, String name, Expression value) {
		return then(
			new LocalVariableDeclarationStatement(type, Ident.dummy(name), Optional.of(new ExprWithParens(value))));
	}

	public Block thenBlock(Block block) {
		return then(block);
	}

	public Block thenEmpty() {
		return then(Statement.newEmpty());
	}

	public Block thenExpr(Expression expression) {
		return then(Statement.newExpr(expression));
	}

	public Block thenIf(Expression condition, Statement body) {
		return then(Statement.newIf(condition, body));
	}

	public Block thenIf(Expression condition, Statement body, Statement elseBody) {
		return then(Statement.newIf(condition, body, elseBody));
	}

	public Block thenReturn() {
		return then(Statement.newReturn());
	}

	public Block thenReturn(Expression returnValue) {
		return then(Statement.newReturn(returnValue));
	}

	public Block thenWhile(Expression condition, Statement body) {
		return then(Statement.newWhile(condition, body));
	}

	@Override
	public BlockStatement asBlockStatement() {
		return this;
	}

	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		List<BlockStatement> statements =
			this.statements.stream().filter(s -> !(s instanceof EmptyStatement)).collect(Collectors.toList());

		if (statements.isEmpty()) {
			p.add("{ }");
		} else {
			p.add("{").indent().newline();
			p.addAll(statements, "", true);
			p.unindent().add("}");
		}
	}
}
