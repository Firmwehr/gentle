package com.github.firmwehr.gentle.backend.ir.register;

import java.util.Set;

public record IkeaRegisterRequirement(
	Set<X86Register> limitedTo,
	Set<Integer> shouldBeSame,
	Set<Integer> mustBeDifferent
) {

	public boolean limited() {
		return limitedTo.size() < X86Register.values().length;
	}

	public static IkeaRegisterRequirement gpRegister() {
		return new IkeaRegisterRequirement(X86Register.all(), Set.of(), Set.of());
	}

	public static IkeaRegisterRequirement singleRegister(X86Register register) {
		return new IkeaRegisterRequirement(Set.of(register), Set.of(), Set.of());
	}
}
