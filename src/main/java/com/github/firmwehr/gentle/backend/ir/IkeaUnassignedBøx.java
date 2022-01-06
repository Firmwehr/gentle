package com.github.firmwehr.gentle.backend.ir;

public record IkeaUnassignedBøx(IkeaRegisterSize size) implements IkeaBøx {

	@Override
	public String assemblyName() {
		return "<Unassigned Register>";
	}
}
