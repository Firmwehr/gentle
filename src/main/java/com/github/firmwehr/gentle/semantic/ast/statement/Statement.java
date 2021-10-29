package com.github.firmwehr.gentle.semantic.ast.statement;

public sealed interface Statement permits Block, ExpressionStatement, IfStatement, ReturnStatement, WhileStatement {
}
