package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record BooleanType(SourceSpan sourceSpan) implements BasicType {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add("boolean");
	}
}
