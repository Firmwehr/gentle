package com.github.firmwehr.gentle.debug;

public record DebugInfoCondToBoolBlock(
	HasDebugInformation source,
	CondToBoolBlockType type
) implements HasDebugInformation {

	public enum CondToBoolBlockType {
		AFTER,
		TRUE,
		FALSE
	}

	@Override
	public String toDebugString() {
		return "Cond->Bool block type" + type + ": " + source.toDebugString();
	}
}
