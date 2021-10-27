package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record FieldAccessOp(Ident name) implements PostfixOp {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(".").add(name);
	}
}
