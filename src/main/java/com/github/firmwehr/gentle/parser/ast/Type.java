package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.basictype.BasicType;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record Type(
	BasicType basicType,
	int arrayLevel
) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(basicType);
		for (int i = 0; i < arrayLevel; i++) {
			p.add("[]");
		}
	}
}
