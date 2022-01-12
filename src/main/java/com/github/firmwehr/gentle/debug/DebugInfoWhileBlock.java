package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoWhileBlock(
	SWhileStatement source,
	WhileBlockType type
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source.debugSpan();
	}

	@Override
	public String additionalInfo() {
		return type.name();
	}

	public enum WhileBlockType {
		HEADER,
		BODY,
		AFTER
	}
}
