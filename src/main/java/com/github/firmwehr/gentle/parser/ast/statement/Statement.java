package com.github.firmwehr.gentle.parser.ast.statement;

public sealed interface Statement
	permits Block, EmptyStatement, IfStatement, ExpressionStatement, WhileStatement, ReturnStatement {
}
