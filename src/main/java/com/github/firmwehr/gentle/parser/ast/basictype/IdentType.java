package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record IdentType(Ident name) implements BasicType {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(name);
	}

	@Override
	public SourceSpan sourceSpan() {
		return name.sourceSpan();
	}
}
