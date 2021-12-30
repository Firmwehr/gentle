package com.github.firmwehr.gentle.backend.ir;

public record IkeaImmediate(
	String immediate
) implements IkeaBøx {
	@Override
	public String assemblyName() {
		return immediate;
	}
}
