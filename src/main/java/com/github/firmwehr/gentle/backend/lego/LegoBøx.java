package com.github.firmwehr.gentle.backend.lego;

import com.github.firmwehr.gentle.InternalCompilerException;
import firm.Mode;
import firm.nodes.Node;

public sealed interface LegoBøx permits LegoVirtualRegister, LegoPhysicalRegister, LegoUnassignedBøx {

	String assemblyName();

	LegoRegisterSize size();


	enum LegoRegisterSize {
		BITS_64("", "q", 64),
		BITS_32("d", "l", 32),
		BITS_8("b", "b", 8),
		ILLEGAL("ILLEGAL", "ILLEGAL", -1);

		private final String newRegisterSuffix;
		private final String oldRegisterSuffix;

		private final int bits;

		LegoRegisterSize(String newRegisterSuffix, String oldRegisterSuffix, int bits) {
			this.newRegisterSuffix = newRegisterSuffix;
			this.oldRegisterSuffix = oldRegisterSuffix;
			this.bits = bits;
		}

		public static LegoRegisterSize forMode(Node node) {
			return forMode(node.getMode());
		}

		public static LegoRegisterSize forMode(Mode mode) {
			if (mode.equals(Mode.getP())) {
				return BITS_64;
			}
			return switch (mode.getSizeBits()) {
				case 64 -> BITS_64;
				case 32 -> BITS_32;
				case 8 -> BITS_8;
				default -> throw new InternalCompilerException("Unsupported register size " + mode);
			};
		}

		public String getNewRegisterSuffix() {
			return newRegisterSuffix;
		}

		public String getOldRegisterSuffix() {
			return oldRegisterSuffix;
		}
	}
}
