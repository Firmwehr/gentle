package com.github.firmwehr.gentle.firm;

import firm.Firm;

public class FirmFun {

	public static void main(String[] args) {
		foo();
	}

	public static void foo() {
		Firm.init();


		Firm.finish();
	}
}
