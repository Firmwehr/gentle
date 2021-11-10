package com.github.firmwehr.gentle.semantic.ast.statement;

import java.util.List;

public record SBlock(List<SStatement> statements) implements SStatement {
	public SBlock() {
		this(List.of());
	}
}
