package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record Block(List<BlockStatement> statements) implements Statement, BlockStatement {
	public Block then(BlockStatement statement) {
		List<BlockStatement> newStatements = Stream.concat(this.statements.stream(), Stream.of(statement)).toList();
		return new Block(newStatements);
	}

	public Block thenLocalVar(Type type, String name) {
		return then(new LocalVariableDeclarationStatement(type, new Ident(name), Optional.empty()));
	}

	public Block thenLocalVar(Type type, String name, Expression value) {
		return then(new LocalVariableDeclarationStatement(type, new Ident(name), Optional.of(value)));
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
	public void prettyPrint(PrettyPrinter p) {
		p.add("{").indent().addAll(statements, "", true).unindent().add("}");
	}
}
