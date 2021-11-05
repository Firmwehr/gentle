package com.github.firmwehr.gentle.util.string;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SimpleStringTable implements StringTable {
	private final Map<String, String> table = new HashMap<>();

	@Override
	public String deduplicate(String str) {
		return table.computeIfAbsent(str, Function.identity());
	}
}
