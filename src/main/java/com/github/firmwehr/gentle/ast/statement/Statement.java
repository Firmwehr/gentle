package com.github.firmwehr.gentle.ast.statement;

import com.github.firmwehr.gentle.ast.HasSourcePosition;

// FIXME: Add remaining statements
public sealed interface Statement<I> extends HasSourcePosition
		permits
		BlockStatement {
}
