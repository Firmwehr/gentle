package com.github.firmwehr.gentle.backend.ir;

import firm.TargetValue;

public record IkeaImmediate(
	TargetValue immediate,
	IkeaRegisterSize size
) implements IkeaBøx {
	@Override
	public String assemblyName() {
		return immediate.toString();
	}
}
