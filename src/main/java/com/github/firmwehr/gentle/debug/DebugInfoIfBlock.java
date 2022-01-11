package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;

public record DebugInfoIfBlock(
	SIfStatement source,
	IfBlockType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "if " + type + ": " + source.toDebugString();
	}

	public enum IfBlockType {
		AFTER,
		TRUE,
		FALSE
	}
}
