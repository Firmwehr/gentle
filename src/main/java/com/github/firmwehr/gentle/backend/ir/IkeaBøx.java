package com.github.firmwehr.gentle.backend.ir;

public sealed interface IkeaBøx permits IkeaVirtualRegister, IkeaPhysicalRegister, IkeaUnassignedBøx, IkeaImmediate {

	String assemblyName();

}
