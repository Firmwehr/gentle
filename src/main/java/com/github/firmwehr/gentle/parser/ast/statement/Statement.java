package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface Statement extends PrettyPrint
	permits Block, EmptyStatement, IfStatement, ExpressionStatement, WhileStatement, ReturnStatement {
}
