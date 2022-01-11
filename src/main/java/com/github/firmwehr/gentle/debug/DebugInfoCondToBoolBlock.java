package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoCondToBoolBlock(
	HasDebugInformation source,
	CondToBoolBlockType type
) implements HasDebugInformation {

	@Override
	public String additionalInfo() {
		return "Cond -> Bool";
	}

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	public enum CondToBoolBlockType {
		AFTER,
		TRUE,
		FALSE

	}
}
