package com.github.firmwehr.gentle.backend.lego;

public record LegoVirtualRegister(
	int num,
	LegoRegisterSize size
) implements LegoBøx {

	@Override
	public String assemblyName() {
		return String.valueOf(num);
	}
}
