package com.github.firmwehr.gentle.backend.ir;

public record IkeaVirtualRegister(
	int num,
	IkeaRegisterSize size
) implements IkeaBøx {

	@Override
	public String assemblyName() {
		return String.valueOf(num);
	}
}
