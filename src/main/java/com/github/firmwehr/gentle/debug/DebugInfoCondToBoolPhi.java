package com.github.firmwehr.gentle.debug;

public record DebugInfoCondToBoolPhi(HasDebugInformation source) implements HasDebugInformation {
	@Override
	public String toDebugString() {
		return "Cond->bool Phi: " + source.toDebugString();
	}
}
