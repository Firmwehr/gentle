package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoIfBlock(
	SIfStatement source,
	IfBlockType type
) implements HasDebugInformation {

	@Override
	public String additionalInfo() {
		return type.toString();
	}

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	public enum IfBlockType {
		AFTER,
		TRUE,
		FALSE
	}
}
