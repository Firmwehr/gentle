package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public class DebugInfoImplicitMainReturn implements HasDebugInformation {

	@Override
	public String additionalInfo() {
		return "implicit main return with 0";
	}

	@Override
	public Optional<SourceSpan> debugSpan() {
		return Optional.empty();
	}
}
