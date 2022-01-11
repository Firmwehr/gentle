package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;

public record DebugInfoWhileBlock(
	SWhileStatement source,
	WhileBlockType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "while " + type + ": " + source.toDebugString();
	}

	public enum WhileBlockType {
		HEADER,
		BODY,
		AFTER
	}
}
