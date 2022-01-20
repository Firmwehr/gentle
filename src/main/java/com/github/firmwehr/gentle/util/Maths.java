package com.github.firmwehr.gentle.util;

import javax.annotation.Nonnegative;

public final class Maths {

	private Maths() {
		throw new AssertionError();
	}

	public static int floorLog2(@Nonnegative int n) {
		return 31 - Integer.numberOfLeadingZeros(n);
	}
}
