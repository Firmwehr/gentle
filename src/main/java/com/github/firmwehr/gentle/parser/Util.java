package com.github.firmwehr.gentle.parser;

import java.util.List;
import java.util.stream.Stream;

public final class Util {
	private Util() {
	}

	public static <T> List<T> copyAndAppend(List<T> ts, T t) {
		return Stream.concat(ts.stream(), Stream.of(t)).toList();
	}
}
