package com.github.firmwehr.gentle.backend.ir.register;

import java.util.Optional;

public class RegisterInformation {
	private boolean spilled;
	private Optional<X86Register> register;

	public RegisterInformation() {
		this.spilled = false;
		this.register = Optional.empty();
	}

	public boolean isSpilled() {
		return spilled;
	}

	public void setSpilled(boolean spilled) {
		this.spilled = spilled;
	}

	public Optional<X86Register> getRegister() {
		return register;
	}

	public void setRegister(X86Register register) {
		this.register = Optional.of(register);
	}

	public boolean hasRegister() {
		return getRegister().isPresent();
	}
}
