package com.github.firmwehr.gentle.backend.ir;

import java.util.Map;

public record IkeaParentBløck(
	IkeaBløck parent,
	Map<IkeaBøx, IkeaBøx> renames
) {
}
