package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record IdentExpression(Ident name) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(name);
	}

	@Override
	public SourceSpan sourceSpan() {
		return name.sourceSpan();
	}
}
