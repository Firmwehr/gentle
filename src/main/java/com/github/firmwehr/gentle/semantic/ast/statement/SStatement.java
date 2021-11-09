package com.github.firmwehr.gentle.semantic.ast.statement;

public sealed interface SStatement
	permits SBlock, SExpressionStatement, SIfStatement, SReturnStatement, SWhileStatement {
}
