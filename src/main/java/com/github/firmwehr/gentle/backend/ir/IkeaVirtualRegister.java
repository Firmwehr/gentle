package com.github.firmwehr.gentle.backend.ir;

public record IkeaVirtualRegister(int num) implements IkeaBøx {

	@Override
	public String assemblyName() {
		return String.valueOf(num);
	}
}
