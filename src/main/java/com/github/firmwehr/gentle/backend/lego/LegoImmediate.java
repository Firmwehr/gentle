package com.github.firmwehr.gentle.backend.lego;

import firm.Mode;
import firm.TargetValue;

public record LegoImmediate(
	TargetValue immediate,
	LegoRegisterSize size
) implements LegoBøx {

	@SuppressWarnings("AssignmentToMethodParameter")
	public LegoImmediate {
		if (immediate.getMode().equals(Mode.getP())) {
			immediate = immediate.convertTo(Mode.getLu());
		}
	}

	@Override
	public String assemblyName() {
		return immediate.toString();
	}
}
