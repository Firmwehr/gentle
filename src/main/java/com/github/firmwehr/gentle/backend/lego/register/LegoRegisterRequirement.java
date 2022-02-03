package com.github.firmwehr.gentle.backend.lego.register;

import java.util.Set;

public record LegoRegisterRequirement(
	Set<X86Register> limitedTo,
	Set<Integer> shouldBeSame,
	Set<Integer> mustBeDifferent
) {

	public boolean limited() {
		return limitedTo.size() < X86Register.values().length;
	}

	public static LegoRegisterRequirement gpRegister() {
		return new LegoRegisterRequirement(X86Register.all(), Set.of(), Set.of());
	}

	public static LegoRegisterRequirement singleRegister(X86Register register) {
		return new LegoRegisterRequirement(Set.of(register), Set.of(), Set.of());
	}

	public static LegoRegisterRequirement none() {
		return new LegoRegisterRequirement(Set.of(), Set.of(), Set.of());
	}
}
