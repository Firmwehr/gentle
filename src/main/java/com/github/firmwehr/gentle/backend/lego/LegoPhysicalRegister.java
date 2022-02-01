package com.github.firmwehr.gentle.backend.lego;

public record LegoPhysicalRegister(
	String assemblyName,
	LegoRegisterSize size
) implements LegoBøx {
}
