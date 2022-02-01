package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.BITS_32;
import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.BITS_64;
import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.BITS_8;

public enum X86Register {
	RAX(Map.of(BITS_64, "rax", BITS_32, "eax", BITS_8, "AL")),
	RBX(Map.of(BITS_64, "rbx", BITS_32, "ebx", BITS_8, "BL")),
	RCX(Map.of(BITS_64, "rcx", BITS_32, "ecx", BITS_8, "CL")),
	RDX(Map.of(BITS_64, "rdx", BITS_32, "edx", BITS_8, "DL")),
	RSI(Map.of(BITS_64, "rsi", BITS_32, "esi", BITS_8, "SIL")),
	RDI(Map.of(BITS_64, "rdi", BITS_32, "edi", BITS_8, "DIL")),
	RSP(Map.of(BITS_64, "rsp", BITS_32, "rsp", BITS_8, "SPL")),
	RBP(Map.of(BITS_64, "rbp", BITS_32, "rbp", BITS_8, "BPL")),
	R8(Map.of(BITS_64, "r8", BITS_32, "r8d", BITS_8, "r8b")),
	R9(Map.of(BITS_64, "r9", BITS_32, "r9d", BITS_8, "r9b")),
	R10(Map.of(BITS_64, "r10", BITS_32, "r10d", BITS_8, "r10b")),
	R11(Map.of(BITS_64, "r11", BITS_32, "r11d", BITS_8, "r11b")),
	R12(Map.of(BITS_64, "r12", BITS_32, "r12d", BITS_8, "r12b")),
	R13(Map.of(BITS_64, "r13", BITS_32, "r13d", BITS_8, "r13b")),
	R14(Map.of(BITS_64, "r14", BITS_32, "r14d", BITS_8, "r14b")),
	R15(Map.of(BITS_64, "r15", BITS_32, "r15d", BITS_8, "r15b"));

	private Map<LegoBøx.LegoRegisterSize, String> names;

	X86Register(Map<LegoBøx.LegoRegisterSize, String> names) {
		this.names = names;
	}

	public String nameForSize(LegoNode node) {
		return nameForSize(node.size());
	}

	public String nameForSize(LegoBøx.LegoRegisterSize size) {
		String s = names.get(size);
		if (s == null) {
			throw new InternalCompilerException("tried to retrieve register for non-register");
		}
		return "%" + s;
	}

	public static Set<X86Register> all() {
		return EnumSet.allOf(X86Register.class);
	}

	public static int registerCount() {
		return values().length;
	}

	public static void main(String[] args) {
		for (X86Register value : values()) {
			for (LegoBøx.LegoRegisterSize size : LegoBøx.LegoRegisterSize.values()) {
				System.out.println("%" + value.nameForSize(size));
			}
		}
	}
}
