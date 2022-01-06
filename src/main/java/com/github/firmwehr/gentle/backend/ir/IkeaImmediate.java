package com.github.firmwehr.gentle.backend.ir;

import firm.Mode;
import firm.TargetValue;

public record IkeaImmediate(
	TargetValue immediate,
	IkeaRegisterSize size
) implements IkeaBøx {

	public IkeaImmediate {
		if (immediate.getMode().equals(Mode.getP())) {
			immediate = immediate.convertTo(Mode.getLu());
		}
	}

	@Override
	public String assemblyName() {
		return immediate.toString();
	}
}
