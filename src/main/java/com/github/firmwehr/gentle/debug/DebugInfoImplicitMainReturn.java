package com.github.firmwehr.gentle.debug;

public class DebugInfoImplicitMainReturn implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "implicit main return with 0";
	}
}
