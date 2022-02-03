package com.github.firmwehr.gentle.backend.lego;

public record LegoUnassignedBøx(LegoRegisterSize size) implements LegoBøx {

	@Override
	public String assemblyName() {
		return "<Unassigned Register>";
	}
}
