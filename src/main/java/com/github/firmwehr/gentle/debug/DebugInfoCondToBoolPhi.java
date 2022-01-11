package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoCondToBoolPhi(HasDebugInformation source) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	@Override
	public String additionalInfo() {
		return "Cond -> Bool";
	}
}
