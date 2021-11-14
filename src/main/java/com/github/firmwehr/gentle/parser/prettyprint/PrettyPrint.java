package com.github.firmwehr.gentle.parser.prettyprint;

public interface PrettyPrint {
	void prettyPrint(PrettyPrinter p, Parentheses parens);

	enum Parentheses {
		OMIT,
		INCLUDE
	}
}
