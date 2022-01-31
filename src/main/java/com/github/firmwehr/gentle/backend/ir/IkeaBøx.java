package com.github.firmwehr.gentle.backend.ir;

import com.github.firmwehr.gentle.InternalCompilerException;
import firm.Mode;
import firm.nodes.Node;

public sealed interface IkeaBøx permits IkeaVirtualRegister, IkeaPhysicalRegister, IkeaUnassignedBøx, IkeaImmediate {

	String assemblyName();

	IkeaRegisterSize size();


	enum IkeaRegisterSize {
		BITS_64("", "q", 64),
		BITS_32("d", "l", 32),
		BITS_8("b", "b", 8),
		ILLEGAL("ILLEGAL", "ILLEGAL", -1);

		private final String newRegisterSuffix;
		private final String oldRegisterSuffix;

		private final int bits;

		IkeaRegisterSize(String newRegisterSuffix, String oldRegisterSuffix, int bits) {
			this.newRegisterSuffix = newRegisterSuffix;
			this.oldRegisterSuffix = oldRegisterSuffix;
			this.bits = bits;
		}

		public static IkeaRegisterSize forMode(Node node) {
			return forMode(node.getMode());
		}

		public static IkeaRegisterSize forMode(Mode mode) {
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
