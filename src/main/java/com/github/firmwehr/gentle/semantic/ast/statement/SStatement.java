package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;

public sealed interface SStatement
	permits SBlock, SExpressionStatement, SIfStatement, SReturnStatement, SWhileStatement {

	<T> T accept(Visitor<T> visitor) throws SemanticException;
}
