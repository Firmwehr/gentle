package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.parser.ast.Program;

// This wrapper allows us to give the parameterized test cases nice labels
public record ParserTestCase(
	String label,
	String source,
	Program expectedProgram
) {
	@Override
	public String toString() {
		return label();
	}
}