package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record MainMethod(
	Ident name,
	Parameter parameter,
	Block body
) implements PrettyPrint {
	public static MainMethod dummy(String name, Type paramType, String paramName) {
		return new MainMethod(Ident.dummy(name), new Parameter(paramType, Ident.dummy(paramName)),
			Statement.newBlock());
	}

	public MainMethod withBody(Block body) {
		return new MainMethod(name, parameter, body);
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add("public static void ").add(name).add("(").add(parameter).add(") ").add(body);
	}
}
