package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record ArrayAccessOp(Expression index) implements PostfixOp {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("[").add(index).add("]");
	}
}
